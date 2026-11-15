package com.dragonminez.client.render.shader;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Upstream fullscreen effects with live parameters, using 26.2 render passes and uniform buffers. */
public final class FullscreenPostEffect {
    private static final List<FullscreenPostEffect> INSTANCES = new ArrayList<>();
    private final RenderPipeline pipeline;
    private final String label;
    private RenderTarget swap;
    private GpuBuffer parameters;
    private boolean ready;
    private boolean loadFailed;

    public FullscreenPostEffect(String fragmentShader) {
        label = "DMZ " + fragmentShader;
        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "post/" + fragmentShader))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(Reference.MOD_ID, fragmentShader))
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withSampler("DiffuseSampler")
                        .withUniform("DmzPostParams", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
        INSTANCES.add(this);
    }

    /** Writes one vec4 per draw; the source depth texture is never modified. */
    public void process(float x, float y, float z, float w) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || loadFailed) return;
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (main.width <= 0 || main.height <= 0 || main.getColorTextureView() == null) return;
        try {
            if (!ready) {
                if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) {
                    throw new IllegalStateException("Shader compilation failed");
                }
                ready = true;
            }
            if (swap == null) {
                swap = new TextureTarget(label, main.width, main.height, false, GpuFormat.RGBA8_UNORM);
            } else if (swap.width != main.width || swap.height != main.height) {
                swap.resize(main.width, main.height);
            }
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var data = Std140Builder.onStack(stack, 16).putVec4(x, y, z, w).get();
                if (parameters == null) {
                    parameters = RenderSystem.getDevice().createBuffer(() -> label + " parameters",
                            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data);
                } else {
                    encoder.writeToBuffer(parameters.slice(), data);
                }
            }
            try (RenderPass pass = encoder.createRenderPass(() -> label, swap.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(pipeline);
                pass.setUniform("DmzPostParams", parameters);
                pass.bindTexture("DiffuseSampler", main.getColorTextureView(),
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                pass.draw(3, 1, 0, 0);
            }
            encoder.copyTextureToTexture(swap.getColorTexture(), main.getColorTexture(),
                    0, 0, 0, 0, 0, main.width, main.height);
        } catch (Exception exception) {
            loadFailed = true;
            reset();
            LogUtil.warn(Env.CLIENT, "Skipping optional fullscreen effect {}: {}", label, exception.getMessage());
        }
    }

    public void reset() {
        if (swap != null) {
            swap.destroyBuffers();
            swap = null;
        }
        if (parameters != null) {
            parameters.close();
            parameters = null;
        }
        ready = false;
    }

    public static void onResourceReload() {
        for (FullscreenPostEffect effect : INSTANCES) {
            effect.reset();
            effect.loadFailed = false;
        }
    }
}
