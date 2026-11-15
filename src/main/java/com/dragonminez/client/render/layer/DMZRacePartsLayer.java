package com.dragonminez.client.render.layer;

import com.dragonminez.client.util.BoneVisibilityHandler;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.dragonminez.Reference;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import top.theillusivec4.curios.api.CuriosApi;

public class DMZRacePartsLayer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerGeoLayer<T> {
	private com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> sourcePass;
	private static final Identifier RACES_PARTS_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/raceparts");
	private static final Identifier RACES_PARTS_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/raceparts.png");
	private static final Identifier RACES_PARTS_SHADOW_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/raceparts_shadow.png");

	private static final Identifier ACCESORIES_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/accesories");
	private static final Identifier SCOUTER_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/scouter");

	private static final Identifier YAJIROBE_SWORD_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/yajirobe_katana");
	private static final Identifier YAJIROBE_SWORD_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/yajirobe_katana.png");
	private static final Identifier Z_SWORD_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/z_sword");
	private static final Identifier Z_SWORD_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/z_sword.png");
	private static final Identifier BRAVE_SWORD_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/brave_sword");
	private static final Identifier BRAVE_SWORD_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/brave_sword.png");
	private static final Identifier POWER_POLE_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/power_pole");

	private static  float BRAVE_BACK_X = 0.7F;
	private static  float BRAVE_BACK_Y = 2.0F;
	private static  float BRAVE_BACK_Z = 0.15F;
	private static  float BRAVE_BACK_ROT_X = 0.0F;
	private static  float BRAVE_BACK_ROT_Y = 0.0F;
	private static  float BRAVE_BACK_ROT_Z = 135.0f;
	private static  float BRAVE_BACK_SCALE = 0.9F;
	private static final Identifier POWER_POLE_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/power_pole.png");

	private static final Identifier WEIGHTED_ITEMS_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/weighted_items");
	private static final Identifier WEIGHTED_ITEMS_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");

	public DMZRacePartsLayer(GeoRenderer<T, ?, ?> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	protected boolean skipInHudPortrait() {
		return false;
	}

	@Override
	protected void render(DMZLayerContext ctx) {
		@SuppressWarnings("unchecked") T animatable = (T) ctx.player();
		if (animatable == null) return;
		var collector = ctx.collector();
		PoseStack poseStack = ctx.poseStack();
		BakedGeoModel playerModel = ctx.model();
		float partialTick = ctx.partialTick();
		int packedLight = ctx.packedLight();
		int packedOverlay = ctx.packedOverlay();
		var info = ctx.info();

		var stats = ctx.stats();
		if (stats == null) return;

		int playerId = animatable.getId();
		long gameTime = animatable.level().getGameTime();
		boolean shouldFadeIn = stats.getStatus().isChargingKi()
				|| stats.getStatus().isAuraActive()
				|| stats.getStatus().isPermanentAura();
		AuraTintTracker.update(playerId, gameTime, shouldFadeIn);

		// GeckoLib 5 invokes layers once per render pass instead of once per bone, so the bones
		// this layer cares about are walked explicitly.
		sourcePass = info;
		try {
			for (String anchorName : RENDER_ANCHORS) {
				GeoBone bone = playerModel.getBone(anchorName).orElse(null);
				if (bone == null) continue;
				renderForAnchor(anchorName, bone, poseStack, animatable, collector, info, partialTick, packedLight, packedOverlay, stats, AuraTintTracker.get(playerId));
			}
		} finally {
			sourcePass = null;
		}
	}

	/** Bones that race accessories / weapons / weighted items are anchored to. */
	private static final String[] RENDER_ANCHORS = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

	private void renderForAnchor(String anchor, GeoBone playerBone, PoseStack poseStack, T animatable, net.minecraft.client.renderer.SubmitNodeCollector collector, com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState> info, float partialTick, int packedLight, int packedOverlay, StatsData stats, float alpha) {
		boolean isLimb = "right_arm".equals(anchor) || "left_arm".equals(anchor)
				|| "right_leg".equals(anchor) || "left_leg".equals(anchor);
		if (!"head".equals(anchor) && !"body".equals(anchor) && !isLimb) return;

		if (animatable.hasEffect(MainEffects.CANDY)) return;

		if (FirstPersonManager.shouldRenderFirstPerson(animatable)) {
			// stats supplied by caller
			if ("body".equals(anchor) && !animatable.isSpectator() && stats.getStatus().isHasCreatedCharacter()) {
				boolean isOozaru = stats.getCharacter().getActiveForm() != null && stats.getCharacter().getActiveForm().contains("ozaru");
				if (!isOozaru && stats.getStatus().isRenderKatana()) {
					BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
					if (yajirobeModel != null) {
						RenderType type = RenderTypes.entityCutout(YAJIROBE_SWORD_TEXTURE);
						renderWeaponFromBodyAnchor(yajirobeModel, "katana", playerBone, poseStack, collector, animatable, type, partialTick, packedLight, 1.0f);
					}
				}
			}
			// submitted via collector
			return;
		}


		if (isLimb) {
			if (!animatable.isSpectator() && !stats.getCharacter().isOozaruCached()) {
				renderWeightedItems(poseStack, animatable, collector, anchor, partialTick, packedLight, alpha);
			}
			// submitted via collector
			return;
		}

		float tintProgress = AuraTintTracker.get(animatable.getId());

		BakedGeoModel playerModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(info.renderState()));

		renderRacePartsForAnchor(poseStack, animatable, playerModel, collector, stats, anchor, partialTick, packedLight, alpha, tintProgress);

		if (!animatable.isSpectator()) {
			if ("head".equals(anchor) && !stats.getCharacter().isOozaruCached()) {
				renderAccessories(poseStack, animatable, playerModel, collector, partialTick, packedLight);
				renderScouter(poseStack, animatable, playerModel, collector, partialTick, packedLight);
			} else {
				renderSword(poseStack, animatable, playerBone, collector, partialTick, packedLight);
				if ("body".equals(anchor) && !stats.getCharacter().isOozaruCached()) {
					renderWeightedItems(poseStack, animatable, collector, anchor, partialTick, packedLight, alpha);
				}
			}
		}

		// submitted via collector
	}

	private void renderRacePartsForAnchor(PoseStack poseStack, T animatable, BakedGeoModel playerModel, net.minecraft.client.renderer.SubmitNodeCollector collector, StatsData stats, String anchor, float partialTick, int packedLight, float alpha, float tintProgress) {
		var character = stats.getCharacter();
		var isAlive = stats.getStatus().isAlive();
		String race = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();

		boolean isOozaru = currentForm != null && currentForm.toLowerCase().contains("oozaru");
		if (isOozaru) return;

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
		if (raceConfig == null) return;

		String formCustomModel = "";
		boolean hasStackForm = character.hasActiveStackForm() && character.getActiveStackFormData() != null;
		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;

		if (hasStackForm && character.getActiveStackFormData().hasCustomModel()) {
			formCustomModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (hasForm && character.getActiveFormData().hasCustomModel()) {
			formCustomModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		String raceCustomModel = raceConfig.getCustomModel() != null ? raceConfig.getCustomModel().toLowerCase() : "";
		String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (key.isEmpty()) key = race;

		String logicKey = key;

		BakedGeoModel partsModel = getGeoModel().getBakedModel(RACES_PARTS_MODEL);
		if (partsModel == null) return;

		RenderType partsRenderType = RenderTypes.entityTranslucent(RACES_PARTS_TEXTURE);
		FormConfig.FormData tintForm = DMZSkinLayer.resolveTintForm(stats);
		float[] formTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;
		float formTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
		float[] topAuraColor = getTopAuraColor(stats);

		float[] accessoryColor = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getHairColor().isEmpty()) {
			accessoryColor = character.getActiveFormData().getRgbHairColor();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getHairColor().isEmpty()) {
			accessoryColor = character.getActiveStackFormData().getRgbHairColor();
		}

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					accessoryColor = DMZSkinLayer.lerpColor(factor, accessoryColor, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					accessoryColor = DMZSkinLayer.lerpColor(factor, accessoryColor, nextForm.getRgbHairColor());
				}
			}
		}

		if (anchor.equals("head")) {
			boolean extraHeadBonesEnabled = character.areExtraHeadBonesEnabled();
			String activeBone = character.getRenderableHeadBone();

			if (extraHeadBonesEnabled && activeBone != null && !activeBone.isEmpty() && !activeBone.equals("hair")) {
				for (String boneName : activeBone.split("\\+")) {
					if (boneName.isEmpty() || boneName.equals("hair")) continue;
					GeoBone targetBone = partsModel.getBone(boneName).orElse(null);
					boolean fromPlayerModel = false;
					if (targetBone == null) {
						targetBone = playerModel.getBone(boneName).orElse(null);
						fromPlayerModel = true;
					}
					if (targetBone != null) {
						if (!fromPlayerModel) {
							syncTargetBoneAndParents(targetBone, playerModel);
						}

						float[] colorToTint = accessoryColor;

						if (character.getRaceName().equals("majin") || character.getRaceName().equals("namekian")) {
							colorToTint = resolveBodyColor1(stats);
						}

						float[] tintedColor = applyAuraTint(colorToTint[0], colorToTint[1], colorToTint[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);

						if (boneName.contains("horn") && character.getRaceName().equals("frostdemon")) {
							tintedColor = ColorUtils.hexToRgb("#1A1A1A");
						}

						renderTargetedBone(targetBone, poseStack, collector, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
					}
				}
			}

			if (extraHeadBonesEnabled && (race.equals("namekian") || logicKey.equals("namekian_orange") || logicKey.equals("namekian_buffed"))) {

				GeoBone antennaBone = partsModel.getBone("antennas1").orElse(null);
				boolean antennaFromPlayerModel = false;

				if (antennaBone == null) {
					antennaBone = playerModel.getBone("antennas1").orElse(null);
					antennaFromPlayerModel = true;
				}

				if (antennaBone != null) {
					if (!antennaFromPlayerModel) {
						syncTargetBoneAndParents(antennaBone, playerModel);
					}
					float[] antennaColor = resolveBodyColor1(stats);
					float[] tintedColor = applyAuraTint(antennaColor[0], antennaColor[1], antennaColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
					renderTargetedBone(antennaBone, poseStack, collector, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

			if (extraHeadBonesEnabled && race.equals("majin")) {
				float[] majinBodyColor = resolveBodyColor1(stats);

				GeoBone earsBone = partsModel.getBone("ears3").orElse(null);
				boolean earsFromPlayerModel = false;

				if (earsBone == null) {
					earsBone = playerModel.getBone("ears3").orElse(null);
					earsFromPlayerModel = true;
				}

				if (earsBone != null) {
					if (!earsFromPlayerModel) {
						syncTargetBoneAndParents(earsBone, playerModel);
					}

					float[] tintedColor = applyAuraTint(majinBodyColor[0], majinBodyColor[1], majinBodyColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
					renderTargetedBone(earsBone, poseStack, collector, animatable, partsRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
				}
			}

			if (!stats.getStatus().isAlive() || stats.getStatus().isForceHalo()) {
				GeoBone haloBone = partsModel.getBone("halo").orElse(null);

				if (haloBone != null) {
					syncTargetBoneAndParents(haloBone, playerModel);

					float[] haloColor = ColorUtils.hexToRgb("#FFF461");

					renderTargetedBone(haloBone, poseStack, collector, animatable,
							ModRenderTypes.energy(RACES_PARTS_TEXTURE),
							haloColor[0], haloColor[1], haloColor[2], 0.75f, partialTick, packedLight);
				}
			}
		}

		if (anchor.equals("body")) {
			boolean isSaiyanLogic = race.equals("saiyan");
			boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();

			if ((isSaiyanLogic || hasSaiyanTail) && !stats.getStatus().isTailVisible() && character.isHasSaiyanTail()) {
				RenderType tailRenderType = RenderTypes.entityTranslucent(RACES_PARTS_TEXTURE);
				partsModel.getBone("tailenrolled").ifPresent(targetBone -> {
					syncTargetBoneAndParents(targetBone, playerModel);
					float[] tailColor = ColorUtils.hexToRgb("#572117");

					if (character.getBodyColor2() != null && !character.getBodyColor2().isEmpty()) {
						tailColor = character.getRgbBodyColor2();
					}
					if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor2().isEmpty()) {
						tailColor = character.getActiveFormData().getRgbBodyColor2();
					}
					if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor2().isEmpty()) {
						tailColor = character.getActiveStackFormData().getRgbBodyColor2();
					}

					if (stats.getStatus().isActionCharging()) {
						if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
							var nextForm = TransformationsHelper.getNextAvailableForm(stats);
							if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
								float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
								tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
							}
						} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
							var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
							if (nextForm != null && !nextForm.getBodyColor2().isEmpty()) {
								float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
								tailColor = DMZSkinLayer.lerpColor(factor, tailColor, nextForm.getRgbBodyColor2());
							}
						}
					}

					float[] tintedColor = applyAuraTint(tailColor[0], tailColor[1], tailColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
					float[] shadowColor = ColorUtils.skinShadowTone(tailColor);
					shadowColor = applyAuraTint(shadowColor[0], shadowColor[1], shadowColor[2], formTintColor, formTintIntensity, topAuraColor, tintProgress);
					renderTargetedBone(targetBone, poseStack, collector, animatable, tailRenderType, tintedColor[0], tintedColor[1], tintedColor[2], alpha, partialTick, packedLight);
					renderTargetedBone(targetBone, poseStack, collector, animatable, RenderTypes.entityTranslucent(RACES_PARTS_SHADOW_TEXTURE), shadowColor[0], shadowColor[1], shadowColor[2], alpha, partialTick, packedLight);
				});
			}
		}
	}

	private void renderTargetedBone(GeoBone targetBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, T animatable, RenderType renderType, float r, float g, float b, float alpha, float partialTick, int packedLight) {
		if (renderType == null) return;
		final int packedColor = com.dragonminez.client.render.util.RenderBufferUtil.packColor(r, g, b, alpha);
		com.dragonminez.client.render.util.GeoAttachmentRenderer.submit(sourcePass, poseStack, collector,
				targetBone, renderType, packedLight, OverlayTexture.NO_OVERLAY, packedColor);
	}

	private float[] resolveBodyColor1(StatsData stats) {
		var character = stats.getCharacter();
		float[] color = character.getRgbBodyColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor1().isEmpty()) {
			color = character.getActiveFormData().getRgbBodyColor1();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor1().isEmpty()) {
			color = character.getActiveStackFormData().getRgbBodyColor1();
		}

		if (stats.getStatus().isActionCharging()) {
			float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getBodyColor1().isEmpty()) {
					color = DMZSkinLayer.lerpColor(factor, color, nextForm.getRgbBodyColor1());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getBodyColor1().isEmpty()) {
					color = DMZSkinLayer.lerpColor(factor, color, nextForm.getRgbBodyColor1());
				}
			}
		}

		return color;
	}

	private float[] getTopAuraColor(StatsData stats) {
		var character = stats.getCharacter();
		float[] color = character.getRgbAuraColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getAuraColor() != null && !character.getActiveFormData().getAuraColor().isEmpty()) {
			color = character.getActiveFormData().getRgbAuraColor();
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getAuraColor() != null && !character.getActiveStackFormData().getAuraColor().isEmpty()) {
			color = character.getActiveStackFormData().getRgbAuraColor();
		}

		return color;
	}

	private float[] applyAuraTint(float r, float g, float b, float[] formTintColor, float formTintIntensity, float[] auraColor, float tintProgress) {
		boolean hasFormTint = formTintIntensity > 0.0f && formTintColor != null;
		float intensity = hasFormTint ? Mth.clamp(formTintIntensity, 0.0f, 1.0f) : 0.4f * tintProgress;
		intensity *= AuraTintTracker.darkTintScale(r, g, b);

		if (intensity <= 0.001f) return new float[]{r, g, b};

		float[] target = hasFormTint ? formTintColor : auraColor;
		float newR = r * (1.0f - intensity) + (target[0] * intensity);
		float newG = g * (1.0f - intensity) + (target[1] * intensity);
		float newB = b * (1.0f - intensity) + (target[2] * intensity);

		return new float[]{newR, newG, newB};
	}

	private void syncModelToPlayer(BakedGeoModel partsModel, BakedGeoModel playerModel) {
		for (GeoBone partBone : partsModel.topLevelBones()) {
			syncBoneRecursively(partBone, playerModel);
		}
	}

	private void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.name()).ifPresent(sourceBone -> copyBoneData(sourceBone, destBone));
		for (GeoBone child : destBone.children()) {
			syncBoneRecursively(child, sourceModel);
		}
	}

	private void syncTargetBoneAndParents(GeoBone destBone, BakedGeoModel sourceModel) {
		GeoBone currentDest = destBone;
		while (currentDest != null) {
			final GeoBone finalDest = currentDest;
			sourceModel.getBone(finalDest.name()).ifPresent(sourceBone -> copyBoneData(sourceBone, finalDest));
			currentDest = currentDest.parent();
		}
	}

	/**
	 * Copies an animated pose from source onto dest.
	 *
	 * <h2>GeckoLib 5 migration</h2>
	 * {@link GeoBone} is immutable now: setRotX/getRotX/setPosX/setScaleX are gone and only
	 * pivotX/Y/Z() and baseRotX/Y/Z() (static geometry) remain. Every per-frame value lives on
	 * {@link com.geckolib.animation.state.BoneSnapshot} (bone.frameSnapshot), so the pose is copied
	 * between snapshots rather than between bones.
	 */
	private void copyBoneData(GeoBone source, GeoBone dest) {
		com.geckolib.animation.state.BoneSnapshot src = source.frameSnapshot;
		com.geckolib.animation.state.BoneSnapshot dst = dest.frameSnapshot;
		if (src == null || dst == null) return;

		dst.setRotation(src.getRotX(), src.getRotY(), src.getRotZ());
		dst.setTranslation(src.getTranslateX(), src.getTranslateY(), src.getTranslateZ());
		dst.setScale(src.getScaleX(), src.getScaleY(), src.getScaleZ());
	}

	private void renderAccessories(PoseStack poseStack, T animatable, BakedGeoModel playerModel, net.minecraft.client.renderer.SubmitNodeCollector collector, float partialTick, int packedLight) {
		ItemStack headTechStack = getRenderableCurio(animatable, "head_tech", 0);

		String headTechId = headTechStack.getItem().getDescriptionId();
		boolean hasPothalaPair = headTechId.contains("pothala_pair");
		boolean hasPothalaRight = hasPothalaPair || headTechId.contains("pothala_right");
		boolean hasPothalaLeft = hasPothalaPair || headTechId.contains("pothala_left");

		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		boolean isFused = stats.getStatus().isFused() && stats.getStatus().getFusionType().equalsIgnoreCase("POTHALA");

		if (!isFused && !hasPothalaRight && !hasPothalaLeft) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(ACCESORIES_MODEL);
		if (accModel == null) return;

		String pothalaColor = stats.getStatus().getPothalaColor().contains("green") ? "green" : "yellow";
		RenderType accRenderType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + pothalaColor + "pothala.png"));

		if (hasPothalaRight || isFused) {
			accModel.getBone("pothala_right").ifPresent(bone -> {
				syncTargetBoneAndParents(bone, playerModel);
				renderTargetedBone(bone, poseStack, collector, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
			});
		}

		if (hasPothalaLeft || isFused) {
			accModel.getBone("pothala_left").ifPresent(bone -> {
				syncTargetBoneAndParents(bone, playerModel);
				renderTargetedBone(bone, poseStack, collector, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
			});
		}
	}

	private void renderScouter(PoseStack poseStack, T animatable, BakedGeoModel playerModel, net.minecraft.client.renderer.SubmitNodeCollector collector, float partialTick, int packedLight) {
		ItemStack headTechStack = getRenderableCurio(animatable, "head_tech", 0);
		if (headTechStack.isEmpty()) return;

		Item item = headTechStack.getItem();
		String color = null;

		if (item == MainItems.GREEN_SCOUTER.get()) color = "green";
		else if (item == MainItems.RED_SCOUTER.get()) color = "red";
		else if (item == MainItems.BLUE_SCOUTER.get()) color = "blue";
		else if (item == MainItems.PURPLE_SCOUTER.get()) color = "purple";

		if (color == null) return;

		BakedGeoModel accModel = getGeoModel().getBakedModel(SCOUTER_MODEL);
		if (accModel == null) return;

		RenderType accRenderType = ModRenderTypes.scouterLens(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + color + "_scouter.png"));

		accModel.getBone("radar").ifPresent(bone -> {
			syncTargetBoneAndParents(bone, playerModel);
			renderTargetedBone(bone, poseStack, collector, animatable, accRenderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight);
		});
	}

	private void renderWeightedItems(PoseStack poseStack, T animatable, net.minecraft.client.renderer.SubmitNodeCollector collector, String anchor, float partialTick, int packedLight, float alpha) {
		ItemStack weightStack = getRenderableCurio(animatable, "weights", 0);
		if (weightStack.isEmpty() || !(weightStack.getItem() instanceof WeightItem weightItem)) return;

		BakedGeoModel weightModel = getGeoModel().getBakedModel(WEIGHTED_ITEMS_MODEL);
		if (weightModel == null) return;

		RenderType type = RenderTypes.entityCutout(WEIGHTED_ITEMS_TEXTURE);

		switch (weightItem.getWeightType()) {
			case TURTLE_SHELL -> {
				if ("body".equals(anchor)) {
					renderWeightBone(weightModel, "turtleweight", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
				}
			}
			case WORKOUT_WEIGHTS -> {
				switch (anchor) {
					case "right_arm" -> renderWeightBone(weightModel, "right_arm_glove", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
					case "left_arm" -> renderWeightBone(weightModel, "left_arm_glove", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
					case "right_leg" -> renderWeightBone(weightModel, "right_leg_glove", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
					case "left_leg" -> renderWeightBone(weightModel, "left_leg_glove", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
				}
			}
			case PICCOLO_CAPE -> {
				switch (anchor) {
					case "body" -> {
						GeoBone capeBone = weightModel.getBone("cape").orElse(null);
						boolean wasHidden = capeBone != null && BoneVisibilityHandler.isBoneHidden(capeBone);
						if (capeBone != null) BoneVisibilityHandler.setBoneHidden(capeBone, true);

						renderWeightBone(weightModel, "piccoloweight_middle", poseStack, animatable, collector, type, partialTick, packedLight, alpha);

						if (capeBone != null) BoneVisibilityHandler.setBoneHidden(capeBone, wasHidden);
					}
					case "right_arm" -> renderWeightBone(weightModel, "piccoloweight_right", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
					case "left_arm" -> renderWeightBone(weightModel, "piccoloweight_left", poseStack, animatable, collector, type, partialTick, packedLight, alpha);
				}
			}
		}
	}

	private void renderWeightBone(BakedGeoModel weightModel, String boneName, PoseStack poseStack, T animatable, net.minecraft.client.renderer.SubmitNodeCollector collector, RenderType type, float partialTick, int packedLight, float alpha) {
		weightModel.getBone(boneName).ifPresent(bone -> renderTargetedBone(bone, poseStack, collector, animatable, type, 1.0f, 1.0f, 1.0f, alpha, partialTick, packedLight));
	}

	private void renderSword(PoseStack poseStack, T animatable, GeoBone playerBodyBone, net.minecraft.client.renderer.SubmitNodeCollector collector, float partialTick, int packedLight) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
		var stats = statsCap.orElse(new StatsData(animatable));

		if (!stats.getStatus().isHasCreatedCharacter()) return;
		if (stats.getCharacter().getActiveFormData() != null && stats.getCharacter().getActiveForm().contains("ozaru")) return;

		if (stats.getStatus().isRenderKatana()) {
			BakedGeoModel yajirobeModel = getGeoModel().getBakedModel(YAJIROBE_SWORD_MODEL);
			if (yajirobeModel != null) {
				RenderType type = RenderTypes.entityCutout(YAJIROBE_SWORD_TEXTURE);
				renderWeaponFromBodyAnchor(yajirobeModel, "katana", playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, 1.0f);
			}
		}

		String backWeapon = stats.getStatus().getBackWeapon();

		if (backWeapon == null || backWeapon.isEmpty()) return;

		// backWeapon only names the weapon the player owns. Whether it is in hand right now
		// is read straight off the entity, which costs no server round trip, so the sheath
		// never blinks out while a stale value catches up. Held items are synced for every
		// player, so this is correct for other players too.
		if (backWeapon.equals(MainItems.POWER_POLE.get().getDescriptionId())) {
			renderPowerPoleOnBack(poseStack, animatable, playerBodyBone, collector, partialTick, packedLight, isHolding(animatable, MainItems.POWER_POLE.get()));
		} else if (backWeapon.equals(MainItems.Z_SWORD.get().getDescriptionId())) {
			// The Z Sword model carries no sheath bone, so it just leaves the back when drawn.
			if (!isHolding(animatable, MainItems.Z_SWORD.get())) {
				BakedGeoModel zModel = getGeoModel().getBakedModel(Z_SWORD_MODEL);
				if (zModel != null) {
					RenderType type = RenderTypes.entityCutout(Z_SWORD_TEXTURE);
					renderWeaponFromBodyAnchor(zModel, "espada", playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, 1.0f);
				}
			}
		} else if (backWeapon.equals(MainItems.BRAVE_SWORD.get().getDescriptionId())) {
			renderBraveSwordOnBack(poseStack, animatable, playerBodyBone, collector, partialTick, packedLight, isHolding(animatable, MainItems.BRAVE_SWORD.get()));
		}
	}

	private static boolean isHolding(AbstractClientPlayer player, Item item) {
		return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
	}

	/**
	 * Renders the Brave Sword on the player's back. With {@code scabbardOnly} the sword
	 * bone is hidden so only the scabbard draws, which is what stays behind while the
	 * sword itself is in hand.
	 */
	private void renderBraveSwordOnBack(PoseStack poseStack, T animatable, GeoBone playerBodyBone, net.minecraft.client.renderer.SubmitNodeCollector collector, float partialTick, int packedLight, boolean scabbardOnly) {
		BakedGeoModel braveModel = getGeoModel().getBakedModel(BRAVE_SWORD_MODEL);
		if (braveModel == null) return;

		RenderType type = RenderTypes.entityCutout(BRAVE_SWORD_TEXTURE);
		PoseStack local = new PoseStack();
		local.translate(BRAVE_BACK_X, BRAVE_BACK_Y, BRAVE_BACK_Z);
		if (BRAVE_BACK_ROT_X != 0.0F) local.mulPose(Axis.XP.rotationDegrees(BRAVE_BACK_ROT_X));
		if (BRAVE_BACK_ROT_Y != 0.0F) local.mulPose(Axis.YP.rotationDegrees(BRAVE_BACK_ROT_Y));
		if (BRAVE_BACK_ROT_Z != 0.0F) local.mulPose(Axis.ZP.rotationDegrees(BRAVE_BACK_ROT_Z));
		renderWeaponFromBodyAnchor(braveModel, "espadatrunks", playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, BRAVE_BACK_SCALE,
                scabbardOnly ? java.util.Set.of("espada") : java.util.Set.of(), local.last().pose());
	}

	/**
	 * Renders the Power Pole on the player's back. With {@code holsterOnly} the pole bone
	 * is hidden so only the holster draws.
	 */
	private void renderPowerPoleOnBack(PoseStack poseStack, T animatable, GeoBone playerBodyBone, net.minecraft.client.renderer.SubmitNodeCollector collector, float partialTick, int packedLight, boolean holsterOnly) {
		BakedGeoModel powerpole = getGeoModel().getBakedModel(POWER_POLE_MODEL);
		if (powerpole == null) return;

		RenderType type = RenderTypes.entityCutout(POWER_POLE_TEXTURE);
		renderWeaponFromBodyAnchor(powerpole, "baculo", playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, 1.0f,
                holsterOnly ? java.util.Set.of("palo") : java.util.Set.of());
	}

	private void renderWeaponFromBodyAnchor(BakedGeoModel weaponModel, String anchorBoneName, GeoBone playerBodyBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, T animatable, RenderType type, float partialTick, int packedLight, float scale) {
        renderWeaponFromBodyAnchor(weaponModel, anchorBoneName, playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, scale, java.util.Set.of());
    }

    private void renderWeaponFromBodyAnchor(BakedGeoModel weaponModel, String anchorBoneName, GeoBone playerBodyBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, T animatable, RenderType type, float partialTick, int packedLight, float scale, java.util.Set<String> hidden) {
        renderWeaponFromBodyAnchor(weaponModel, anchorBoneName, playerBodyBone, poseStack, collector, animatable, type, partialTick, packedLight, scale, hidden, new org.joml.Matrix4f());
    }

    private void renderWeaponFromBodyAnchor(BakedGeoModel weaponModel, String anchorBoneName, GeoBone playerBodyBone, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, T animatable, RenderType type, float partialTick, int packedLight, float scale, java.util.Set<String> hidden, org.joml.Matrix4fc localTransform) {
		GeoBone weaponAnchor = weaponModel.getBone(anchorBoneName).orElse(null);
		if (weaponAnchor == null) return;

        com.dragonminez.client.render.util.GeoAttachmentRenderer.submitMounted(sourcePass, poseStack, collector,
                weaponAnchor, playerBodyBone, type, packedLight, OverlayTexture.NO_OVERLAY, -1, scale, localTransform, hidden);
	}

	private ItemStack getRenderableCurio(T animatable, String slotId, int index) {
		var inventory = CuriosApi.getCuriosInventory(animatable).orElse(null);
		if (inventory == null) return ItemStack.EMPTY;

		var stacksHandler = inventory.getCurios().get(slotId);
		if (stacksHandler == null) return ItemStack.EMPTY;

		if (!stacksHandler.getRenders().get(index)) return ItemStack.EMPTY;

		return stacksHandler.getStacks().getStackInSlot(index);
	}
}
