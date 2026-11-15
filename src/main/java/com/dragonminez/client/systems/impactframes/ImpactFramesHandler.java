package com.dragonminez.client.systems.impactframes;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.FullscreenPostEffect;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayDeque;
import java.util.Queue;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ImpactFramesHandler {

	private static final Queue<ImpactFrame> IMPACT_FRAMES = new ArrayDeque<>();
	private static ImpactFrame currentFrame = null;
	private static int currentTick = 0;
	private static final FullscreenPostEffect EFFECT =
			new FullscreenPostEffect("impact_frame");

	public static void addImpactFrame(ImpactFrame frame) {
		if (ConfigManager.getUserConfig().isImpactFramesEnabled()) {
			IMPACT_FRAMES.offer(frame);
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !ConfigManager.getUserConfig().isImpactFramesEnabled()) {
			IMPACT_FRAMES.clear();
			currentFrame = null;
			currentTick = 0;
			return;
		}
		if (mc.isPaused()) return;

		if (currentFrame == null) {
			if (!IMPACT_FRAMES.isEmpty()) {
				currentFrame = IMPACT_FRAMES.poll();
				currentTick = 0;
			}
		} else {
			currentTick++;
			if (currentTick >= currentFrame.getDuration()) {
				currentFrame = null;
				currentTick = 0;
				if (!IMPACT_FRAMES.isEmpty()) {
					currentFrame = IMPACT_FRAMES.poll();
				}
			}
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) {
		if (currentFrame == null) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.gameRenderer == null) return;

		EFFECT.process(currentFrame.getThreshold(), currentFrame.getThresholdLerp(),
				currentFrame.isInverted() ? 1.0f : 0.0f, 0.0f);
	}

}
