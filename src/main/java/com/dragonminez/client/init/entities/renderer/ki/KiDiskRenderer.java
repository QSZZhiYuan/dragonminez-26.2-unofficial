package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiDiskEntity;
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
 * Renders the ki disk (a rotating cylinder).
 *
 * <p>See {@link KiBarrierRenderer} for the 26.2 migration notes — the same three changes apply:
 * per-frame values go into a render state, geometry is replayed through
 * {@code submitCustomGeometry}, and the old shader uniforms become a per-vertex tint.
 */
public class KiDiskRenderer extends EntityRenderer<KiDiskEntity, KiDiskRenderer.State> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kidisc.png");

    public KiDiskRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public boolean firing;
        public float size;
        public float castTime;
        public int ageInTicks;
        public float yRot;
        public float xRot;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiDiskEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.firing = entity.isFiring();
        state.size = entity.getSize();
        state.castTime = (float) entity.getCastTime();
        state.ageInTicks = entity.tickCount;
        state.yRot = entity.getYRot();
        state.xRot = entity.getXRot();

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

        float ageInTicks = state.ageInTicks + state.partialTick;
        float scale = state.size;
        if (!state.firing && state.castTime > 0.1F && ageInTicks <= state.castTime) {
            scale *= (ageInTicks / state.castTime);
        }

        int tint = ARGB.colorFromFloat(1.0f, state.coreR, state.coreG, state.coreB);
        List<KiMeshFactory.Vertex> mesh = KiMeshFactory.cylinderMesh();
        RenderType renderType = ModRenderTypes.kiDisk();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        if (state.firing) {
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot + 90.0F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(ageInTicks * 35.0f));
        poseStack.translate(0, 0, -0.025D);
        poseStack.scale(scale, scale, 0.05f);

        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();
    }
}
