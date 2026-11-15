package com.dragonminez.client.render.effects;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.util.AuraNoiseTexture;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;

/** Upstream shell meshes and shaders rendered after the world, including shader packs. */
public final class Aura3DRenderer {
    private static final List<Draw> QUEUE = new ArrayList<>();
    private static final Map<String, Shell> SHELLS = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private static GpuBuffer parameters;

    private Aura3DRenderer() {}

    public static boolean isAvailable() { return true; }

    public static void beginFrame() { QUEUE.clear(); }

    public static boolean enqueue(String type, PoseStack pose, AuraStyle style, float alpha, float growth,
                                  float time, float backFace) {
        String resolved = "sparking".equalsIgnoreCase(type) ? "sparking" : "god".equalsIgnoreCase(type) ? "god" : "smooth";
        if (FAILED.contains(resolved)) return false;
        QUEUE.add(new Draw(resolved, new Matrix4f(pose.last().pose()), new AuraStyle().copyFrom(style),
                Math.clamp(alpha, 0, 1), Math.clamp(growth, 0, 1), time, backFace));
        return true;
    }

    public static void flush(Matrix4f view, Matrix4f projection) {
        if (QUEUE.isEmpty()) return;
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        var glow = EffectBloomRenderer.captureTarget();
        try {
            for (Draw draw : QUEUE) {
                if (FAILED.contains(draw.type)) continue;
                try {
                    Shell shell = SHELLS.computeIfAbsent(draw.type, Aura3DRenderer::createShell);
                    draw(shell, draw, view, projection, main, false);
                    if (glow != null && draw.style.bloomIntensity > 0) {
                        draw(shell, draw, view, projection, glow, true);
                        EffectBloomRenderer.markDrawn();
                    }
                } catch (Exception exception) {
                    FAILED.add(draw.type);
                    LogUtil.warn(Env.CLIENT, "Skipping 3D aura {} until resource reload: {}", draw.type, exception.toString());
                }
            }
        } finally {
            QUEUE.clear();
        }
    }

    private static void draw(Shell shell, Draw draw, Matrix4f view, Matrix4f projection, RenderTarget target, boolean bloom) {
        int passes = "god".equals(draw.type) ? 3 : "sparking".equals(draw.type) ? 2 : 1;
        for (int layer = 0; layer < passes; layer++) {
            Matrix4f modelView = new Matrix4f(view).mul(draw.pose);
            Matrix4f normal = new Matrix4f(modelView).invert().transpose();
            AuraStyle s = draw.style;
            float clock = draw.time * ("sparking".equals(draw.type) ? 0.67f : 1);
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Three matrices followed by twelve vec4s; matches aura3d_params.glsl.
                ByteBuffer bytes = Std140Builder.onStack(stack, 384)
                        .putMat4f(modelView).putMat4f(projection).putMat4f(normal)
                        .putVec4(s.sizeX, s.sizeY, s.sizeZ, clock)
                        .putVec4(clock * s.waveSpeed, draw.growth, Math.max(1, Math.round(s.peaks)), s.waveFrequency)
                        .putVec4(s.waveAmplitude, s.noiseDetail, s.upwardBias, s.coreAlpha)
                        .putVec4(s.rimAlpha, s.rimPower, s.rimThreshold, s.noiseFactor)
                        .putVec4(draw.alpha, draw.backFace, s.bloomIntensity, bloom ? 1 : 0)
                        .putVec4(s.spikeDensity, s.spikeBurstRate, s.spikeRarity, s.bandStart)
                        .putVec4(s.waveSpeed, layer == 0 ? 1 : 0, 0, layer == 1 ? 0 : 1)
                        .putVec4(passes == 3 ? (layer == 0 ? 1 : layer == 2 ? 2 : 0) : 0, s.secondBorderFactor, 0, 0)
                        .putVec4(s.coreColor[0], s.coreColor[1], s.coreColor[2], 0)
                        .putVec4(s.rimColor[0], s.rimColor[1], s.rimColor[2], 0)
                        .putVec4(s.noiseColor[0], s.noiseColor[1], s.noiseColor[2], 0)
                        .putVec4(s.secondBorderColor[0], s.secondBorderColor[1], s.secondBorderColor[2], 0).get();
                if (parameters == null) parameters = RenderSystem.getDevice().createBuffer(() -> "DMZ 3D aura parameters",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, bytes);
                else encoder.writeToBuffer(parameters.slice(), bytes);
            }
            var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            try (var pass = encoder.createRenderPass(() -> "DMZ 3D aura " + draw.type, target.getColorTextureView(),
                    Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty())) {
                pass.setPipeline(shell.pipeline);
                pass.setUniform("AuraParams", parameters);
                if (!"sparking".equals(draw.type)) pass.bindTexture("NoiseTex", AuraNoiseTexture.getView(),
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR));
                pass.setVertexBuffer(0, shell.vertices.slice());
                pass.draw(shell.vertexCount, 1, 0, 0);
            }
        }
    }

    private static Shell createShell(String type) {
        String shader = "core/aura_" + type;
        var layout = BindGroupLayout.builder().withUniform("AuraParams", UniformType.UNIFORM_BUFFER);
        if (!"sparking".equals(type)) layout.withSampler("NoiseTex");
        var pipeline = RenderPipeline.builder().withLocation(id("aura3d/" + type))
                .withVertexShader(id(shader)).withFragmentShader(id(shader))
                .withBindGroupLayout(layout.build())
                .withVertexBinding(0, "god".equals(type) ? DefaultVertexFormat.POSITION_TEX : DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build();
        if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) throw new IllegalStateException("Shader compilation failed");
        int rings = "sparking".equals(type) ? 72 : 96;
        int segments = "sparking".equals(type) ? 128 : 96;
        double normalization = 1;
        if ("sparking".equals(type)) {
            double peak = 0;
            for (int i = 0; i <= rings; i++) peak = Math.max(peak, sparkingProfile((double) i / rings));
            if (peak > 0) normalization = peak;
        }
        int count = rings * segments * 6;
        int stride = "god".equals(type) ? 20 : 12;
        ByteBuffer bytes = MemoryUtil.memAlloc(count * stride);
        try {
            for (int i = 0; i < rings; i++) for (int j = 0; j < segments; j++) {
                float t0 = (float) i / rings, t1 = (float) (i + 1) / rings;
                float u0 = (float) j / segments, u1 = (float) (j + 1) / segments;
                vertex(bytes, type, t0, u0, normalization); vertex(bytes, type, t0, u1, normalization); vertex(bytes, type, t1, u1, normalization);
                vertex(bytes, type, t0, u0, normalization); vertex(bytes, type, t1, u1, normalization); vertex(bytes, type, t1, u0, normalization);
            }
            bytes.flip();
            return new Shell(pipeline, RenderSystem.getDevice().createBuffer(() -> "DMZ " + type + " shell", GpuBuffer.USAGE_VERTEX, bytes), count);
        } finally { MemoryUtil.memFree(bytes); }
    }

    private static void vertex(ByteBuffer bytes, String type, float t, float u, double normalization) {
        double y = t * 2 - 1, radius;
        if ("god".equals(type)) radius = Math.pow(Math.sqrt(Math.max(0, 1 - y * y)), 0.9) * (0.86 + 0.30 * Math.pow(1 - t, 1.3));
        else if ("sparking".equals(type)) radius = sparkingProfile(t) / normalization;
        else radius = (0.8 + 0.5 * Math.pow(1 - t, 1.5)) * Math.sqrt(Math.max(0, 1 - y * y));
        bytes.putFloat((float) (Math.cos(u * Math.PI * 2) * radius));
        bytes.putFloat("god".equals(type) ? t : (float) y);
        bytes.putFloat((float) (Math.sin(u * Math.PI * 2) * radius));
        if ("god".equals(type)) bytes.putFloat(u).putFloat(t);
    }

    private static double sparkingProfile(double t) {
        double y = 2 * t - 1;
        return (0.84 + 0.42 * Math.pow(1 - t, 1.25)) * Math.sqrt(Math.max(0, 1 - y * y));
    }

    public static void reset() {
        QUEUE.clear();
        SHELLS.values().forEach(shell -> shell.vertices.close());
        SHELLS.clear(); FAILED.clear();
        if (parameters != null) { parameters.close(); parameters = null; }
        AuraNoiseTexture.reset();
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("dragonminez", path); }
    private record Shell(RenderPipeline pipeline, GpuBuffer vertices, int vertexCount) {}
    private record Draw(String type, Matrix4f pose, AuraStyle style, float alpha, float growth, float time, float backFace) {}
}
