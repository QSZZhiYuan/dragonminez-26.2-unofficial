package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.ClientGravityState;
import com.dragonminez.client.render.shader.GravityRedManager;
import com.dragonminez.client.render.util.IrisCompat;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GravityRedRenderer {

	private GravityRedRenderer() {}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
		handle(event, true);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAfterWeather(RenderLevelStageEvent.AfterWeather event) {
		handle(event, false);
	}

	/**
	 * 26.2 removed the {@code RenderLevelStageEvent.Stage} enum: each stage is now its own event
	 * subclass. The old code compared {@code event.getStage()} against a target chosen at runtime
	 * (iris -> AFTER_LEVEL, otherwise AFTER_WEATHER), so the equivalent is two subscribers that
	 * both funnel here and only act when they are the stage the current shader-pack state wants.
	 */
	private static void handle(RenderLevelStageEvent event, boolean afterLevel) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());
		boolean targetIsAfterLevel = iris;
		if (afterLevel != targetIsAfterLevel) return;

		float intensity = ClientGravityState.getShaderIntensity();
		if (intensity <= 0.0f) {
			GravityRedManager.reset();
			return;
		}

		if (iris) {
			GravityRedManager.process(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), intensity, false);
		} else {
			GravityRedManager.process(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), intensity);
		}
	}
}

