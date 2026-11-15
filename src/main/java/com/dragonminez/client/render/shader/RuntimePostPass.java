package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.util.Map;
import java.util.Optional;

/** A fullscreen pass with live outline parameters and explicitly owned input/output targets. */
public final class RuntimePostPass implements AutoCloseable {
    private final RenderPipeline pipeline;
    private GpuBuffer parameters;
    private boolean ready;

    public RuntimePostPass(String shader, String... samplers) {
        var layout = BindGroupLayout.builder().withUniform("OutlineParams", UniformType.UNIFORM_BUFFER);
        for (String sampler : samplers) layout.withSampler(sampler);
        pipeline = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("dragonminez", "runtime/" + shader))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "runtime/" + shader))
                .withBindGroupLayout(layout.build()).withColorTargetState(ColorTargetState.DEFAULT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build();
    }

    public void process(RenderTarget output, float[] values, Map<String, RenderTarget> inputs) {
        if (!ready) {
            if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) throw new IllegalStateException("Unable to compile " + pipeline.getLocation());
            ready = true;
        }
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(stack, 48);
            for (int i = 0; i < 12; i += 4) builder.putVec4(values[i], values[i + 1], values[i + 2], values[i + 3]);
            var bytes = builder.get();
            if (parameters == null) parameters = RenderSystem.getDevice().createBuffer(() -> "DMZ outline parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, bytes);
            else encoder.writeToBuffer(parameters.slice(), bytes);
        }
        try (var pass = encoder.createRenderPass(() -> "DMZ outline pass", output.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("OutlineParams", parameters);
            for (var input : inputs.entrySet()) pass.bindTexture(input.getKey(), input.getValue().getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    @Override public void close() {
        if (parameters != null) { parameters.close(); parameters = null; }
        ready = false;
    }
}
