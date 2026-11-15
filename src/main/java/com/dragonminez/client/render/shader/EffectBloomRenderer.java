package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.Optional;

/** Dedicated emission mask and blur, added without attenuating scene brightness. */
public final class EffectBloomRenderer {
    private static final RenderPipeline BLUR = pipeline("effect_blur", false);
    private static final RenderPipeline COMPOSITE = pipeline("effect_bloom_composite", true);
    private static RenderTarget capture, horizontal, vertical;
    private static GpuBuffer parameters;
    private static boolean drawn, failed, ready;

    private EffectBloomRenderer() {}

    public static void beginFrame() { drawn = false; }

    public static RenderTarget captureTarget() {
        if (failed) return null;
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (main.width <= 0 || main.height <= 0) return null;
        try {
            if (capture == null) {
                capture = new TextureTarget("DMZ effect emission", main.width, main.height, false, GpuFormat.RGBA8_UNORM);
                horizontal = new TextureTarget("DMZ bloom horizontal", Math.max(1, main.width / 2), Math.max(1, main.height / 2), false, GpuFormat.RGBA8_UNORM);
                vertical = new TextureTarget("DMZ bloom vertical", Math.max(1, main.width / 2), Math.max(1, main.height / 2), false, GpuFormat.RGBA8_UNORM);
            } else if (capture.width != main.width || capture.height != main.height) {
                capture.resize(main.width, main.height);
                horizontal.resize(Math.max(1, main.width / 2), Math.max(1, main.height / 2));
                vertical.resize(horizontal.width, horizontal.height);
            }
            if (!drawn) RenderSystem.getDevice().createCommandEncoder().clearColorTexture(capture.getColorTexture(), new Vector4f());
            return capture;
        } catch (Exception exception) { disable(exception); return null; }
    }

    public static void markDrawn() { drawn = true; }

    public static void composite() {
        if (!drawn || failed || capture == null) return;
        try {
            if (!ready) {
                if (!RenderSystem.getDevice().precompilePipeline(BLUR).isValid()
                        || !RenderSystem.getDevice().precompilePipeline(COMPOSITE).isValid()) throw new IllegalStateException("Bloom shader compilation failed");
                ready = true;
            }
            blit(BLUR, capture, horizontal, 1, 0, 3, 1);
            blit(BLUR, horizontal, vertical, 0, 1, 3, 1);
            blit(COMPOSITE, vertical, Minecraft.getInstance().gameRenderer.mainRenderTarget(), 0, 0, 0, 0.65f);
        } catch (Exception exception) { disable(exception); }
        finally { drawn = false; }
    }

    private static void blit(RenderPipeline pipeline, RenderTarget source, RenderTarget target, float x, float y, float radius, float strength) {
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bytes = Std140Builder.onStack(stack, 16).putVec4(x, y, radius, strength).get();
            if (parameters == null) parameters = RenderSystem.getDevice().createBuffer(() -> "DMZ bloom parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, bytes);
            else encoder.writeToBuffer(parameters.slice(), bytes);
        }
        try (var pass = encoder.createRenderPass(() -> "DMZ effect bloom", target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("DmzPostParams", parameters);
            pass.bindTexture("DiffuseSampler", source.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    private static RenderPipeline pipeline(String name, boolean additive) {
        return RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("dragonminez", name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", name))
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("DiffuseSampler").withUniform("DmzPostParams", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(additive ? new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)) : ColorTargetState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build();
    }

    private static void disable(Exception exception) {
        failed = true;
        release();
        LogUtil.warn(Env.CLIENT, "Skipping effect bloom until resource reload: {}", exception.toString());
    }

    private static void release() {
        if (capture != null) { capture.destroyBuffers(); capture = null; }
        if (horizontal != null) { horizontal.destroyBuffers(); horizontal = null; }
        if (vertical != null) { vertical.destroyBuffers(); vertical = null; }
        if (parameters != null) { parameters.close(); parameters = null; }
        ready = false; drawn = false;
    }

    public static void reset() { release(); failed = false; }
}
