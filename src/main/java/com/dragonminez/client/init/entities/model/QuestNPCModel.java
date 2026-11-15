package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic GeckoLib model for QuestNPCEntity.
 * Resolves quest NPC visuals from existing saga/master assets with a Goku fallback.
 */
public class QuestNPCModel extends GeoModel<QuestNPCEntity> {

	private static final String FALLBACK_MODEL = "saga_goku";
	private static final String FALLBACK_TEXTURE = "saga_goku_early";
	private static final String SAGA_BASE_ANIMATION = "entity/sagas/saga_base";

	private static final AssetPaths FALLBACK = saga(FALLBACK_MODEL, FALLBACK_TEXTURE);
	private static final Map<String, AssetPaths> NPC_ASSETS = Map.ofEntries(
			Map.entry("generic_npc", FALLBACK),
			Map.entry("goku", FALLBACK),
			Map.entry("bulma", saga("saga_bulma", "saga_bulma")),
			Map.entry("krillin", saga("saga_vegeta", "saga_krillin")),
			Map.entry("yamcha", saga("saga_yamcha", "saga_yamcha")),
			Map.entry("tien", saga("saga_goku", "saga_tien_early")),
			Map.entry("chiaotzu", saga("saga_chaoz", "saga_chaoz")),
			Map.entry("piccolo", saga("saga_piccolo", "saga_piccolo")),
			Map.entry("gohan", saga("saga_gohan_mid", "saga_gohan_mid_base")),
			Map.entry("vegeta", saga("saga_vegeta", "saga_vegeta")),
			Map.entry("trunks", saga("saga_trunks", "saga_ftrunks_base")),
			Map.entry("videl", saga("saga_videl", "saga_videl")),
			Map.entry("shin", saga("saga_shin", "saga_shin")),
			Map.entry("namek_elder", master("master_guru"))
	);

	/** Cache which resource keys have been confirmed to have assets, to avoid repeated resource lookups. */
	private static final Set<String> VALID_GEO_KEYS = new HashSet<>();
	private static final Set<String> VALID_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> VALID_ANIMATION_KEYS = new HashSet<>();
	private static final Set<String> MISSING_GEO_KEYS = new HashSet<>();
	private static final Set<String> MISSING_TEXTURE_KEYS = new HashSet<>();
	private static final Set<String> MISSING_ANIMATION_KEYS = new HashSet<>();

    // GeckoLib 5: the entity is unavailable at texture time, so the per-NPC keys are stashed in
    // the render state by addAdditionalStateData and read back in getModel/getTextureResource.
    private static final com.geckolib.constant.dataticket.DataTicket<AssetPaths> DMZ_ASSETS =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_npc_assets", AssetPaths.class);
    private static final com.geckolib.constant.dataticket.DataTicket<String> DMZ_MODEL_KEY =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_npc_model_key", String.class);
    private static final com.geckolib.constant.dataticket.DataTicket<String> DMZ_TEXTURE_KEY =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_npc_texture_key", String.class);

	@Override
	public void addAdditionalStateData(QuestNPCEntity animatable, Object unused, GeoRenderState renderState) {
		renderState.addGeckolibData(DMZ_ASSETS, resolveAsset(animatable));
		renderState.addGeckolibData(DMZ_MODEL_KEY, animatable.getModelKey());
		renderState.addGeckolibData(DMZ_TEXTURE_KEY, animatable.getTextureKey());
	}

	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		AssetPaths asset = renderState.getGeckolibData(DMZ_ASSETS);
		if (asset == null) asset = FALLBACK;
		return existingOrFallback(asset.model(), fallbackModel(), VALID_GEO_KEYS, MISSING_GEO_KEYS, GeoAssetPaths::modelFile);
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		String textureKey = renderState.getGeckolibData(DMZ_TEXTURE_KEY);
		AssetPaths asset = renderState.getGeckolibData(DMZ_ASSETS);
		if (asset == null) asset = FALLBACK;
		Identifier texture = assetFromKey(textureKey, "textures/entity/sagas/", ".png");
		if (resourceExistsCached(texture, VALID_TEXTURE_KEYS, MISSING_TEXTURE_KEYS)) {
			return texture;
		}
		return existingOrFallback(asset.texture(), fallbackTexture(), VALID_TEXTURE_KEYS, MISSING_TEXTURE_KEYS, java.util.function.Function.identity());
	}

	@Override
	public Identifier getAnimationResource(QuestNPCEntity animatable) {
		AssetPaths asset = resolveAsset(animatable);
		return existingOrFallback(asset.animation(), fallbackAnimation(), VALID_ANIMATION_KEYS, MISSING_ANIMATION_KEYS, GeoAssetPaths::animationFile);
	}

	/**
	 * Checks whether a resource exists in the current resource manager.
	 */
	private static boolean resourceExists(Identifier location) {
		try {
			ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
			return resourceManager.getResource(location).isPresent();
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean resourceExistsCached(Identifier location, Set<String> valid, Set<String> missing) {
		String key = location.toString();
		if (valid.contains(key)) {
			return true;
		}
		if (missing.contains(key)) {
			return false;
		}
		if (resourceExists(location)) {
			valid.add(key);
			return true;
		}
		missing.add(key);
		return false;
	}

	private static Identifier existingOrFallback(Identifier candidate, Identifier fallback,
			Set<String> valid, Set<String> missing, java.util.function.Function<Identifier, Identifier> filePath) {
		return resourceExistsCached(filePath.apply(candidate), valid, missing) ? candidate : fallback;
	}

	private static AssetPaths resolveAsset(QuestNPCEntity animatable) {
		String modelKey = animatable.getModelKey();
		AssetPaths explicit = NPC_ASSETS.get(modelKey);
		if (explicit != null) {
			return explicit;
		}
		AssetPaths byNpc = NPC_ASSETS.get(animatable.getNpcId());
		if (byNpc != null && (modelKey == null || modelKey.isBlank() || modelKey.equals(animatable.getNpcId()))) {
			return byNpc;
		}
		Identifier sagaModel = assetFromKey(modelKey, "entity/sagas/", "");
		if (resourceExistsCached(GeoAssetPaths.modelFile(sagaModel), VALID_GEO_KEYS, MISSING_GEO_KEYS)) {
			return saga(modelKey, animatable.getTextureKey());
		}
		Identifier masterModel = assetFromKey(modelKey, "entity/master/", "");
		if (resourceExistsCached(GeoAssetPaths.modelFile(masterModel), VALID_GEO_KEYS, MISSING_GEO_KEYS)) {
			return master(modelKey);
		}
		return byNpc != null ? byNpc : FALLBACK;
	}

	private static AssetPaths saga(String modelKey, String textureKey) {
		String safeTexture = textureKey == null || textureKey.isBlank() ? FALLBACK_TEXTURE : textureKey;
		return new AssetPaths(
				assetFromKey(modelKey, "entity/sagas/", ""),
				assetFromKey(safeTexture, "textures/entity/sagas/", ".png"),
				Identifier.fromNamespaceAndPath(Reference.MOD_ID, SAGA_BASE_ANIMATION)
		);
	}

	private static AssetPaths master(String key) {
		return new AssetPaths(
				assetFromKey(key, "entity/master/", ""),
				assetFromKey(key, "textures/entity/master/", ".png"),
				assetFromKey(key, "entity/master/", "")
		);
	}

	private static Identifier assetFromKey(String key, String prefix, String suffix) {
		String safeKey = key == null || key.isBlank() ? FALLBACK_MODEL : key;
		if (safeKey.contains(":")) {
			return Identifier.parse(safeKey);
		}
		if (safeKey.contains("/")) {
			return Identifier.fromNamespaceAndPath(Reference.MOD_ID, safeKey);
		}
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, prefix + safeKey + suffix);
	}

	private static Identifier fallbackModel() {
		return FALLBACK.model();
	}

	private static Identifier fallbackTexture() {
		return FALLBACK.texture();
	}

	private static Identifier fallbackAnimation() {
		return FALLBACK.animation();
	}

	/**
	 * Clears the asset cache. Call on resource reload if needed.
	 */
	public static void clearCache() {
		VALID_GEO_KEYS.clear();
		VALID_TEXTURE_KEYS.clear();
		VALID_ANIMATION_KEYS.clear();
		MISSING_GEO_KEYS.clear();
		MISSING_TEXTURE_KEYS.clear();
		MISSING_ANIMATION_KEYS.clear();
	}

	private record AssetPaths(Identifier model, Identifier texture, Identifier animation) {
	}
}

