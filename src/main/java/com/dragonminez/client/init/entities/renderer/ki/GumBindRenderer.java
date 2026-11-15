package com.dragonminez.client.init.entities.renderer.ki;
import com.dragonminez.Reference;
import com.dragonminez.client.render.GeoKiRenderState;
import com.dragonminez.common.init.entities.ki.GumBindEntity;
import com.dragonminez.common.network.C2S.GumStruggleC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.BoneSnapshots;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GumBindRenderer extends GeoEntityRenderer<GumBindEntity, GumBindRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/gum_bind.png");
    private static final float REACH_TICKS = 3;
    private static final double STRAND_HALF_WIDTH = 0.13;
    private static final float STRAND_U0 = 4f / 16, STRAND_U1 = 8f / 16, STRAND_V0 = 0, STRAND_V1 = 4f / 16;

    public static class State extends GeoKiRenderState {
        float scaleY, scaleXZ;
        boolean hideTop;
        Vec3 offset = Vec3.ZERO, strandFrom, strandTo;
        double strandWidth;
        int tint;
    }

    public GumBindRenderer(EntityRendererProvider.Context context) { super(context, new Model()); }
    @Override public State createRenderState(GumBindEntity entity, Void unused) { return new State(); }
    @Override public boolean shouldRender(GumBindEntity gum, Frustum frustum, double x, double y, double z) {
        if (gum.tickCount < GumBindEntity.STRAND_TICKS) return true;
        var victim = gum.victim();
        return frustum.isVisible((victim == null ? gum.getBoundingBox() : victim.getBoundingBox()).inflate(1.5));
    }
    @Override public void addRenderData(GumBindEntity gum, Void unused, State state, float partialTick) {
        super.addRenderData(gum, unused, state, partialTick);
        var victim = gum.victim();
        Vec3 origin = gum.getPosition(partialTick);
        Vec3 anchor = victim == null ? origin : victim.getPosition(partialTick);
        state.offset = anchor.subtract(origin);
        state.scaleY = (victim == null ? 1.8f : victim.getBbHeight()) / 1.8f;
        float width = victim == null ? 0.6f : victim.getBbWidth();
        state.scaleXZ = Mth.clamp(Math.max(state.scaleY, width / 0.6f * 0.85f), state.scaleY, state.scaleY * 1.6f);
        state.lockedYaw = victim == null ? gum.getYRot() : Mth.rotLerp(partialTick, victim.yBodyRotO, victim.yBodyRot);
        var mc = Minecraft.getInstance();
        state.hideTop = mc.player != null && mc.player.getId() == gum.victimId() && mc.options.getCameraType().isFirstPerson();
        state.tint = 0xFF000000 | gum.color();
        state.strandFrom = state.strandTo = null;
        if (victim != null && !gum.isReleasing() && gum.tickCount < GumBindEntity.STRAND_TICKS
                && gum.level().getEntity(gum.casterId()) instanceof LivingEntity caster) {
            float age = gum.tickCount + partialTick;
            float reach = Mth.clamp(age / REACH_TICKS, 0, 1);
            float pull = Mth.clamp((age - REACH_TICKS) / (GumBindEntity.STRAND_TICKS - REACH_TICKS), 0, 1);
            Vec3 hand = hand(caster, partialTick).subtract(origin);
            Vec3 chest = anchor.add(0, victim.getBbHeight() * 0.55, 0).subtract(origin);
            state.strandFrom = hand.lerp(chest, pull);
            state.strandTo = hand.lerp(chest, reach);
            state.strandWidth = STRAND_HALF_WIDTH * (1 - 0.5 * pull);
        }
    }
    @Override public int getRenderColor(GumBindEntity gum, Void unused, float partialTick) { return 0xFF000000 | gum.color(); }
    @Override public RenderType getRenderType(State state, Identifier texture) { return RenderTypes.entityCutout(texture); }
    @Override public void adjustRenderPose(RenderPassInfo<State> info) {
        super.adjustRenderPose(info);
        var state = info.renderState();
        info.poseStack().translate(state.offset.x, state.offset.y, state.offset.z);
        info.poseStack().mulPose(Axis.YP.rotationDegrees(180 - state.lockedYaw));
        info.poseStack().scale(state.scaleXZ, state.scaleY, state.scaleXZ);
    }
    @Override public void adjustModelBonesForRender(RenderPassInfo<State> info, BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        bones.ifPresent("coil_1", snapshot -> snapshot.skipRender(info.renderState().hideTop));
        for (var bone : info.model().boneLookup().get().values()) {
            if (bone.name().endsWith("_gloss")) bones.get(bone).skipRender(true);
        }
    }
    @Override public void preRenderPass(RenderPassInfo<State> info, SubmitNodeCollector collector) {
        super.preRenderPass(info, collector);
        for (var gloss : info.model().boneLookup().get().values()) {
            if (!gloss.name().endsWith("_gloss")) continue;
            info.addPerBoneRender(gloss, (pass, bone, nodes) -> {
                for (var parent = bone.parent(); parent != null; parent = parent.parent()) {
                    if (parent.frameSnapshot != null && (parent.frameSnapshot.isHidden() || parent.frameSnapshot.areChildrenHidden())) return;
                }
                int tint = pass.renderState().tint;
                int r = (tint >> 16) & 255, g = (tint >> 8) & 255, b = tint & 255;
                int color = 0xFF000000 | (r + Math.round((255 - r) * 0.55f)) << 16
                        | (g + Math.round((255 - g) * 0.55f)) << 8 | (b + Math.round((255 - b) * 0.55f));
                nodes.submitCustomGeometry(pass.poseStack(), RenderTypes.entityCutout(TEXTURE), (drawPose, vertices) -> {
                    var pose = pass.poseStack();
                    pose.pushPose();
                    pose.last().set(drawPose);
                    try { pass.renderPosed(() -> bone.render(pass, pose, vertices, pass.packedLight(), pass.packedOverlay(), color)); }
                    finally { pose.popPose(); }
                });
            });
        }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        if (state.strandFrom != null) collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TEXTURE), (drawPose, vertices) -> {
            float r = ((state.tint >> 16) & 255) / 255f, g = ((state.tint >> 8) & 255) / 255f, b = (state.tint & 255) / 255f;
            tube(vertices, drawPose.pose(), drawPose.normal(), state.strandFrom, state.strandTo, state.strandWidth, r, g, b, state.lightCoords);
        });
    }
    public static class Model extends GeoModel<GumBindEntity> {
        @Override public Identifier getModelResource(GeoRenderState state) { return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/gum_bind"); }
        @Override public Identifier getTextureResource(GeoRenderState state) { return TEXTURE; }
        @Override public Identifier getAnimationResource(GumBindEntity gum) { return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/gum_bind"); }
    }
private static Vec3 hand(LivingEntity caster, float partialTick) {
		return GumBindEntity.hand(caster.getPosition(partialTick), Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot),
				caster.getBbHeight() / 1.8F);
	}
private static void tube(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 from, Vec3 to, double half,
							 float r, float g, float b, int light) {
		Vec3 dir = to.subtract(from);
		if (dir.lengthSqr() < 1.0E-6D) return;
		dir = dir.normalize();
		Vec3 up = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 a = dir.cross(up).normalize().scale(half);
		Vec3 c = dir.cross(a).normalize().scale(half);
		Vec3[] corners = {a.add(c), c.subtract(a), a.scale(-1.0D).subtract(c), a.subtract(c)};
		for (int side = 0; side < 4; side++) {
			Vec3 c0 = corners[side];
			Vec3 c1 = corners[(side + 1) % 4];
			Vec3 n = c0.add(c1).normalize();
			float shade = side == 0 ? 1.0F : side == 2 ? 0.7F : 0.85F;
			vertex(consumer, pose, normal, from.add(c0), STRAND_U0, STRAND_V0, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, normal, from.add(c1), STRAND_U1, STRAND_V0, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, normal, to.add(c1), STRAND_U1, STRAND_V1, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, normal, to.add(c0), STRAND_U0, STRAND_V1, r * shade, g * shade, b * shade, light, n);
		}
	}
private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Vec3 pos, float u, float v,
							   float r, float g, float b, int light, Vec3 n) {
        Vector3f transformed = normal.transform(new Vector3f((float) n.x, (float) n.y, (float) n.z));
		consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).setColor(r, g, b, 1.0F).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(transformed.x(), transformed.y(), transformed.z());
	}
    public static class BallRenderer extends EntityRenderer<GumBindEntity.Ball, BallRenderer.State> {
        private static final float RADIUS = 0.22f, CORE = 0.7f, STRETCH = 1.35f;
        private static final float SIDE_U0 = 4f / 16, SIDE_U1 = 8f / 16, SIDE_V0 = 4f / 16, SIDE_V1 = 1;
        private static final float TOP_U0 = 12f / 16, SHADE_U0 = 8f / 16, PATCH_V1 = 4f / 16;
        public static class State extends EntityRenderState {
            int color;
            float age, centreY;
            Vec3 hand, motion;
        }
        public BallRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public State createRenderState() { return new State(); }
        @Override public boolean shouldRender(GumBindEntity.Ball ball, Frustum frustum, double x, double y, double z) { return true; }
        @Override public void extractRenderState(GumBindEntity.Ball ball, State state, float partialTick) {
            super.extractRenderState(ball, state, partialTick);
            state.color = ball.color(); state.age = ball.tickCount + partialTick; state.centreY = ball.getBbHeight() * 0.5f;
            state.motion = ball.getDeltaMovement();
            state.hand = ball.getOwner() instanceof LivingEntity caster ? hand(caster, partialTick).subtract(ball.getPosition(partialTick)) : null;
        }
        @Override public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (drawPose, consumer) -> {
                PoseStack pose = new PoseStack(); pose.last().set(drawPose);
                float r = ((state.color >> 16) & 255) / 255f, g = ((state.color >> 8) & 255) / 255f, b = (state.color & 255) / 255f;
                if (state.hand != null) tube(consumer, drawPose.pose(), drawPose.normal(), state.hand, new Vec3(0, state.centreY, 0), STRAND_HALF_WIDTH * 0.8, r, g, b, state.lightCoords);
                Vec3 motion = state.motion;
                float wobble = Mth.sin(state.age * 1.3f) * 0.12f;
                pose.translate(0, state.centreY, 0);
                pose.mulPose(Axis.YP.rotation((float) Math.atan2(motion.x, motion.z)));
                pose.mulPose(Axis.XP.rotation((float) -Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
                pose.scale(1 - wobble * 0.5f, 1 + wobble * 0.5f, STRETCH + wobble);
                pose.mulPose(Axis.ZP.rotation(state.age * 0.4f));
                var last = pose.last();
                box(consumer, last.pose(), last.normal(), RADIUS, RADIUS * CORE, RADIUS * CORE, r,g,b,state.lightCoords);
                box(consumer, last.pose(), last.normal(), RADIUS * CORE, RADIUS, RADIUS * CORE, r,g,b,state.lightCoords);
                box(consumer, last.pose(), last.normal(), RADIUS * CORE, RADIUS * CORE, RADIUS, r,g,b,state.lightCoords);
            });
        }
private static void box(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float hx, float hy, float hz,
								float r, float g, float b, int light) {
			quad(consumer, pose, normal, -hx, hy, -hz, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, 0, 1, 0, TOP_U0, 0.0F, 1.0F, PATCH_V1, r, g, b, light);
			quad(consumer, pose, normal, -hx, -hy, hz, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, 0, -1, 0, SHADE_U0, 0.0F, TOP_U0, PATCH_V1, r, g, b, light);
			quad(consumer, pose, normal, hx, hy, -hz, hx, -hy, -hz, -hx, -hy, -hz, -hx, hy, -hz, 0, 0, -1, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, normal, -hx, hy, hz, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, 0, 0, 1, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, normal, -hx, hy, -hz, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -1, 0, 0, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, normal, hx, hy, hz, hx, -hy, hz, hx, -hy, -hz, hx, hy, -hz, 1, 0, 0, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
		}
private static void quad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
								 float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
								 float nx, float ny, float nz, float u0, float v0, float u1, float v1, float r, float g, float b, int light) {
			corner(consumer, pose, normal, x1, y1, z1, u0, v0, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, normal, x2, y2, z2, u0, v1, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, normal, x3, y3, z3, u1, v1, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, normal, x4, y4, z4, u1, v0, nx, ny, nz, r, g, b, light);
		}
private static void corner(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v,
								   float nx, float ny, float nz, float r, float g, float b, int light) {
            Vector3f transformed = normal.transform(new Vector3f(nx, ny, nz));
			consumer.addVertex(pose, x, y, z).setColor(r, g, b, 1.0F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(transformed.x(), transformed.y(), transformed.z());
		}
    }

    @EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var mc = Minecraft.getInstance();
            if (mc.level == null) { GumBindEntity.clearClientBindings(); return; }
            if (mc.player == null || mc.level == null || mc.gui.screen() != null || !GumBindEntity.isClientBound(mc.player)) return;
            boolean pressed = false;
            while (mc.options.keyShift.consumeClick()) pressed = true;
            if (pressed) NetworkHandler.sendToServer(new GumStruggleC2S());
        }
    }

}
