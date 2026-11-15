package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.taiyoken.TaiyokenBlindState;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.dragonminez.client.gui.hud.DMZHudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class LockOnEvent {
	private static final Identifier LOCK_ICON = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/lock_on.png");
	private static LivingEntity lockedTarget = null;
	private static int scanTickCounter = 0;
	private static boolean markerVisible;
	private static float markerX;
	private static float markerY;
	private static float markerHalfSize = 16.0F;

	public static final DMZHudLayer HUD_LOCK_ON = (gui, deltaTracker) -> {
		if (!markerVisible || lockedTarget == null || !lockedTarget.isAlive()) return;

		long time = System.currentTimeMillis();
		boolean lod = Minecraft.getInstance().player != null && Minecraft.getInstance().player.distanceTo(lockedTarget) > 24.0;
		float angle = lod ? 0.0F : (time % 3600L) / 10.0F;

		gui.pose().pushMatrix();
		gui.pose().translate(markerX, markerY);
		gui.pose().rotate((float) Math.toRadians(angle));
		int size = Math.max(8, Math.round(markerHalfSize * 2.0F));
		gui.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, LOCK_ICON, -size / 2, -size / 2, 0.0F, 0.0F, size, size, 64, 64, 0xE600FFFF);
		gui.pose().popMatrix();

	};

	public static void toggleLock() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;
		if (TaiyokenBlindState.isActive()) {
			unlock();
			return;
		}
		if (lockedTarget != null) {
			unlock();
			return;
		}
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getSkills().hasSkill("kisense")) return;
			int level = data.getSkills().getSkillLevel("kisense");
			if (level <= 0) return;
			double range = 15.0 + 5.0 * level;
			if (data.getStatus().isAndroidUpgraded()) range += 25.0;
			findTargetInFront(player, range, data).ifPresent(target -> {
				lockedTarget = target;
				player.playSound(MainSounds.LOCKON.get());
			});
		});
	}

	public static void unlock() {
		lockedTarget = null;
		markerVisible = false;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || lockedTarget == null) return;
		if (com.dragonminez.client.systems.taiyoken.TaiyokenBlindState.isActive()) {
			unlock();
			return;
		}
		scanTickCounter++;
		if (scanTickCounter >= 5) {
			scanTickCounter = 0;
			if (!lockedTarget.isAlive()) {
				unlock();
				return;
			}
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int level = data.getSkills().getSkillLevel("kisense");
				if (level <= 0 || data.getSkills().getSkill("kisense") == null) {
					unlock();
					return;
				}
				double maxRange = 15.0 + 5.0 * level;
				if (data.getStatus().isAndroidUpgraded()) maxRange += 25.0;
				if (player.distanceTo(lockedTarget) > maxRange) {
					unlock();
					return;
				}
				if (!KiSenseScan.canTarget(lockedTarget, data)) {
					unlock();
					return;
				}
				if (!player.hasLineOfSight(lockedTarget) && !data.getStatus().isAndroidUpgraded()) unlock();
			});
		}
	}

	@SubscribeEvent
	public static void onRenderTick(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || lockedTarget == null) return;
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double targetX = Mth.lerp(partialTick, lockedTarget.xo, lockedTarget.getX());
		double targetY = Mth.lerp(partialTick, lockedTarget.yo, lockedTarget.getY()) + lockedTarget.getBbHeight() * 0.5;
		double targetZ = Mth.lerp(partialTick, lockedTarget.zo, lockedTarget.getZ());
		Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
		Vec3 playerPos = player.getEyePosition(partialTick);
		double dX = targetPos.x - playerPos.x;
		double dY = targetPos.y - playerPos.y;
		double dZ = targetPos.z - playerPos.z;
		double dist = Math.sqrt(dX * dX + dZ * dZ);
		float targetYaw = (float) (Mth.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
		float targetPitch = (float) -(Mth.atan2(dY, dist) * (180 / Math.PI));
		float smoothFactor = 0.15F;
		float newYaw = rotlerp(player.getYRot(), targetYaw, smoothFactor);
		float newPitch = rotlerp(player.getXRot(), targetPitch, smoothFactor);
		player.setYRot(newYaw);
		player.setXRot(newPitch);
	}

	@SubscribeEvent
	public static void onRenderWorldLast(RenderLevelStageEvent.AfterLevel event) {
		Minecraft mc = Minecraft.getInstance();
		// 26.2: stage is guaranteed by the event subclass
		if (lockedTarget == null || !lockedTarget.isAlive()) {
			markerVisible = false;
			return;
		}

		// Project the target during the world pass, then render the original rotating
		// icon as a real GUI layer. This keeps it above every 1.21 framebuffer composite
		// (Fabulous, Iris, Aeronautics/Sable) instead of letting later passes erase it.
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double lerpX = Mth.lerp(partialTick, lockedTarget.xo, lockedTarget.getX());
		double lerpY = Mth.lerp(partialTick, lockedTarget.yo, lockedTarget.getY());
		double lerpZ = Mth.lerp(partialTick, lockedTarget.zo, lockedTarget.getZ());
		Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
		Vector4f view = new Vector4f(
				(float) (lerpX - cameraPos.x),
				(float) ((lerpY - cameraPos.y) + lockedTarget.getBbHeight() * 0.5F),
				(float) (lerpZ - cameraPos.z), 1.0F).mul(event.getModelViewMatrix());
		Vector4f clip = new Vector4f(view).mul(event.getLevelRenderState().cameraRenderState.projectionMatrix);
		if (clip.w <= 0.001F) {
			markerVisible = false;
			return;
		}

		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;
		float ndcZ = clip.z / clip.w;
		if (ndcZ < -1.0F || ndcZ > 1.0F || Math.abs(ndcX) > 1.15F || Math.abs(ndcY) > 1.15F) {
			markerVisible = false;
			return;
		}

		int guiWidth = mc.getWindow().getGuiScaledWidth();
		int guiHeight = mc.getWindow().getGuiScaledHeight();
		markerX = (ndcX * 0.5F + 0.5F) * guiWidth;
		markerY = (0.5F - ndcY * 0.5F) * guiHeight;

		Vector4f edge = new Vector4f(view.x + 0.64F, view.y, view.z, 1.0F).mul(event.getLevelRenderState().cameraRenderState.projectionMatrix);
		float edgeNdcX = edge.w > 0.001F ? edge.x / edge.w : ndcX;
		markerHalfSize = Mth.clamp(Math.abs(edgeNdcX - ndcX) * guiWidth * 0.5F, 8.0F, 64.0F);
		markerVisible = true;
	}

	private static Optional<LivingEntity> findTargetInFront(Player player, double range, StatsData data) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(viewVec.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0);
		List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive() && e.isPickable() && canTarget(e, data));
		LivingEntity closest = null;
		double closestDist = range * range;
		for (LivingEntity e : list) {
			AABB axisalignedbb = e.getBoundingBox().inflate(e.getPickRadius());
			Optional<Vec3> hit = axisalignedbb.clip(eyePos, endPos);
			if (e.isInvisible() || e.isInvisibleTo(player) || !player.hasLineOfSight(e)) continue;
			if (axisalignedbb.contains(eyePos)) {
				if (closestDist >= 0.0) {
					closest = e;
					closestDist = 0.0;
				}
			} else if (hit.isPresent()) {
				double dist = eyePos.distanceToSqr(hit.get());
				if (dist < closestDist) {
					closest = e;
					closestDist = dist;
				}
			}
		}
		return Optional.ofNullable(closest);
	}

	private static float rotlerp(float start, float target, float amount) {
		float f = Mth.wrapDegrees(target - start);
		if (f > 180.0F) f -= 360.0F;
		if (f < -180.0F) f += 360.0F;
		return start + amount * f;
	}

	public static boolean canTarget(LivingEntity target, StatsData myData) {
		if (!(target instanceof Player targetPlayer)) return true;
		StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).orElse(null);
		if (targetData == null) return true;
		if (KiSenseScan.isCloaked(targetPlayer)) return false;
		if (TransformationsHelper.hasGodFormActive(targetData) && myData.getSkills().getSkillLevel("godforms") <= 0) return false;
		return true;
	}

	@java.lang.SuppressWarnings("all")
	public static LivingEntity getLockedTarget() {
		return LockOnEvent.lockedTarget;
	}
}

