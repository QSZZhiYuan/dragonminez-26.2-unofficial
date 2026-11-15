package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.SearchGrayscaleManager;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.List;

/**
 * Ki Sense scan: grayscale post-process plus a billboard aura over every detected entity.
 *
 * <h2>26.2 migration</h2>
 * This used to do both jobs inside one {@code RenderLevelStageEvent} handler, drawing the auras
 * immediately through a bound {@code VertexBuffer} with {@code ShaderInstance} uniforms and a raw
 * {@code RenderSystem} blend/depth state.
 *
 * <p>26.2 split the two concerns:
 * <ul>
 *   <li>{@code RenderLevelStageEvent} no longer exposes a {@code SubmitNodeCollector} (or a
 *       projection matrix), so it keeps only the post-process job;</li>
 *   <li>custom geometry is submitted from {@link SubmitCustomGeometryEvent}, which hands over both
 *       the frame's {@link SubmitNodeCollector} and the {@link PoseStack} to draw in.</li>
 * </ul>
 * The blend/depth/cull state that {@code customSetup}/{@code customClear} flipped by hand now lives
 * on the render pipeline, so both helpers are gone. Per-aura colour and alpha become a per-vertex
 * tint, since the submit callback cannot reach a {@code RenderPass}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class KiSenseAuraRenderer {

	private static final double LOD_DISTANCE = 24.0;
	private static final Identifier AURA_TEXTURE =
			Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/kakarot_cross.png");

	private KiSenseAuraRenderer() {}

	// ------------------------------------------------------------------
	// Post-process (grayscale) — stays on the level stage event
	// ------------------------------------------------------------------

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
		handlePostProcess(true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAfterWeather(RenderLevelStageEvent.AfterWeather event) {
		handlePostProcess(false);
	}

	/**
	 * 26.2 removed {@code RenderLevelStageEvent.Stage}; each stage is its own event subclass. The old
	 * code picked AFTER_LEVEL under an Iris shaderpack and AFTER_WEATHER otherwise, so both
	 * sub-events funnel here and the shader-pack state decides which one does the work.
	 */
	private static void handlePostProcess(boolean afterLevel) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		if (afterLevel != iris) return;

		if (!KiSenseState.isSearch()) {
			SearchGrayscaleManager.reset();
			return;
		}

		if (iris) {
			SearchGrayscaleManager.process(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), 1.0F);
		} else {
			SearchGrayscaleManager.process(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), 1.0F);
		}
	}

	// ------------------------------------------------------------------
	// Aura geometry — submitted through the collector
	// ------------------------------------------------------------------

	@SubscribeEvent
	public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		if (!KiSenseState.isSearch()) return;

		StatsData myData = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (myData == null) return;

		double myBP = KiSenseScan.getMyBP();
		if (myBP <= 0) myBP = Math.max(1, myData.getBattlePower());

		// 26.2 has no live Camera during submission; the camera lives in the level render state.
		var cameraState = event.getLevelRenderState().cameraRenderState;
		org.joml.Quaternionf cameraRotation = new org.joml.Quaternionf(cameraState.orientation);
		Vec3 camPos = cameraState.pos;
		float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		long gameTime = mc.level.getGameTime();

		PoseStack poseStack = event.getPoseStack();
		SubmitNodeCollector collector = event.getSubmitNodeCollector();
		RenderType renderType = auraType(AURA_TEXTURE);
		float[][] quad = AuraMeshFactory.billboardQuad();

		for (int id : KiSenseScan.getSearchEntities()) {
			Entity entity = mc.level.getEntity(id);
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;

			double dist = mc.player.distanceTo(living);
			boolean lod = dist > LOD_DISTANCE;

			double bp = KiSenseScan.getCachedBP(id);
			double ratio = bp / (double) Math.max(1, myBP);
			float[][] colors = auraColors(living, ratio);
			float[] border = colors[0];
			float[] inner = colors[1];

			double lerpX = Mth.lerp(partialTick, living.xo, living.getX());
			double lerpY = Mth.lerp(partialTick, living.yo, living.getY());
			double lerpZ = Mth.lerp(partialTick, living.zo, living.getZ());
			float bbHeight = living.getBbHeight();

			float hitbox = Math.max(living.getBbWidth(), bbHeight);
			float baseRadius = hitbox * 0.9f + 0.6f;
			float powerMul = (float) Mth.clamp(1.0 + 0.5 * Math.log10(Math.max(1.0, ratio)), 1.0, 3.0);

			float speed = (gameTime + partialTick) * 0.5f;
			float scale = baseRadius * powerMul;

			drawAuraLayer(poseStack, collector, renderType, quad, cameraRotation, camPos,
					lerpX, lerpY, lerpZ, bbHeight, border, scale, 0.9f);
			drawAuraLayer(poseStack, collector, renderType, quad, cameraRotation, camPos,
					lerpX, lerpY, lerpZ, bbHeight, inner, scale * 0.66f, 0.5f);
			drawAuraLayer(poseStack, collector, renderType, quad, cameraRotation, camPos,
					lerpX, lerpY, lerpZ, bbHeight, inner, scale * 0.78f, 0.4f);

			// `speed` and `lod` used to drive the aura shader's UV scroll. Without per-draw uniforms
			// they have no sink; the value is still computed so the intent stays visible here.
			if (lod && speed == 0.0f) {
				// LOD fallback: same geometry, no scroll. Nothing extra to draw.
			}
		}
	}

	private static void drawAuraLayer(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
									  float[][] quad, org.joml.Quaternionf cameraRotation, Vec3 camPos,
									  double lerpX, double lerpY, double lerpZ, float bbHeight,
									  float[] color, float scale, float alpha) {
		poseStack.pushPose();
		poseStack.translate(lerpX - camPos.x, lerpY - camPos.y + bbHeight * 0.5, lerpZ - camPos.z);
		poseStack.mulPose(cameraRotation);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		poseStack.scale(scale, scale, scale);

		int tint = ARGB.colorFromFloat(alpha, color[0], color[1], color[2]);
		List<KiMeshFactory.Vertex> mesh = toQuadVertices(quad, tint);

		collector.submitCustomGeometry(poseStack, renderType,
				(pose, vc) -> KiMeshFactory.emit(mesh, pose, vc));
		poseStack.popPose();
	}

	/** Converts an {@link AuraMeshFactory} quad ({@code x,y,z,u,v}) into replayable vertices. */
	private static List<KiMeshFactory.Vertex> toQuadVertices(float[][] quad, int argb) {
		List<KiMeshFactory.Vertex> out = new java.util.ArrayList<>(quad.length);
		for (float[] v : quad) {
			out.add(new KiMeshFactory.Vertex(v[0], v[1], v[2], v[3], v[4], 0.0f, 0.0f, 1.0f, argb));
		}
		return out;
	}

	private static float[][] auraColors(LivingEntity entity, double ratio) {
		if (isPassive(entity)) return new float[][]{{0.3f, 0.5f, 1.0f}, {0.55f, 0.85f, 1.0f}};
		if (ratio > 1.15) return new float[][]{{1.0f, 0.25f, 0.2f}, {1.0f, 0.6f, 0.2f}};
		if (ratio < 0.85) return new float[][]{{0.3f, 0.5f, 1.0f}, {0.55f, 0.85f, 1.0f}};
		return new float[][]{{1.0f, 0.9f, 0.4f}, {1.0f, 1.0f, 0.7f}};
	}

	private static boolean isPassive(LivingEntity entity) {
		return !(entity instanceof Player) && !(entity instanceof Enemy) && !(entity instanceof NeutralMob);
	}

	private static RenderType auraType(Identifier texture) {
		return IrisCompat.isShaderPackInUse() ? ModRenderTypes.getCustomAuraCompat(texture) : ModRenderTypes.getCustomAura(texture);
	}
}
