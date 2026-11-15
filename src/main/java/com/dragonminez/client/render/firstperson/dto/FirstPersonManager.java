package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import org.joml.Vector3f;

public class FirstPersonManager {

	/**
	 * Set once per frame from {@code FirstPersonListener} so render code deeper in the pipeline can
	 * tell whether a submit collector was available for this frame.
	 *
	 * The listener extracts the local player's body separately because vanilla omits the camera entity.
	 */
	private static boolean frameStatePublished;

	public static boolean shouldRenderFirstPerson(Player player) {
		// Vanilla owns first-person arms and held items, including for legacy configs that enabled
		// the full-body camera. DMZ's model and combat animations remain active in third person.
		return false;
	}

	public static Vector3f offsetFirstPersonView(Player player) {
		final float BASE_OFFSET_Y = 0.1F;
		final float[] BASE_OFFSET_Z = {0.3F};
		final float BASE_SCALE = 0.9375f;

		final float[][] scaling = {{BASE_SCALE, BASE_SCALE, BASE_SCALE}};

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			var character = data.getCharacter();

			Float[] modelScaling = character.getModelScaling();
			if (modelScaling != null && modelScaling.length >= 2) {
				scaling[0][0] = modelScaling[0];
				scaling[0][1] = modelScaling[1];
			}

			if (character.isOozaruCached()) {
				BASE_OFFSET_Z[0] = 1.3F;
			} else {
				Float[] resolved = character.getResolvedModelScaling();
				if (resolved != null && resolved.length >= 2) {
					scaling[0][0] = resolved[0];
					scaling[0][1] = resolved[1];
				}
			}
		});

		final float BASE_EYE_FRACTION = 0.85f;
		final float SHRINK_EYE_COMPENSATION = 1.5f;
		float eyeFraction = BASE_EYE_FRACTION + Math.max(0f, BASE_SCALE - scaling[0][1]) * SHRINK_EYE_COMPENSATION;

		float modelHeightInBlocks = scaling[0][1] * 1.8f;
		float eyeHeightInBlocks = modelHeightInBlocks * eyeFraction;
		float defaultEyeHeight = 1.42f;
		float adjustedOffsetY = BASE_OFFSET_Y + (eyeHeightInBlocks - defaultEyeHeight);

		return new Vector3f(0, adjustedOffsetY, BASE_OFFSET_Z[0]);
	}

	/** Called once per frame by the submit-geometry listener. */
	public static void publishFrameState(Player player, boolean collectorAvailable) {
		frameStatePublished = collectorAvailable;
	}

	/** True when this frame ran through the 26.2 submit-geometry path. */
	public static boolean isFrameStatePublished() {
		return frameStatePublished;
	}
}

