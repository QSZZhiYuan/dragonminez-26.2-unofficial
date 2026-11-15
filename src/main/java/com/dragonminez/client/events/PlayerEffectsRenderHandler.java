package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.render.effects.Aura3DRenderer;
import com.dragonminez.client.render.shader.EffectBloomRenderer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.KiBloomRenderer;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.RenderBufferUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class PlayerEffectsRenderHandler {
    private static final Matrix4f FRAME_PROJECTION = new Matrix4f();
    private static final net.minecraft.client.renderer.SubmitNodeStorage LATE_AURAS = new net.minecraft.client.renderer.SubmitNodeStorage();
	private static final Set<Integer> CURRENT_FRAME_PLAYERS = new HashSet<>();
	/** Current ki-attack alpha, read by the geometry submit path (see DmzMaterial). */
	private static final float[] kiAlphaHolder = {1.0f};

	@SubscribeEvent
	public static void onRenderTick(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        Aura3DRenderer.beginFrame();
        com.dragonminez.client.render.effects.AuraBorderRenderer.beginFrame();
        EffectBloomRenderer.beginFrame();
        KiBloomRenderer.beginFrame();
        com.dragonminez.client.render.shader.TransformationOutlineRenderer.beginFrame();
		{
			PlayerEffectQueue.getAndClearAuras();
			PlayerEffectQueue.getAndClearSparks();
			PlayerEffectQueue.getAndClearFirstPersonAuras();
			PlayerEffectQueue.getAndClearKiAttacks();
			PlayerEffectQueue.getAndClearEntityEffects();
		}
	}

	/**
	 * 26.2: {@code Minecraft#renderBuffers()} no longer exists, so the frame's
	 * {@code SubmitNodeCollector} is published from the event that actually offers one. The stage
	 * subscribers below then read it back through {@code RenderBufferUtil#frameCollector()}.
	 */
	@SubscribeEvent
	public static void onSubmitCustomGeometry(net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event) {
		FRAME_PROJECTION.set(event.getLevelRenderState().cameraRenderState.projectionMatrix);
		RenderBufferUtil.publishFrameCollector(event.getSubmitNodeCollector());
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) renderEffects(mc, event);
	}

	@SubscribeEvent
	public static void onAfterSky(RenderLevelStageEvent.AfterSky event) {
		handleStage(event, true);
	}

	@SubscribeEvent
	public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        // LevelRenderer has already popped its view matrix before AfterLevel fires.
        // RenderType.prepare() must still see the world camera for the captured meshes.
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        try {
            view.set(event.getModelViewMatrix());
            handleStage(event, false);
            KiBloomRenderer.flush();
            com.dragonminez.client.render.shader.TransformationOutlineRenderer.flush(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            com.dragonminez.client.render.effects.AuraBorderRenderer.flush(new Matrix4f(event.getModelViewMatrix()), FRAME_PROJECTION,
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            Aura3DRenderer.flush(new Matrix4f(event.getModelViewMatrix()), FRAME_PROJECTION);
            EffectBloomRenderer.composite();
        } finally { view.popMatrix(); }
	}

	/**
	 * 26.2 removed the {@code RenderLevelStageEvent.Stage} enum in favour of one event subclass per
	 * stage, so the single {@code onRenderLevelStage} dispatcher becomes one subscriber per stage.
	 *
	 * @param afterSky {@code true} for {@link RenderLevelStageEvent.AfterSky},
	 *                 {@code false} for {@link RenderLevelStageEvent.AfterLevel}
	 */
	private static void handleStage(RenderLevelStageEvent event, boolean afterSky) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean shaderPack = IrisCompat.isShaderPackInUse(mc.level.getGameTime());

		if (shaderPack) {

			if (afterSky) {
				TransformationPostShaderManager.setShaderpackMainPass(true);
			} else {
				TransformationPostShaderManager.setShaderpackMainPass(false);
                // Submit into a separate collection before preparation, then draw after Iris
                // has finished the world. The world's collector has already been consumed here.
                mc.gameRenderer.featureRenderDispatcher().renderAllFeatures(LATE_AURAS);
				TransformationPostShaderManager.processShaderpackOutline(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
			}
			return;
		}

		// Geometry must be submitted before FeatureRenderDispatcher prepares this frame.
		// AfterLevel runs after that frame has been drawn and its collector consumed.
	}

	private static void renderEffects(Minecraft mc, net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event) {
		SubmitNodeCollector buffers = event.getSubmitNodeCollector();
		// The engine applies the camera view matrix at draw time. Keep the supplied entity-space
		// pose here; baking the view matrix into it rotates every effect twice.
		PoseStack poseStack = event.getPoseStack();
		Matrix4f projectionMatrix = new Matrix4f(event.getLevelRenderState().cameraRenderState.projectionMatrix);
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		long gameTime = mc.level.getGameTime();

		CURRENT_FRAME_PLAYERS.clear();

		boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
		boolean isCameraColliding = false;

		// 26.2: Minecraft#cameraEntity is gone - the camera entity comes from the main camera.
		var cameraEntity = mc.gameRenderer.mainCamera().entity();
		if (!isFirstPerson && cameraEntity != null) {
			Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
			Vec3 entityPos = cameraEntity.getEyePosition(partialTick);
			if (cameraPos.distanceToSqr(entityPos) < 0.25) isCameraColliding = true;
		}

		AuraRenderer.processFusionFlashes(mc, gameTime, partialTick, poseStack, buffers);

		var kiAttacks = PlayerEffectQueue.getAndClearKiAttacks();
		if (!kiAttacks.isEmpty()) {
			float kiAlpha = isFirstPerson ? 0.35f : 0.85f;
			// 26.2: per-draw uniforms live in the DmzMaterial std140 block, not on a ShaderInstance handle.
			// The alpha is carried by the caller-supplied material when the ki geometry is submitted.
			kiAlphaHolder[0] = kiAlpha;


			for (var task : kiAttacks) {
				task.render(poseStack, projectionMatrix);
			}


			if (!IrisCompat.isShaderPackInUse(gameTime)) {
				KiBloomRenderer.render(kiAttacks, poseStack, projectionMatrix, partialTick);
			}

			kiAlphaHolder[0] = 1.0f;
		}

        SubmitNodeCollector auraCollector = IrisCompat.isShaderPackInUse() ? LATE_AURAS : buffers;
		AuraRenderer.processThirdPersonAuras(mc, poseStack, auraCollector, CURRENT_FRAME_PLAYERS, isFirstPerson, isCameraColliding);
		AuraRenderer.processFirstPersonAuras(mc, poseStack, auraCollector, partialTick, CURRENT_FRAME_PLAYERS, isFirstPerson, event.getLevelRenderState().cameraRenderState);
		AuraRenderer.processGhostAuras(mc, poseStack, auraCollector, partialTick, CURRENT_FRAME_PLAYERS);
		AuraRenderer.processSparks(poseStack, auraCollector, isFirstPerson, event.getLevelRenderState().cameraRenderState);
		AuraRenderer.processChargeBursts(mc, poseStack, auraCollector, partialTick);

		var entityEffects = PlayerEffectQueue.getAndClearEntityEffects();
		for (var task : entityEffects) task.render();

		AuraRenderer.cleanCaches(CURRENT_FRAME_PLAYERS);
	}
}


