package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Renders ki waves / beams (generic, brightness, double and lightning variants).
 *
 * <h2>26.2 migration</h2>
 * See {@link KiBarrierRenderer} for the general notes. The specifics here:
 * <ul>
 *   <li>every {@code VertexBuffer} bind/draw and {@code ShaderInstance} uniform push is gone; the
 *       three helpers ({@code renderKiSphere}, {@code renderKiCylinder}, {@code renderGalickLightning})
 *       now take a {@link SubmitNodeCollector} and replay pre-baked geometry through
 *       {@code submitCustomGeometry};</li>
 *   <li>per-draw colour and alpha become a per-vertex tint;</li>
 *   <li>the procedural ki shader receives its three colours and unit-local coordinates through
 *       vertex attributes; the shaft is clipped on the CPU where it meets the two end balls;</li>
 *   <li>{@code RenderType#setupRenderState/clearRenderState} are gone (state lives on the pipeline).</li>
 * </ul>
 */
public class KiWaveRenderer extends EntityRenderer<KiWaveEntity, KiWaveRenderer.State> {
    private static final Identifier TEXTURE_NULL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

    public KiWaveRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public boolean firing;
        public int kiRenderType;
        public int entityId;
        public float ageInTicks;
        public float fadeAlpha = 1.0f;
        public float size;
        public float castSize;
        public float castWave;
        public float beamLength;
        public float yaw;
        public float pitch;
        public float ownerWidth = 0.5f;
        public float coreR, coreG, coreB;
        public float borderR, borderG, borderB;
        public float outlineR, outlineG, outlineB;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiWaveEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.firing = entity.isFiring();
        state.kiRenderType = entity.getKiRenderType();
        state.entityId = entity.getId();
        state.ageInTicks = entity.tickCount + partialTick;
        state.size = entity.getSize();
        state.castSize = entity.getCastSize();
        state.castWave = (float) entity.getCastWave();
        state.beamLength = entity.getBeamLength();
        state.yaw = entity.getYRot();
        state.pitch = entity.getXRot();
        state.ownerWidth = entity.getOwner() != null ? entity.getOwner().getBbWidth() : 0.5F;

        int maxLife = entity.getMaxLife();
        int fadeTicks = 20;
        float fade = 1.0F;
        if (entity.tickCount >= maxLife - fadeTicks) {
            fade = Math.max(0.0F, (maxLife - state.ageInTicks) / (float) fadeTicks);
        }
        state.fadeAlpha = fade;

        float[] core = entity.getRgbColorMain();
        float[] border = entity.getRgbColorBorder();
        float[] outline = entity.getRgbColorOutline();
        state.coreR = core[0]; state.coreG = core[1]; state.coreB = core[2];
        state.borderR = border[0]; state.borderG = border[1]; state.borderB = border[2];
        state.outlineR = outline[0]; state.outlineG = outline[1]; state.outlineB = outline[2];
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        switch (state.kiRenderType) {
            case 1, 2 -> renderWaveBrightness(state, poseStack, collector);
            case 3 -> renderWaveDouble(state, poseStack, collector, true);
            case 5 -> renderWaveDouble(state, poseStack, collector, false);
            default -> renderWave(state, poseStack, collector);
        }
    }

    // ------------------------------------------------------------------
    // Variant 1: generic wave
    // ------------------------------------------------------------------

    private void renderWave(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float ageInTicks = state.ageInTicks;
        float currentWidth = computeCurrentWidth(state);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));

        // Charge / core ball.
        poseStack.pushPose();
        float ballScale = currentWidth * 1.5F;
        poseStack.scale(ballScale, ballScale, ballScale);
        renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
        poseStack.popPose();

        if (state.firing) {
            float length = Math.max(state.beamLength, 0.1F);
            float cylinderWidth = currentWidth * 1.2F;

            poseStack.pushPose();
            renderKiCylinder(state, poseStack, collector, cylinderWidth, length,
                    state.fadeAlpha, ballScale, currentWidth * 1.5F);
            poseStack.popPose();

            // Ball at the beam tip.
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, length);
            float endBallScale = currentWidth * 1.5F;
            poseStack.scale(endBallScale, endBallScale, endBallScale);
            renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Variant 2: brightness (pulsing) wave
    // ------------------------------------------------------------------

    private void renderWaveBrightness(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        float ageInTicks = state.ageInTicks;
        float currentWidth = computeCurrentWidth(state);
        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float width = currentWidth * basePulse;

        Vec3 dir = Vec3.directionFromRotation(state.pitch, state.yaw);

        poseStack.pushPose();

        if (!state.firing) {
            poseStack.pushPose();
            float startBallScale = width * 1.5F;
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
            poseStack.popPose();
            poseStack.popPose();
            return;
        }

        float length = Math.max(state.beamLength, 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));

        float tubeLength = Math.max(length, 0.1F);
        renderKiCylinder(state, poseStack, collector, width, tubeLength,
                state.fadeAlpha, width * 1.5F, width * 2.5F);

        if (state.kiRenderType == 2) {
            renderGalickLightning(state, poseStack, collector, ageInTicks, width);
        }
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPosSphere = dir.scale(0.1D);
        poseStack.translate(startPosSphere.x, startPosSphere.y, startPosSphere.z);
        float startBallScaleDisp = width * 1.5F;
        poseStack.scale(startBallScaleDisp, startBallScaleDisp, startBallScaleDisp);
        renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPosSphere = dir.scale(length);
        poseStack.translate(endPosSphere.x, endPosSphere.y, endPosSphere.z);
        float endBallScaleDisp = width * 2.5F;
        poseStack.scale(endBallScaleDisp, endBallScaleDisp, endBallScaleDisp);
        renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Variants 3 / 5: double beam
    // ------------------------------------------------------------------

    private void renderWaveDouble(State state, PoseStack poseStack, SubmitNodeCollector collector, boolean withLightning) {
        float ageInTicks = state.ageInTicks;
        float chargeProgress = (state.castWave > 0.1F) ? Math.min(1.0F, ageInTicks / state.castWave) : 1.0F;
        float currentWidth = (!state.firing) ? (state.castSize * chargeProgress) : state.size;

        float basePulse = 1.0F + (float) Math.sin(ageInTicks * 1.5F) * 0.15F;
        float width = currentWidth * basePulse;

        Vec3 dir = Vec3.directionFromRotation(state.pitch, state.yaw);

        poseStack.pushPose();

        if (!state.firing) {
            float startBallScale = width;
            float initialSpread = 1.8F;
            float hitboxWidth = state.ownerWidth;
            float holdTicks = 23F;
            float joinTicks = 24F;
            float convergeProgress = ageInTicks <= holdTicks ? 0.0F : Math.min(1.0F, (ageInTicks - holdTicks) / (joinTicks - holdTicks));
            float lateralOffset = (hitboxWidth * initialSpread) * (1.0F - convergeProgress);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.translate(lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            poseStack.translate(-lateralOffset, 0.0D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            poseStack.scale(startBallScale, startBallScale, startBallScale);
            renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
            poseStack.popPose();

            poseStack.popPose();
            return;
        }

        float length = Math.max(state.beamLength, 0.1F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        renderKiCylinder(state, poseStack, collector, width, length,
                state.fadeAlpha, width * 1.8F, width * 2.5F);
        if (withLightning) {
            renderGalickLightning(state, poseStack, collector, ageInTicks, width);
        }
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 startPos = dir.scale(0.1D);
        poseStack.translate(startPos.x, startPos.y, startPos.z);
        float startBallScale = width * 1.8F;
        poseStack.scale(startBallScale, startBallScale, startBallScale);
        renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
        poseStack.popPose();

        poseStack.pushPose();
        Vec3 endPos = dir.scale(length);
        poseStack.translate(endPos.x, endPos.y, endPos.z);
        poseStack.scale(width * 2.5F, width * 2.5F, width * 2.5F);
        renderKiSphere(state, poseStack, collector, ageInTicks, state.fadeAlpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Shared geometry helpers
    // ------------------------------------------------------------------

    private static float computeCurrentWidth(State state) {
        if (state.firing) return state.size;
        if (state.castWave > 0.1f && state.ageInTicks <= state.castWave) {
            return state.castSize * (state.ageInTicks / state.castWave);
        }
        return state.castSize;
    }

    /** Draws the wave's core sphere with the member's colours. */
    private void renderKiSphere(State state, PoseStack poseStack, SubmitNodeCollector collector,
                                float ageInTicks, float alphaMultiplier) {
        List<KiMeshFactory.Vertex> mesh = KiMeshFactory.sphereMesh();
        RenderType renderType = ModRenderTypes.kiWave();
        int tint = ARGB.colorFromFloat(alphaMultiplier, state.coreR, state.coreG, state.coreB);
        int border = ARGB.colorFromFloat(1.0f, state.borderR, state.borderG, state.borderB);
        int outline = ARGB.colorFromFloat(1.0f, state.outlineR, state.outlineG, state.outlineB);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, border, outline));
    }

    /**
     * Draws the beam shaft.
     *
     * @param radius       shaft radius
     * @param length       shaft length
     * @param cutRadius    radius of the start ball
     * @param cutRadiusEnd radius of the end ball
     */
    private void renderKiCylinder(State state, PoseStack poseStack, SubmitNodeCollector collector,
                                  float radius, float length, float alphaMultiplier,
                                  float cutRadius, float cutRadiusEnd) {
        RenderType renderType = ModRenderTypes.kiWave();
        int tint = ARGB.colorFromFloat(alphaMultiplier, state.coreR, state.coreG, state.coreB);
        int border = ARGB.colorFromFloat(1.0f, state.borderR, state.borderG, state.borderB);
        int outline = ARGB.colorFromFloat(1.0f, state.outlineR, state.outlineG, state.outlineB);
        float near = Math.min(KiMeshFactory.beamEndInset(radius, cutRadius) / length, 0.49F);
        float far = Math.max(1.0F - KiMeshFactory.beamEndInset(radius, cutRadiusEnd) / length, 0.51F);

        poseStack.pushPose();
        poseStack.scale(radius, radius, Math.max(length, 0.001f));
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWaveCylinder(pose, vc, tint, border, outline, near, far));
        poseStack.popPose();
    }

    /** Galick-gun style lightning tendrils wrapped around the beam. */
    private void renderGalickLightning(State state, PoseStack poseStack, SubmitNodeCollector collector,
                                       float ageInTicks, float dynamicWidth) {
        List<KiMeshFactory.Vertex> mesh = AuraRenderer.getLightningMesh();
        if (mesh == null) return;

        RenderType lightningType = ModRenderTypes.getCustomLightning(TEXTURE_NULL);
        int tint = ARGB.colorFromFloat(state.fadeAlpha, 1.0f, 1.0f, 1.0f);

        Random seededRand = new Random((long) (state.entityId + ((int) ageInTicks * 5)));
        float baseScale = dynamicWidth * (state.firing ? 2.2f : 1.8f);

        for (int i = 0; i < (state.firing ? 6 : 4); i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, state.firing ? -0.2D : -0.3D, 0.0D);

            poseStack.mulPose(Axis.XP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float individualScale = baseScale * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(individualScale, individualScale, individualScale);

            collector.submitCustomGeometry(poseStack, lightningType,
                    (pose, vc) -> KiMeshFactory.emit(mesh, pose, vc, tint));
            poseStack.popPose();
        }
    }
}
