package com.dragonminez.client.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** Textured GUI quad with captured pose, tint and pipeline, including the animated aura pass. */
public record DmzGuiTexturedQuad(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0, float y0, float x1, float y1,
        float u0, float v0, float u1, float v1,
        int color,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    /** Convenience factory using the standard GUI textured pipeline. */
    public static DmzGuiTexturedQuad textured(TextureSetup textureSetup, Matrix3x2f pose,
                                              float x0, float y0, float x1, float y1,
                                              float u0, float v0, float u1, float v1,
                                              int color, @Nullable ScreenRectangle scissorArea) {
        return new DmzGuiTexturedQuad(RenderPipelines.GUI_TEXTURED, textureSetup, pose,
                x0, y0, x1, y1, u0, v0, u1, v1, color, scissorArea);
    }

    @Override
    public void buildVertices(VertexConsumer vc) {
        // Two triangles, counter-clockwise. UVs are already normalised by the caller.
        vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(this.u0, this.v0).setColor(this.color);
        vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(this.u0, this.v1).setColor(this.color);
        vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(this.u1, this.v1).setColor(this.color);
        vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(this.u1, this.v0).setColor(this.color);
    }

    @Override
    public ScreenRectangle bounds() {
        int minX = (int) Math.floor(Math.min(x0, x1));
        int minY = (int) Math.floor(Math.min(y0, y1));
        int maxX = (int) Math.ceil(Math.max(x0, x1));
        int maxY = (int) Math.ceil(Math.max(y0, y1));
        ScreenRectangle area = new ScreenRectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
        area = area.transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(area) : area;
    }
}
