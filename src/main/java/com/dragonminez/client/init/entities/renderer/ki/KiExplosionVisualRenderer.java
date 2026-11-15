package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * Renders the fading ki explosion flash.
 *
 * <p>See {@link KiBarrierRenderer} for the 26.2 migration notes. The old code computed the scale and
 * fade alpha inside the deferred draw lambda; both are now derived at submit time from the render
 * state, and the alpha becomes part of the per-vertex tint.
 */
public class KiExplosionVisualRenderer extends EntityRenderer<KiExplosionVisualEntity, KiExplosionVisualRenderer.State> {

    private static final Identifier TEXTURE_EXPLOSION = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    private static final float LIFE_TIME = 25.0F;
    private static final float FADE_START = 0.55F;
    private static final float MAX_ALPHA = 0.45F;

    public KiExplosionVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public float maxSize;
        public float ageInTicks;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiExplosionVisualEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.maxSize = entity.getMaxSize();
        state.ageInTicks = entity.tickCount + partialTick;

        float[] core = entity.getRgbColorMain();
        state.coreR = core[0];
        state.coreG = core[1];
        state.coreB = core[2];
        state.borderColor = 0xFF000000 | entity.getColorBorder();
        state.outlineColor = 0xFF000000 | entity.getColorOutline();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        float progress = Math.min(state.ageInTicks / LIFE_TIME, 1.0F);
        float currentScale = state.maxSize * (0.5F + (progress * 0.5F));

        float currentAlpha = MAX_ALPHA;
        if (progress > FADE_START) {
            currentAlpha = MAX_ALPHA * (1.0F - ((progress - FADE_START) / (1.0F - FADE_START)));
        }

        int tint = ARGB.colorFromFloat(currentAlpha, state.coreR, state.coreG, state.coreB);
        List<KiMeshFactory.Vertex> mesh = KiMeshFactory.sphereMesh();
        RenderType renderType = ModRenderTypes.kiWave();

        poseStack.pushPose();
        poseStack.scale(currentScale, currentScale, currentScale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();
    }
}
