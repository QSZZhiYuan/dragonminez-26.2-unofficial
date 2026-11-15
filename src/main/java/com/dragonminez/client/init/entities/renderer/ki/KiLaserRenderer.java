package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.KiSpiralMesh;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * Renders ki laser beams (plain laser, aro variant and makankosappo).
 *
 * <h2>26.2 migration</h2>
 * See {@link KiBarrierRenderer} for the general notes. The specifics here:
 * <ul>
 *   <li>the 1.21 code deferred everything into {@code PlayerEffectQueue.addKiAttack} because it
 *       needed a {@code VertexBuffer} bound for the whole batch. Geometry now goes straight into the
 *       frame's {@link SubmitNodeCollector}, so the deferral is dropped and each beam segment is its
 *       own {@code submitCustomGeometry} call;</li>
 *   <li>{@code KiSpiralMesh} returns a vertex list instead of a GPU handle;</li>
 *   <li>the beam alpha (the fades near end of life) becomes part of the per-vertex tint;</li>
 *   <li>{@code localPosMode} has no direct equivalent — the spiral mesh already encodes local
 *       position in its vertex colours, which is what the shader consumed.</li>
 * </ul>
 */
public class KiLaserRenderer extends EntityRenderer<KiLaserEntity, KiLaserRenderer.State> {
    private static final Identifier TEXTURE_KI = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");

    public KiLaserRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public int kiRenderType;
        public float alphaMultiplier = 1.0f;
        public float size;
        public boolean firing;
        public float beamLength;
        public float ageInTicks;
        public float fixedYaw;
        public float fixedPitch;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiLaserEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.kiRenderType = entity.getKiRenderType();
        state.size = entity.getSize();
        state.firing = entity.isFiring();
        state.beamLength = entity.getBeamLength();
        state.ageInTicks = entity.tickCount + partialTick;
        state.fixedYaw = entity.getFixedYaw();
        state.fixedPitch = entity.getFixedPitch();

        int maxLife = entity.getMaxLife();
        int fadeTicks = 10;
        float alpha = 1.0F;
        if (entity.tickCount >= maxLife - fadeTicks) {
            alpha = Math.max(0.0F, (maxLife - state.ageInTicks) / (float) fadeTicks);
        }
        state.alphaMultiplier = alpha;

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

        switch (state.kiRenderType) {
            case 1, 2 -> renderKiMakkankosanpo(state, collector, poseStack);
            default -> renderKiLaser(state, collector, poseStack);
        }
    }

    private void renderKiLaser(State state, SubmitNodeCollector collector, PoseStack poseStack) {
        float radius = state.size * 0.08f;
        float length = state.firing ? state.beamLength : radius;
        int tint = tint(state);

        RenderType renderType = ModRenderTypes.kiWave();
        List<KiMeshFactory.Vertex> mesh = state.firing ? KiMeshFactory.cylinderMesh() : KiMeshFactory.sphereMesh();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.fixedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.fixedPitch));
        poseStack.pushPose();
        if (state.firing) {
            poseStack.scale(radius, radius, length);
        } else {
            float chargeScale = state.size * 0.16f;
            poseStack.scale(chargeScale, chargeScale, chargeScale);
        }
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();
        poseStack.popPose();
    }

    private void renderKiMakkankosanpo(State state, SubmitNodeCollector collector, PoseStack poseStack) {
        boolean isAro = state.kiRenderType == 2;
        float width = state.size * (isAro ? 0.2f : 0.15f);
        float chargeRadius = state.size * 0.08f;
        float length = state.firing ? state.beamLength : chargeRadius;
        int tint = tint(state);
        RenderType renderType = ModRenderTypes.kiWave();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.fixedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.fixedPitch));

        if (!state.firing) {
            List<KiMeshFactory.Vertex> sphere = KiMeshFactory.sphereMesh();
            poseStack.pushPose();
            float chargeScale = state.size * 0.16f;
            poseStack.scale(chargeScale, chargeScale, chargeScale);
            collector.submitCustomGeometry(poseStack, renderType,
                    (pose, vc) -> KiMeshFactory.emitKiWave(sphere, pose, vc, tint, state.borderColor, state.outlineColor));
            poseStack.popPose();
            poseStack.popPose();
            return;
        }

        // Beam shaft.
        List<KiMeshFactory.Vertex> cylinder = KiMeshFactory.cylinderMesh();
        poseStack.pushPose();
        poseStack.scale(width, width, length);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(cylinder, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();

        // Helix around the shaft. The old code applied a Z twist to the model-view matrix; the same
        // rotation is expressed on the pose stack here. Axis#rotation takes radians.
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation((float) Math.toRadians(-(state.ageInTicks * 30.0F))));
        List<KiMeshFactory.Vertex> spiral = isAro
                ? KiSpiralMesh.get(1, state.size, length, width * 2.0f,
                        width * 0.5f * 1.3f, width * 0.5f, 0.9f, true)
                : KiSpiralMesh.get(2, state.size, length, width * 1.5f,
                        width * 0.4f, 0.0f, 0.6f, false);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiSpiral(spiral, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();

        // Ball at the beam tip.
        List<KiMeshFactory.Vertex> tipSphere = KiMeshFactory.sphereMesh();
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, length);
        float endBallScale = width * (isAro ? 2.5f : 1.5f);
        poseStack.scale(endBallScale, endBallScale, endBallScale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(tipSphere, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();

        poseStack.popPose();
    }

    private static int tint(State state) {
        return ARGB.colorFromFloat(state.alphaMultiplier, state.coreR, state.coreG, state.coreB);
    }
}
