package com.dragonminez.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;

public final class EntityPreviewRenderer extends PictureInPictureRenderer<EntityPreviewRenderState> {
    @Override
    public Class<EntityPreviewRenderState> getRenderStateClass() {
        return EntityPreviewRenderState.class;
    }

    @Override
    public void prepare(EntityPreviewRenderState state, GuiRenderState guiState,
            FeatureRenderDispatcher features, int guiScale) {
        EntityPreviewRenderContext.withAppearance(state, () -> super.prepare(state, guiState, features, guiScale));
    }

    @Override
    protected void renderToTexture(EntityPreviewRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        pose.translate(state.translation().x(), state.translation().y(), state.translation().z());
        pose.mulPose(state.rotation());
        if (state.hudPortrait()) pose.translate(0, -28.5f / 16f, 0);
        CameraRenderState camera = new CameraRenderState();
        if (state.cameraOrientation() != null) {
            camera.orientation = state.cameraOrientation().conjugate(new Quaternionf()).rotateY((float) Math.PI);
        }
        minecraft.getEntityRenderDispatcher().submit(state.renderState(), camera, 0.0, 0.0, 0.0, pose, collector);
    }

    @Override
    protected String getTextureLabel() {
        return "dmz character preview";
    }
}
