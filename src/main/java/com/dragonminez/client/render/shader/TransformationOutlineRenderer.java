package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.*;

/** Captures the actual skinned silhouette and runs the upstream 2.2.1 outline passes after the world. */
public final class TransformationOutlineRenderer {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<RenderType, RenderPipeline> MASK_PIPELINES = new IdentityHashMap<>();
    private static final Map<String, RenderTarget> TARGETS = new HashMap<>();
    private static final RuntimePostPass UNPACK = new RuntimePostPass("transformation_unpack", "DiffuseSampler");
    private static final RuntimePostPass BLUR_H = new RuntimePostPass("transformation_blur_h", "DiffuseSampler");
    private static final RuntimePostPass BLUR_V = new RuntimePostPass("transformation_blur_v", "DiffuseSampler");
    private static final RuntimePostPass WIDE_H = new RuntimePostPass("ki_blur_h", "DiffuseSampler");
    private static final RuntimePostPass WIDE_V = new RuntimePostPass("ki_blur_v", "DiffuseSampler");
    private static final RuntimePostPass BLOOM = new RuntimePostPass("transformation_bloom_source", "DiffuseSampler", "GlowSampler", "OuterSampler");
    private static final RuntimePostPass COMPOSITE = new RuntimePostPass("transformation_composite", "DiffuseSampler", "ColoredMaskSampler", "GlowSampler", "OuterSampler", "BloomSampler");
    private static GpuBuffer colors;
    private static boolean failed;
    private static Player capturePlayer;

    private TransformationOutlineRenderer() {}

    public static void beginFrame() { ENTRIES.clear(); }
    public static Player setCapturePlayer(Player player) { Player previous = capturePlayer; capturePlayer = player; return previous; }
    public static Player capturePlayer() { return capturePlayer; }

    public static FormConfig.FormData.OutlineShaderConfig config(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (failed || player == null || mc.level == null || EntityPreviewRenderContext.isRendering()
                || !Boolean.TRUE.equals(ConfigManager.getUserConfig().getTransformationOutlines())) return null;
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return null;
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return null;
        var form = stats.getCharacter().getActiveFormData();
        var stack = stats.getCharacter().getActiveStackFormData();
        if (stack != null && stack.getOutlineShader().isEnabled()) return stack.getOutlineShader();
        return form != null && form.getOutlineShader().isEnabled() ? form.getOutlineShader() : null;
    }

    public static void capture(Player player, PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer draw) {
        com.dragonminez.client.render.effects.AuraBorderRenderer.capture(player, pose, type, draw);
        var config = config(player);
        if (config != null) ENTRIES.add(new Entry(new EffectGeometry(pose, type, draw), config));
    }

    public static void flush(float partialTick) {
        if (failed || ENTRIES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { ENTRIES.clear(); return; }
        try {
            RenderTarget main = mc.gameRenderer.mainRenderTarget();
            RenderTarget mask = target("mask", main), colored = target("colored", main), outer = target("outer", main);
            RenderTarget horizontal = target("horizontal", main), glow = target("glow", main), outerBlur = target("outer_blur", main);
            RenderTarget bloom = target("bloom", main), swap = target("swap", main);
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mask.getColorTexture(), new Vector4f());
            for (Entry entry : ENTRIES) {
                float[] primary = ColorUtils.hexToRgb(entry.config.getPrimaryColor());
                float[] secondary = ColorUtils.hexToRgb(entry.config.getSecondaryColor());
                var encoder = RenderSystem.getDevice().createCommandEncoder();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var data = Std140Builder.onStack(stack, 16).putVec4(pack(primary[0], secondary[0]),
                            pack(primary[1], secondary[1]), pack(primary[2], secondary[2]), 1).get();
                    if (colors == null) colors = RenderSystem.getDevice().createBuffer(() -> "DMZ outline colors", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
                    else encoder.writeToBuffer(colors.slice(), data);
                }
                entry.geometry.draw(mask, MASK_PIPELINES.computeIfAbsent(entry.geometry.type(), TransformationOutlineRenderer::maskPipeline), colors);
            }
            var config = ENTRIES.getFirst().config;
            float radius = Math.clamp(6 + (float) config.getOutlineThickness() * 3, 3, 28);
            float[] values = {(mc.level.getGameTime() % 72000 + partialTick) / 20, radius, 0.95f, 1.35f,
                    2.5f, radius, config.getFireIntensity(), config.isGradient() ? 1 : 0, config.getBodyGlow(), config.isGradient() ? 1 : 0, 0, 0};
            UNPACK.process(colored, values, input(mask));
            values[9] = 2;
            UNPACK.process(outer, values, input(mask));
            values[9] = 0;
            BLUR_H.process(horizontal, values, input(colored));
            BLUR_V.process(glow, values, input(horizontal));
            values[1] = config.isGradient() ? radius : 0;
            BLUR_H.process(horizontal, values, input(outer));
            BLUR_V.process(outerBlur, values, input(horizontal));
            BLOOM.process(bloom, values, Map.of("DiffuseSampler", colored, "GlowSampler", glow, "OuterSampler", outerBlur));
            values[1] = 34;
            for (int i = 0; i < 2; i++) {
                WIDE_H.process(horizontal, values, input(bloom));
                WIDE_V.process(bloom, values, input(horizontal));
            }
            COMPOSITE.process(swap, values, Map.of("DiffuseSampler", main, "ColoredMaskSampler", colored,
                    "GlowSampler", glow, "OuterSampler", outerBlur, "BloomSampler", bloom));
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(swap.getColorTexture(), main.getColorTexture(),
                    0, 0, 0, 0, 0, main.width, main.height);
        } catch (Exception exception) {
            failed = true;
            LogUtil.warn(Env.CLIENT, "Skipping transformation outline until resource reload: {}", exception.toString());
        } finally { ENTRIES.clear(); }
    }

    private static RenderPipeline maskPipeline(RenderType source) {
        var pipeline = RenderPipeline.builder().withLocation(id("outline_mask_" + MASK_PIPELINES.size()))
                .withVertexShader(id("core/outline_capture")).withFragmentShader(id("core/outline_capture"))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("MaskColor", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(ColorTargetState.DEFAULT).withVertexBinding(0, source.format())
                .withPrimitiveTopology(source.primitiveTopology()).withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).build();
        if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) throw new IllegalStateException("Outline mask compilation failed");
        return pipeline;
    }

    private static RenderTarget target(String name, RenderTarget main) {
        RenderTarget result = TARGETS.computeIfAbsent(name, key -> new TextureTarget("DMZ outline " + key, main.width, main.height, false, GpuFormat.RGBA8_UNORM));
        if (result.width != main.width || result.height != main.height) result.resize(main.width, main.height);
        return result;
    }

    private static float pack(float primary, float secondary) {
        return (Math.round(Math.clamp(primary, 0, 1) * 15) * 16 + Math.round(Math.clamp(secondary, 0, 1) * 15)) / 255.0f;
    }
    private static Map<String, RenderTarget> input(RenderTarget input) { return Map.of("DiffuseSampler", input); }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("dragonminez", path); }

    public static void reset() {
        ENTRIES.clear(); MASK_PIPELINES.clear();
        TARGETS.values().forEach(RenderTarget::destroyBuffers); TARGETS.clear();
        for (var pass : List.of(UNPACK, BLUR_H, BLUR_V, WIDE_H, WIDE_V, BLOOM, COMPOSITE)) pass.close();
        if (colors != null) { colors.close(); colors = null; }
        failed = false;
    }

    private record Entry(EffectGeometry geometry, FormConfig.FormData.OutlineShaderConfig config) {}
}
