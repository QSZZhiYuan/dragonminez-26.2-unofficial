package com.dragonminez.client.render;



import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.client.model.KiWeaponModelLoader;
import com.dragonminez.client.render.firstperson.DMZPOVPlayerRenderer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import com.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DMZRendererCache {
	private DMZRendererCache() {}
	private static final Map<UUID, CachedRendererEntry> TP_RENDERERS = new HashMap<>();
	private static final Map<UUID, CachedRendererEntry> POV_RENDERERS = new HashMap<>();

	@Nullable
	private static EntityRendererProvider.Context context;

	public static void onResourceReload(EntityRendererProvider.Context ctx) {
		context = ctx;
		clear();
		KiWeaponModelLoader.clear();
		LogUtil.info(Env.CLIENT, "DMZRendererCache cleared on resource reload");
	}

	public static void clear() {
		TP_RENDERERS.clear();
		POV_RENDERERS.clear();
	}

	@Nullable
	public static DMZPlayerRenderer<?> getRenderer(Player player) {
		if (context == null || !(player instanceof AbstractClientPlayer acp)) return null;

		return StatsProvider.get(StatsCapability.INSTANCE, acp).map(data -> {
			var character = data.getCharacter();
			String race = character.getRaceName();
			String gender = character.getGender();
			String form = character.getActiveForm();

			boolean pov = FirstPersonManager.shouldRenderFirstPerson(player);
			String stateSignature = buildStateSignature(race, gender, form);

			Map<UUID, CachedRendererEntry> cache = pov ? POV_RENDERERS : TP_RENDERERS;
			CachedRendererEntry entry = cache.get(acp.getUUID());
			GeoEntityRenderer<?, com.dragonminez.client.render.GeoEntityRenderState> renderer = (entry != null && entry.matches(stateSignature)) ? entry.renderer() : null;

			if (renderer == null) {
				renderer = createRenderer(race, gender, form, pov);
				// Only cache a real renderer. Caching null kept the entry "matched" on later frames
				// (same signature), so the failure path was never retried and the player stayed
				// invisible forever instead of recovering once the cause was gone.
				if (renderer != null) {
					cache.put(acp.getUUID(), new CachedRendererEntry(stateSignature, renderer));
				}
			}

			return (renderer instanceof DMZPlayerRenderer<?> dmz) ? dmz : null;
		}).orElse(null);
	}


	/**
	 * 26.2 entry point: {@code EntityRenderer#submit} only receives the extracted render state, not
	 * the live entity, so resolve the cached renderer from {@link AvatarRenderState#id}.
	 *
	 * <p>The state carries no {@link AbstractClientPlayer}, which the stats lookup needs; the player is
	 * recovered from the client level by id. Returns {@code null} when the entity is not present (e.g.
	 * it left the view this frame), in which case the mixin falls through to vanilla rendering.
	 */
	@Nullable
	public static DMZPlayerRenderer<?> getRenderer(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
		if (context == null) return null;
		net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
		if (level == null) return null;
		if (!(level.getEntity(state.id) instanceof AbstractClientPlayer acp)) return null;
		return getRenderer(acp);
	}
	@Nullable
	public static DMZPlayerRenderer<?> getTPRenderer(Player player) {
		if (context == null || !(player instanceof AbstractClientPlayer acp)) return null;

		return StatsProvider.get(StatsCapability.INSTANCE, acp).map(data -> {
			var character = data.getCharacter();
			String race = character.getRaceName();
			String gender = character.getGender();
			String form = character.getActiveForm();

			String stateSignature = buildStateSignature(race, gender, form);

			CachedRendererEntry entry = TP_RENDERERS.get(acp.getUUID());
			GeoEntityRenderer<?, com.dragonminez.client.render.GeoEntityRenderState> renderer = (entry != null && entry.matches(stateSignature)) ? entry.renderer() : null;
			if (renderer == null) {
				renderer = createRenderer(race, gender, form, false);
				TP_RENDERERS.put(acp.getUUID(), new CachedRendererEntry(stateSignature, renderer));
			}

			return (renderer instanceof DMZPlayerRenderer<?> dmz) ? dmz : null;
		}).orElse(null);
	}

	@Nullable
	public static EntityRendererProvider.Context getContext() {
		return context;
	}

	private static String buildStateSignature(String race, String gender, String form) {
		String safeRace = race != null ? race.toLowerCase() : "human";
		String safeGender = gender != null ? gender.toLowerCase() : "male";
		String safeForm = form != null ? form.toLowerCase() : "base";
		return safeRace + "|" + safeGender + "|" + safeForm;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static GeoEntityRenderer<?, com.dragonminez.client.render.GeoEntityRenderState> createRenderer(String race, String gender, String form, boolean pov) {
		String safeRace = race != null ? race.toLowerCase() : "human";

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(safeRace);
		String customModel = (raceConfig != null) ? raceConfig.getCustomModel() : "";

		try {
			DMZPlayerModel model = new DMZPlayerModel<>(safeRace, customModel);
			if (pov) return new DMZPOVPlayerRenderer(context, model);
			return new DMZPlayerRenderer(context, model);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Error creating renderer for: {} (pov={})", safeRace, pov);

			// Fall back to the generic human model. This second attempt used to be unguarded: if it
			// also threw, the exception escaped createRenderer() and killed the render thread on the
			// very next frame, so one broken race took the whole game down. A null return now means
			// "draw nothing for this player" — bad, but survivable and diagnosable.
			try {
				DMZPlayerModel fallbackModel = new DMZPlayerModel<>("human", "");
				return pov ? new DMZPOVPlayerRenderer(context, fallbackModel)
						: new DMZPlayerRenderer(context, fallbackModel);
			} catch (Exception fatal) {
				LogUtil.error(Env.CLIENT, "Fallback renderer also failed for: {} (pov={}) - player will not render",
						safeRace, pov);
				return null;
			}
		}
	}

	private record CachedRendererEntry(String stateSignature, GeoEntityRenderer<?, com.dragonminez.client.render.GeoEntityRenderState> renderer) {
		private boolean matches(String otherSignature) {
			return stateSignature.equals(otherSignature);
		}
	}
}
