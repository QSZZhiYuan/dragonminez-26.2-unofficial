package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Optional;
import java.util.OptionalDouble;

/** An immutable per-frame replay of the geometry already submitted by an effect or player layer. */
public record EffectGeometry(PoseStack.Pose pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
    public EffectGeometry(PoseStack stack, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        this(stack.last().copy(), type, renderer);
    }

    public void draw(RenderTarget target, RenderPipeline override, GpuBuffer maskColor) {
        try (var arena = new ByteBufferBuilder(65536)) {
            var builder = new BufferBuilder(arena, type.primitiveTopology(), type.format());
            renderer.render(pose, builder);
            try (var mesh = builder.build()) {
                if (mesh == null) return;
                try (var vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "DMZ effect geometry", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())) {
                    var prepared = type.prepare();
                    var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                    var indices = RenderSystem.getSequentialBuffer(type.primitiveTopology());
                    var indexBuffer = indices.getBuffer(mesh.drawState().indexCount());
                    try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "DMZ captured effect",
                            target.getColorTextureView(), Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(override != null ? override : prepared.pipeline());
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", prepared.dynamicTransforms());
                        if (maskColor != null) pass.setUniform("MaskColor", maskColor);
                        for (var texture : prepared.textures()) {
                            if (override == null || texture.name().equals("Sampler0")) pass.bindTexture(texture.name(), texture.textureView(), texture.sampler());
                        }
                        pass.setVertexBuffer(0, vertexBuffer.slice());
                        pass.setIndexBuffer(indexBuffer, indices.type());
                        pass.drawIndexed(mesh.drawState().indexCount(), 1, 0, 0, 0);
                    }
                }
            }
        }
    }
}
