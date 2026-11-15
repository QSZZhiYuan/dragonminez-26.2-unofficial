package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.SPMajinCandyEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Renders the Majin candy lightning bursts.
 *
 * <h2>26.2 migration</h2>
 * See {@link KiBarrierRenderer} for the general notes. The specifics here:
 * <ul>
 *   <li>{@code ShaderInstance}, {@code VertexBuffer}, {@code RenderType#setupRenderState()},
 *       {@code RenderSystem#getProjectionMatrix()} and the loose
 *       {@code modelMatrix}/{@code normalMatrix} uniforms are all gone. The bolt mesh comes from
 *       {@link AuraRenderer#getLightningMesh()} as vertex data and is replayed into the frame's
 *       {@link SubmitNodeCollector}; the model matrix the shader used to receive implicitly is now
 *       just the pose stack;</li>
 *   <li>the beam's yaw / pitch / distance are computed in {@code extractRenderState}, because
 *       {@code submit} only sees the render state and no longer has the entity or its target.</li>
 * </ul>
 */
public class SPMajinCandyRenderer extends EntityRenderer<SPMajinCandyEntity, SPMajinCandyRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
    private static final float[] LIGHTNING_COLOR = {1.0f, 0.4f, 0.8f};
    private static final float LIGHTNING_ALPHA = 1.0f;

    public SPMajinCandyRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public boolean firing;
        public boolean hasTarget;
        public int entityId;
        public float ageInTicks;
        public float beamYaw;
        public float beamPitch;
        public float beamDistance;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SPMajinCandyEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.firing = entity.isFiring();
        state.entityId = entity.getId();
        state.ageInTicks = entity.tickCount + partialTick;

        LivingEntity target = entity.getTargetEntity();
        state.hasTarget = target != null && target.isAlive();
        if (state.hasTarget) {
            Vec3 startPos = entity.getPosition(partialTick);
            Vec3 targetPos = target.getEyePosition(partialTick);

            double dx = targetPos.x - startPos.x;
            double dy = targetPos.y - startPos.y;
            double dz = targetPos.z - startPos.z;

            float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
            state.beamDistance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            state.beamYaw = (float) (Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F;
            state.beamPitch = (float) -(Mth.atan2(dy, horizontalDistance) * (180.0F / (float) Math.PI));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        List<KiMeshFactory.Vertex> mesh = AuraRenderer.getLightningMesh();
        if (mesh == null) return;

        if (!state.firing) {
            renderCastingLightning(state, poseStack, collector, mesh);
        } else if (state.hasTarget) {
            renderBeamLightning(state, poseStack, collector, mesh);
        }
    }

    private void renderCastingLightning(State state, PoseStack poseStack, SubmitNodeCollector collector,
                                        List<KiMeshFactory.Vertex> mesh) {
        RenderType lightningType = ModRenderTypes.getCustomLightning(TEXTURE);
        int tint = ARGB.colorFromFloat(LIGHTNING_ALPHA, LIGHTNING_COLOR[0], LIGHTNING_COLOR[1], LIGHTNING_COLOR[2]);

        Random seededRand = new Random((long) (state.entityId + ((int) state.ageInTicks * 5)));
        float dynamicWidth = 0.6f;

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, -0.2D, 0.0D);

            poseStack.mulPose(Axis.XP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float individualScale = dynamicWidth * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(individualScale, individualScale, individualScale);

            collector.submitCustomGeometry(poseStack, lightningType,
                    (pose, vc) -> KiMeshFactory.emit(mesh, pose, vc, tint));
            poseStack.popPose();
        }
    }

    private void renderBeamLightning(State state, PoseStack poseStack, SubmitNodeCollector collector,
                                     List<KiMeshFactory.Vertex> mesh) {
        RenderType lightningType = ModRenderTypes.getCustomLightning(TEXTURE);
        int tint = ARGB.colorFromFloat(LIGHTNING_ALPHA, LIGHTNING_COLOR[0], LIGHTNING_COLOR[1], LIGHTNING_COLOR[2]);

        Random seededRand = new Random((long) (state.entityId + ((int) state.ageInTicks * 5)));
        float thickness = 0.8f;

        for (int i = 0; i < 6; i++) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(state.beamYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.beamPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(seededRand.nextFloat() * 360));

            float scaleX = thickness * (0.8f + seededRand.nextFloat() * 0.4f);
            float scaleY = thickness * (0.8f + seededRand.nextFloat() * 0.4f);
            poseStack.scale(scaleX, scaleY, state.beamDistance);

            collector.submitCustomGeometry(poseStack, lightningType,
                    (pose, vc) -> KiMeshFactory.emit(mesh, pose, vc, tint));
            poseStack.popPose();
        }
    }
}
