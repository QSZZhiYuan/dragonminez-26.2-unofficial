package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
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
 * Renders the expanding ki explosion sphere.
 *
 * <p>See {@link KiBarrierRenderer} for the 26.2 migration notes — per-frame values move into a
 * render state, geometry is replayed through {@code submitCustomGeometry}, and the old shader
 * uniforms become a per-vertex tint.
 */
public class KiExplosionRenderer extends EntityRenderer<KiExplosionEntity, KiExplosionRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");

    public KiExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public boolean firing;
        public float scale;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiExplosionEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.firing = entity.isFiring();
        if (!state.firing) {
            state.scale = entity.getSize();
        } else {
            int activeTicks = entity.tickCount - entity.getFireTick();
            float scale = entity.getSize() + (activeTicks * 0.4f);
            float limit = Math.max(entity.getMaxRadius(), entity.getSize() * 5.0f);
            state.scale = Math.min(scale, limit);
        }

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

        int tint = ARGB.colorFromFloat(0.65f, state.coreR, state.coreG, state.coreB);
        List<KiMeshFactory.Vertex> mesh = KiMeshFactory.sphereMesh();
        RenderType renderType = ModRenderTypes.kiWave();

        poseStack.pushPose();
        poseStack.translate(0.0D, state.boundingBoxHeight / 2.0D, 0.0D);
        poseStack.scale(state.scale, state.scale, state.scale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();
    }
}
