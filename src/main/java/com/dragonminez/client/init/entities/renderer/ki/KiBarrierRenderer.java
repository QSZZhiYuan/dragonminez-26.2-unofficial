package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Renders the ki barrier sphere.
 *
 * <h2>26.2 migration</h2>
 * 1.21 drew this with {@code VertexBuffer#bind()/drawWithShader()} plus loose uniforms poked through
 * {@code ShaderInstance#safeGetUniform}. Both of those classes are gone. The port:
 * <ul>
 *   <li>the entity's per-frame values are copied into {@link State} during
 *       {@link #extractRenderState}, because {@code submit(...)} only sees the render state;</li>
 *   <li>geometry is replayed into the frame's {@link SubmitNodeCollector} via
 *       {@code submitCustomGeometry} using the pre-baked {@link KiMeshFactory} sphere;</li>
 *   <li>the old colour uniforms become a per-vertex tint ({@link KiMeshFactory#emit(List, PoseStack.Pose, com.mojang.blaze3d.vertex.VertexConsumer, int)}),
 *       since the submit callback cannot reach a {@code RenderPass}.</li>
 * </ul>
 */
public class KiBarrierRenderer extends EntityRenderer<KiBarrierEntity, KiBarrierRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    private static final float SHIELD_SURROUND_FACTOR = 1.45f;

    public KiBarrierRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public boolean firing;
        public float size;
        public float anchorScale = 1.0f;
        public float age;
        public float alpha = 0.95f;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(KiBarrierEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.firing = entity.isFiring();
        state.size = entity.getCurrentSize();
        state.age = entity.tickCount + partialTick;
        state.anchorScale = resolveAnchorModelScale(entity);

        float[] core = entity.getRgbColorMain();
        state.coreR = core[0];
        state.coreG = core[1];
        state.coreB = core[2];
        state.borderColor = 0xFF000000 | entity.getColorBorder();
        state.outlineColor = 0xFF000000 | entity.getColorOutline();

        state.alpha = 0.95f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null
                && mc.options.getCameraType().isFirstPerson()
                && entity.getShieldHost() == mc.player.getId()
                && entity.getOwner() != mc.player) {
            state.alpha = 0.6f;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (!state.firing) return;

        float scale = state.size * SHIELD_SURROUND_FACTOR * state.anchorScale;
        int tint = ARGB.colorFromFloat(state.alpha, state.coreR, state.coreG, state.coreB);
        List<KiMeshFactory.Vertex> mesh = KiMeshFactory.sphereMesh();
        RenderType renderType = ModRenderTypes.kiWave();

        poseStack.pushPose();
        poseStack.translate(0.0D, state.boundingBoxHeight / 2.0D, 0.0D);
        poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, state.borderColor, state.outlineColor));
        poseStack.popPose();
    }

    private static float resolveAnchorModelScale(KiBarrierEntity entity) {
        LivingEntity anchor = null;
        int hostId = entity.getShieldHost();
        if (hostId >= 0) {
            if (entity.level().getEntity(hostId) instanceof LivingEntity host) anchor = host;
        } else if (entity.getOwner() instanceof LivingEntity owner) {
            anchor = owner;
        }
        if (anchor == null) return 1.0f;

        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, anchor).resolve();
        if (statsOpt.isEmpty()) return 1.0f;

        Float[] resolved = statsOpt.get().getCharacter().getResolvedModelScaling();
        return Math.max(resolved[0], Math.max(resolved[1], resolved[2]));
    }
}
