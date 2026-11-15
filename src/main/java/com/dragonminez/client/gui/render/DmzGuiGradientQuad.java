package com.dragonminez.client.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** A deferred GUI quad; pose, UVs and all four tints are owned by this draw. */
public record DmzGuiGradientQuad(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
        float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
        int topLeft, int bottomLeft, int bottomRight, int topRight,
        @Nullable ScreenRectangle scissorArea) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertices) {
        vertex(vertices, x0, y0, u0, v0, topLeft);
        vertex(vertices, x0, y1, u0, v1, bottomLeft);
        vertex(vertices, x1, y1, u1, v1, bottomRight);
        vertex(vertices, x1, y0, u1, v0, topRight);
    }

    private void vertex(VertexConsumer vertices, float x, float y, float u, float v, int tint) {
        vertices.addVertexWith2DPose(pose, x, y).setColor(tint);
        if (textureSetup.texure0() != null) vertices.setUv(u, v);
    }

    @Override
    public ScreenRectangle bounds() {
        int left = (int) Math.floor(Math.min(x0, x1)), top = (int) Math.floor(Math.min(y0, y1));
        var bounds = new ScreenRectangle(left, top,
                Math.max(1, (int) Math.ceil(Math.max(x0, x1)) - left),
                Math.max(1, (int) Math.ceil(Math.max(y0, y1)) - top)).transformMaxBounds(pose);
        return scissorArea == null ? bounds : scissorArea.intersection(bounds);
    }
}
