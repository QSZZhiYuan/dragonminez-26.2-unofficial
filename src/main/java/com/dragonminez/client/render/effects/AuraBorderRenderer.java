package com.dragonminez.client.render.effects;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.shader.EffectGeometry;
import com.dragonminez.client.render.util.AuraNoiseTexture;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.*;

/** The upstream surge flame follows the actual animated player silhouette, independently of form outlines. */
public final class AuraBorderRenderer {
    private static final int MAX_BORDERS = 254;
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static final Map<Integer, Request> REQUESTS = new LinkedHashMap<>();
    private static final Set<Integer> EVALUATED = new HashSet<>();
    private static final Map<RenderType, RenderPipeline> MASK_PIPELINES = new IdentityHashMap<>();
    private static final RenderPipeline FIELD = postPipeline("aura_border_field", false, "Mask");
    private static final RenderPipeline BLUR = postPipeline("aura_border_blur", false, "Source");
    private static final RenderPipeline FLAME = postPipeline("aura_border", true, "Mask", "Field", "NoiseTex", "SceneDepth");
    private static RenderTarget mask;
    private static final RenderTarget[] fields = new RenderTarget[3], scratch = new RenderTarget[3];
    private static GpuBuffer parameters, maskColor;
    private static boolean failed, ready;

    private AuraBorderRenderer() {}

    public static void beginFrame() { REQUESTS.clear(); EVALUATED.clear(); }

    public static void capture(Player player, PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer draw) {
        Minecraft mc = Minecraft.getInstance();
        if (failed || player == null || mc.level == null || EntityPreviewRenderContext.isRendering()
                || player.isSpectator() || player.isInvisible()
                || (player == mc.gameRenderer.mainCamera().entity() && mc.options.getCameraType().isFirstPerson())) return;
        int id = player.getId();
        if (EVALUATED.add(id)) {
            var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            boolean active = stats != null && stats.getStatus().isSurgeActive();
            State state = STATES.get(id);
            if (state == null && active) { state = new State(); STATES.put(id, state); }
            if (state != null) {
                long now = System.nanoTime();
                float dt = state.lastNanos == 0 ? 1.0f / 60 : Math.min(0.1f, (now - state.lastNanos) / 1.0e9f);
                state.lastNanos = now;
                state.intensity = Mth.clamp(state.intensity + (active ? 4 : -2.5f) * dt, 0, 1);
                if (active) state.color = stats.getCharacter().getActiveRgbAuraColor();
                if (state.intensity > 0.001f && REQUESTS.size() < MAX_BORDERS)
                    REQUESTS.put(id, new Request(player, REQUESTS.size() + 1, state, new ArrayList<>()));
            }
        }
        Request request = REQUESTS.get(id);
        if (request != null) request.geometry.add(new EffectGeometry(pose, type, draw));
    }

    public static void flush(Matrix4f view, Matrix4f projection, float partialTick) {
        if (failed || REQUESTS.isEmpty()) { STATES.clear(); return; }
        var mc = Minecraft.getInstance();
        var main = mc.gameRenderer.mainRenderTarget();
        try {
            ensureTargets(main);
            if (!ready) {
                for (var pipeline : List.of(FIELD, BLUR, FLAME))
                    if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid())
                        throw new IllegalStateException("Surge border shader compilation failed: " + pipeline.getLocation());
                ready = true;
            }
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(), new Vector4f());
            for (Request request : REQUESTS.values()) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var bytes = Std140Builder.onStack(stack, 16).putVec4(request.index / 255.0f, 0, 0, 1).get();
                    if (maskColor == null) maskColor = RenderSystem.getDevice().createBuffer(() -> "DMZ surge mask id",
                            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, bytes);
                    else RenderSystem.getDevice().createCommandEncoder().writeToBuffer(maskColor.slice(), bytes);
                }
                for (EffectGeometry geometry : request.geometry)
                    geometry.draw(mask, MASK_PIPELINES.computeIfAbsent(geometry.type(), AuraBorderRenderer::maskPipeline), maskColor);
            }
            var camera = mc.gameRenderer.mainCamera().position();
            var ordered = new ArrayList<>(REQUESTS.values());
            ordered.sort(Comparator.comparingDouble((Request request) -> request.player.distanceToSqr(camera)).reversed());
            for (Request request : ordered) {
                Placement placement = place(request, view, projection, partialTick, main);
                if (placement != null) draw(request, placement, projection, partialTick, main);
            }
        } catch (Exception exception) {
            failed = true;
            LogUtil.warn(Env.CLIENT, "Skipping surge border until resource reload: {}", exception.toString());
        } finally {
            STATES.keySet().retainAll(REQUESTS.keySet());
            REQUESTS.clear();
        }
    }

    private static Placement place(Request request, Matrix4f view, Matrix4f projection, float partialTick, RenderTarget main) {
        Player player = request.player;
        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        float height = player.getBbHeight(), radius = Math.max(height, player.getBbWidth()) * 0.5f + 0.75f;
        float x = (float) (Mth.lerp(partialTick, player.xo, player.getX()) - camera.x);
        float y = (float) (Mth.lerp(partialTick, player.yo, player.getY()) - camera.y) + height * 0.5f;
        float z = (float) (Mth.lerp(partialTick, player.zo, player.getZ()) - camera.z);
        Vector4f center = new Vector4f(x, y, z, 1).mul(view);
        float depth = -center.z;
        if (depth < -radius) return null;
        float pixelsPerBlock = Math.abs(projection.m11()) * main.height * 0.5f / Math.max(depth, 0.5f);
        float centerX = main.width * 0.5f, centerY = main.height * 0.5f, upX = 0, upY = 1;
        if (depth > 0.1f) {
            Vector4f top = new Vector4f(x, y + 0.5f, z, 1).mul(view).mul(projection);
            Vector4f clip = new Vector4f(center).mul(projection);
            if (clip.w > 1.0e-4f && top.w > 1.0e-4f) {
                centerX = (clip.x / clip.w * 0.5f + 0.5f) * main.width;
                centerY = (clip.y / clip.w * 0.5f + 0.5f) * main.height;
                float dx = (top.x / top.w * 0.5f + 0.5f) * main.width - centerX;
                float dy = (top.y / top.w * 0.5f + 0.5f) * main.height - centerY;
                float length = Mth.sqrt(dx * dx + dy * dy);
                if (length > 1.0e-3f) { upX = dx / length; upY = dy / length; }
            }
        }
        float intensity = request.state.intensity;
        float eased = intensity * intensity * (3 - 2 * intensity);
        float full = Math.min(Mth.clamp(height * 0.11f, 0.12f, 0.60f) * pixelsPerBlock, 96);
        if (full < 1.5f) return null;
        float alpha = eased * Mth.clamp((full - 1.5f) / 2.5f, 0, 1);
        float thickness = Math.max(1.5f, full * (0.35f + 0.65f * eased));
        int level = thickness * 1.25f <= 24 ? 1 : thickness * 1.25f <= 48 ? 2 : 3;
        int[] rect = {0, 0, main.width, main.height};
        if (depth >= radius + 0.5f) {
            float extent = radius * pixelsPerBlock + thickness * 5.6f;
            rect = new int[]{Mth.clamp(Mth.floor(centerX - extent), 0, main.width), Mth.clamp(Mth.floor(centerY - extent), 0, main.height),
                    Mth.clamp(Mth.ceil(centerX + extent), 0, main.width), Mth.clamp(Mth.ceil(centerY + extent), 0, main.height)};
            if (rect[2] <= rect[0] || rect[3] <= rect[1]) return null;
        }
        return new Placement(centerX, centerY, upX, upY, thickness, alpha, level, rect, Math.max(0, depth - radius - 0.6f));
    }

    private static void draw(Request request, Placement placement, Matrix4f projection, float partialTick, RenderTarget main) {
        int shift = placement.level, footprint = 1 << shift;
        RenderTarget field = fields[shift - 1], temp = scratch[shift - 1];
        float radius = Mth.clamp(placement.thickness * 1.25f / footprint, 1, 16);
        float[] rgb = request.state.color;
        boolean depthReadable = main.getDepthTextureView() != null;
        float[] values = {main.width, main.height, placement.centerX, placement.centerY,
                placement.upX, placement.upY, placement.thickness, request.index / 255.0f,
                (request.player.tickCount + partialTick) / 20, (request.player.getId() * 0.6180339f) % 1, placement.alpha, footprint,
                rgb[0], rgb[1], rgb[2], 0, rgb[0], rgb[1], rgb[2], 0,
                projection.m22(), projection.m32(), depthReadable ? placement.occlusionDepth : 0, 0.9f,
                0, 0, radius, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1 : 0};
        int pad = Mth.ceil(radius) + 1;
        post(FIELD, field, values, Map.of("Mask", mask.getColorTextureView()), scissor(placement.rect, shift, pad * 2, field));
        values[24] = 1;
        post(BLUR, temp, values, Map.of("Source", field.getColorTextureView()), scissor(placement.rect, shift, pad, temp));
        values[24] = 0; values[25] = 1;
        post(BLUR, field, values, Map.of("Source", temp.getColorTextureView()), scissor(placement.rect, shift, 0, field));
        var inputs = Map.of("Mask", mask.getColorTextureView(), "Field", field.getColorTextureView(),
                "NoiseTex", AuraNoiseTexture.getView(), "SceneDepth", depthReadable ? main.getDepthTextureView() : mask.getColorTextureView());
        post(FLAME, main, values, inputs, placement.rect);
        RenderTarget emission = EffectBloomRenderer.captureTarget();
        if (emission != null) {
            values[15] = 1;
            post(FLAME, emission, values, inputs, placement.rect);
            EffectBloomRenderer.markDrawn();
        }
    }

    private static void post(RenderPipeline pipeline, RenderTarget output, float[] values, Map<String, GpuTextureView> inputs, int[] rect) {
        if (rect[2] <= rect[0] || rect[3] <= rect[1]) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(stack, 112);
            for (int i = 0; i < values.length; i += 4) builder.putVec4(values[i], values[i + 1], values[i + 2], values[i + 3]);
            var bytes = builder.get();
            if (parameters == null) parameters = RenderSystem.getDevice().createBuffer(() -> "DMZ surge border parameters",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, bytes);
            else encoder.writeToBuffer(parameters.slice(), bytes);
        }
        try (var pass = encoder.createRenderPass(() -> "DMZ surge border", output.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("AuraBorderParams", parameters);
            pass.enableScissor(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]);
            for (var input : inputs.entrySet()) pass.bindTexture(input.getKey(), input.getValue(),
                    input.getKey().equals("NoiseTex") ? RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)
                            : RenderSystem.getSamplerCache().getClampToEdge(input.getKey().equals("Mask") ? FilterMode.NEAREST : FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    private static int[] scissor(int[] rect, int shift, int pad, RenderTarget target) {
        int size = 1 << shift;
        return new int[]{Mth.clamp((rect[0] >> shift) - pad, 0, target.width), Mth.clamp((rect[1] >> shift) - pad, 0, target.height),
                Mth.clamp(((rect[2] + size - 1) >> shift) + pad, 0, target.width), Mth.clamp(((rect[3] + size - 1) >> shift) + pad, 0, target.height)};
    }

    private static RenderPipeline maskPipeline(RenderType source) {
        var pipeline = RenderPipeline.builder().withLocation(id("surge_mask_" + MASK_PIPELINES.size()))
                .withVertexShader(id("core/outline_capture")).withFragmentShader(id("core/outline_capture"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("MaskColor", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(ColorTargetState.DEFAULT).withVertexBinding(0, source.format())
                .withPrimitiveTopology(source.primitiveTopology()).withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).build();
        if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) throw new IllegalStateException("Surge mask compilation failed");
        return pipeline;
    }

    private static RenderPipeline postPipeline(String shader, boolean blended, String... samplers) {
        var layout = BindGroupLayout.builder().withUniform("AuraBorderParams", UniformType.UNIFORM_BUFFER);
        for (String sampler : samplers) layout.withSampler(sampler);
        return RenderPipeline.builder().withLocation(id("runtime/" + shader))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad")).withFragmentShader(id("runtime/" + shader))
                .withBindGroupLayout(layout.build()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
                .withColorTargetState(blended ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT).build();
    }

    private static void ensureTargets(RenderTarget main) {
        if (mask == null) mask = target("mask", main.width, main.height);
        else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height);
        for (int i = 0; i < fields.length; i++) {
            int w = Math.max(1, main.width >> (i + 1)), h = Math.max(1, main.height >> (i + 1));
            if (fields[i] == null) { fields[i] = target("field " + i, w, h); scratch[i] = target("scratch " + i, w, h); }
            else if (fields[i].width != w || fields[i].height != h) { fields[i].resize(w, h); scratch[i].resize(w, h); }
        }
    }

    private static RenderTarget target(String name, int width, int height) {
        return new TextureTarget("DMZ surge " + name, width, height, false, GpuFormat.RGBA8_UNORM);
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("dragonminez", path); }

    public static void reset() {
        REQUESTS.clear(); EVALUATED.clear(); STATES.clear(); MASK_PIPELINES.clear();
        if (mask != null) { mask.destroyBuffers(); mask = null; }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null) fields[i].destroyBuffers();
            if (scratch[i] != null) scratch[i].destroyBuffers();
            fields[i] = null; scratch[i] = null;
        }
        if (parameters != null) { parameters.close(); parameters = null; }
        if (maskColor != null) { maskColor.close(); maskColor = null; }
        ready = false; failed = false;
    }

    private static final class State {
        float intensity;
        long lastNanos;
        float[] color = {1, 1, 1};
    }
    private record Request(Player player, int index, State state, List<EffectGeometry> geometry) {}
    private record Placement(float centerX, float centerY, float upX, float upY, float thickness, float alpha,
                             int level, int[] rect, float occlusionDepth) {}
}
