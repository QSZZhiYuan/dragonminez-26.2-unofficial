package com.dragonminez.client.render.layer;

import com.dragonminez.client.util.BoneVisibilityHandler;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.dragonminez.client.render.util.RenderBufferUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZSkinLayer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerGeoLayer<T> {
	public static boolean PREVIEW_MODE = false;

	private static final Map<Identifier, Identifier> VALIDATED_TEXTURES_CACHE = new ConcurrentHashMap<>();
	private static final Identifier BLANK_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");

	private static final float[] DARK_GRAY = ColorUtils.hexToRgb("#383838");

	private static final String[] ARMOR_BONES = {
			"armorHead", "armorBody", "armorBody2", "armorLeggingsBody",
			"armorRightArm", "armorLeftArm", "armorLeftLeg", "armorLeftBoot",
			"armorRightLeg", "armorRightBoot"
	};

	private float[] currentFormTintColor = null;
	private float currentFormTintIntensity = 0.0f;
	private float currentTintProgress = 0.0f;
	private float[] currentAuraColor = new float[]{1.0f, 1.0f, 1.0f};

	private static final String SSJ4_FUR_LAYER = "ssj4fur";

	private static final int WOUND_OPACITY_PASSES = 4;

	private static final float SPECTATOR_ALPHA = 0.35f;
	private static final float SPECTATOR_HEAD_ALPHA = 0.15f;

	private float currentSsj4Alpha = 0.0f;
	private float[] currentSsj4Color = null;

	public DMZSkinLayer(GeoRenderer<T, ?, ?> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	protected boolean skipInHudPortrait() {
		return false;
	}

	@Override
	protected void render(DMZLayerContext ctx) {
		SkinPass pass = createSkinPass(ctx, false, 0.0f);
		PoseStack poseStack = ctx.poseStack();
		BakedGeoModel model = ctx.model();
		net.minecraft.client.renderer.SubmitNodeCollector collector = ctx.collector();
		float partialTick = ctx.partialTick();
		int packedLight = ctx.packedLight();
		int packedOverlay = ctx.packedOverlay();

		var player = ctx.player();
		if (player == null) return;

		if (player.hasEffect(MainEffects.CANDY)) return;

		var stats = ctx.stats();
		if (stats == null) return;

		int playerId = player.getId();
		long gameTime = player.level().getGameTime();
		boolean shouldFadeIn = stats.getStatus().isChargingKi()
				|| stats.getStatus().isAuraActive()
				|| stats.getStatus().isPermanentAura();
		float tintProgress = AuraTintTracker.update(playerId, gameTime, shouldFadeIn);

		this.currentTintProgress = tintProgress;
		this.currentAuraColor = getTopAuraColor(stats);
		FormConfig.FormData tintForm = resolveTintForm(stats);
		this.currentFormTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
		this.currentFormTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;

		Ssj4Overlay ssj4 = resolveSsj4Overlay(stats);
		float[] ssj4Color = ssj4 != null ? ssj4.color() : null;
		float ssj4Target = ssj4 != null ? ssj4.target() : 0.0f;

		float alpha = player.isSpectator() ? SPECTATOR_ALPHA : 1.0f;
		float headAlpha = player.isSpectator() ? SPECTATOR_HEAD_ALPHA : 1.0f;
		

		List<BodyLayerFadeTracker.FadingLayer> fadingLayers = new ArrayList<>();
		SkinGathererProvider.BodyLayerSink geoConsumer = new SkinGathererProvider.BodyLayerSink() {
			@Override
			public void base(Identifier texture, float[] color) {
				RenderType baseType = alpha < 1.0f ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture);
				renderLayerWholeModel(model, poseStack, collector, pass, baseType, color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
			}

            @Override
            public void overlay(Identifier texture, float[] color) {
                renderLayerWholeModel(model, poseStack, collector, pass, ModRenderTypes.skinOverlayCutout(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
            }

            @Override
            public void translucent(Identifier texture, float[] color) {
                renderLayerWholeModel(model, poseStack, collector, pass, RenderTypes.entityTranslucent(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
            }

            @Override
            public void overlayTranslucent(Identifier texture, float[] color) {
                renderLayerWholeModel(model, poseStack, collector, pass, ModRenderTypes.skinOverlayTranslucent(texture), color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
            }

			@Override
			public void fading(String layerId, Identifier texture, float[] color, float targetAlpha) {
				fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(layerId, texture, color, targetAlpha));
			}
		};

		SkinGathererProvider.BodyLayerSink overlayConsumer = new SkinGathererProvider.BodyLayerSink() {
			@Override
			public void base(Identifier texture, float[] color) {
				RenderType overlayType = alpha < 1.0f ? ModRenderTypes.skinOverlayTranslucent(texture) : ModRenderTypes.skinOverlayCutout(texture);
				renderLayerWholeModel(model, poseStack, collector, pass, overlayType, color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, true);
			}

			@Override
			public void fading(String layerId, Identifier texture, float[] color, float targetAlpha) {
				fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(layerId, texture, color, targetAlpha));
			}
		};

		SkinGathererProvider.INSTANCE.gatherBodyLayers(player, stats, partialTick, geoConsumer);
		com.dragonminez.client.render.util.SaiyanTailFurRenderer.submit(
				ctx.info(), collector, poseStack, player, stats, packedLight, alpha);
		if (ssj4 != null) {
			Identifier furTex = getSafeTexture(SkinGathererProvider.getCachedTexture("textures/entity/races/humansaiyan/" + ssj4.key() + "_layer1.png"));
			fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(SSJ4_FUR_LAYER, furTex, ssj4.color(), ssj4.target()));
		}
		SkinGathererProvider.INSTANCE.gatherAndroidLayers(player, stats, partialTick, geoConsumer);
		renderFadingBodyLayers(model, poseStack, pass, collector, playerId, gameTime, fadingLayers, partialTick, packedLight, packedOverlay, alpha);

		this.currentSsj4Alpha = PREVIEW_MODE ? ssj4Target : BodyLayerFadeTracker.getProgress(playerId, SSJ4_FUR_LAYER);
		this.currentSsj4Color = ssj4Color != null ? ssj4Color : BodyLayerFadeTracker.getColor(playerId, SSJ4_FUR_LAYER);

		renderHair(poseStack, pass, model, collector, player, stats, partialTick, packedLight, packedOverlay, alpha, ctx);
		SkinGathererProvider.INSTANCE.gatherTattooLayers(player, stats, partialTick, overlayConsumer);
		SkinGathererProvider.INSTANCE.gatherEffectLayers(player, stats, partialTick, overlayConsumer);
		renderWounds(model, poseStack, pass, collector, player, partialTick, packedLight, packedOverlay, alpha);
		renderFace(poseStack, pass, model, collector, player, stats, partialTick, packedLight, packedOverlay, headAlpha, ctx);

		// 26.2: there is no caller-owned buffer to restore — everything above was queued on the
		// SubmitNodeCollector, so nothing to rebind here.
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

	private void renderHair(PoseStack poseStack, SkinPass pass, BakedGeoModel model, net.minecraft.client.renderer.SubmitNodeCollector collector, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha, DMZLayerContext ctx) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();
		int hairId = character.getHairId();

		if (!HairManager.canUseHair(character)) return;
		if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) return;
		if (hairId == 5) return;
		if (hairId == 0 && character.getHairBase().getVisibleStrandCount() == 0) return;
		List<String> hairTypes = List.of("base", "ssj", "ssj2", "ssj3");

		float[] currentTint = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			if (character.getActiveFormData().hasDefinedHairType() && !hairTypes.contains(character.getActiveFormData().getHairType().toLowerCase())) return;
			if (!character.getActiveFormData().getHairColor().isEmpty()) {
				currentTint = character.getActiveFormData().getRgbHairColor();
			}
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			if (character.getActiveStackFormData().hasDefinedHairType() && !hairTypes.contains(character.getActiveStackFormData().getHairType().toLowerCase())) return;
			if (!character.getActiveStackFormData().getHairColor().isEmpty()) {
				currentTint = character.getActiveStackFormData().getRgbHairColor();
			}
		}

		float[] finalTint = currentTint;
		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
					float chargeProgress = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					finalTint = lerpColor(chargeProgress, currentTint, nextForm.getRgbHairColor());
				}
			}
		}

		float[] publishedHair = DMZHairLayer.getPublishedHairBaseColor(player.getId(), player.level().getGameTime());
		final float[] hairTint = publishedHair != null ? publishedHair : applyColorTint(finalTint, stats);

		if (model.getBone("head").isPresent() && character.isRenderHairBase()) {
			SkinPass hairPass = createSkinPass(ctx, true, 0.006f);
			renderColoredLayer(model, poseStack, hairPass, collector, "textures/entity/races/hair_base.png", hairTint, partialTick, packedLight, packedOverlay, alpha);
		}
	}

	private void renderFace(PoseStack poseStack, SkinPass pass, BakedGeoModel model, net.minecraft.client.renderer.SubmitNodeCollector collector, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha, DMZLayerContext ctx) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();

		String customModelValue = "";
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().hasCustomModel()) {
			customModelValue = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel()) {
			customModelValue = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		if (customModelValue.isEmpty()) {
			var raceConfig = ConfigManager.getRaceCharacter(raceName);
			if (raceConfig != null && raceConfig.getCustomModel() != null && !raceConfig.getCustomModel().isEmpty()) {
				customModelValue = raceConfig.getCustomModel().toLowerCase();
			}
		}

		final boolean isModelEmpty = customModelValue.isEmpty();
		final String finalFaceKey = isModelEmpty
				? (SkinGathererProvider.isBuiltInRace(raceName) ? raceName : "human")
				: customModelValue;

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));
		if (isOozaruForm || finalFaceKey.equals("oozaru")) return;

		boolean isHumanoidModel = SkinGathererProvider.isHumanoidKey(finalFaceKey);
		if (isHumanoidModel && bodyType == 0) return;

		var raceConfig = ConfigManager.getRaceCharacter(raceName);
		if (raceConfig != null && Boolean.TRUE.equals(raceConfig.getUseVanillaSkin()) && bodyType == 0) return;

		if (model.getBone("head").isPresent()) {
			SkinPass facePass = createSkinPass(ctx, true, 0.002f);
			dispatchFaceRender(model, poseStack, facePass, collector, stats, character, finalFaceKey, isModelEmpty, raceName, partialTick, packedLight, packedOverlay, alpha);
		}
	}

	private void dispatchFaceRender(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, StatsData stats, Character character, String faceKey, boolean isModelEmpty, String race, float pt, int pl, int po, float alpha) {

		float[] eye1 = character.getRgbEye1Color();
		float[] eye2 = character.getRgbEye2Color();
		float[] skin = character.getRgbBodyColor();
		float[] b2 = character.getRgbBodyColor2();
		float[] hair = character.getRgbHairColor();

		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			var f = character.getActiveFormData();
			if (!f.getEye1Color().isEmpty()) eye1 = f.getRgbEye1Color();
			if (!f.getEye2Color().isEmpty()) eye2 = f.getRgbEye2Color();
			if (!f.getHairColor().isEmpty()) hair = f.getRgbHairColor();
			if (!f.getBodyColor1().isEmpty()) skin = f.getRgbBodyColor1();
			if (!f.getBodyColor2().isEmpty()) b2 = f.getRgbBodyColor2();
		}

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			var sf = character.getActiveStackFormData();
			if (!sf.getEye1Color().isEmpty()) eye1 = sf.getRgbEye1Color();
			if (!sf.getEye2Color().isEmpty()) eye2 = sf.getRgbEye2Color();
			if (!sf.getHairColor().isEmpty()) hair = sf.getRgbHairColor();
			if (!sf.getBodyColor1().isEmpty()) skin = sf.getRgbBodyColor1();
			if (!sf.getBodyColor2().isEmpty()) b2 = sf.getRgbBodyColor2();
		}

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, nextForm.getRgbEye1Color());
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, nextForm.getRgbEye2Color());
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, nextForm.getRgbBodyColor1());
					if (!nextForm.getBodyColor2().isEmpty())
						b2 = lerpColor(factor, b2, nextForm.getRgbBodyColor2());
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, nextForm.getRgbHairColor());
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getEye1Color().isEmpty())
						eye1 = lerpColor(factor, eye1, nextForm.getRgbEye1Color());
					if (!nextForm.getEye2Color().isEmpty())
						eye2 = lerpColor(factor, eye2, nextForm.getRgbEye2Color());
					if (!nextForm.getBodyColor1().isEmpty())
						skin = lerpColor(factor, skin, nextForm.getRgbBodyColor1());
					if (!nextForm.getBodyColor2().isEmpty())
						b2 = lerpColor(factor, b2, nextForm.getRgbBodyColor2());
					if (!nextForm.getHairColor().isEmpty())
						hair = lerpColor(factor, hair, nextForm.getRgbHairColor());
				}
			}
		}

		skin = applyColorTint(skin, stats);
		hair = applyColorTint(hair, stats);
		b2 = applyColorTint(b2, stats);

		String family = SkinGathererProvider.modelFamily(faceKey);
        if (faceKey.equals("glindtrueform") || faceKey.equals("trascended")) family = "human";

		if (family.equals("custom")) {
			var rConfig = ConfigManager.getRaceCharacter(race);
			if (rConfig != null && Boolean.TRUE.equals(rConfig.getIsLayered())) {
				renderCustomFace(model, poseStack, pass, collector, character, faceKey, race, eye1, eye2, skin, hair, pt, pl, po, alpha);
			}
			return;
		}

		switch (family) {
			case "human" ->
					renderHumanFace(model, poseStack, pass, collector, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
			case "namekian" ->
					renderNamekianFace(model, poseStack, pass, collector, character, eye1, eye2, skin, pt, pl, po, alpha);
			case "frostdemon" ->
					renderFrostFace(model, poseStack, pass, collector, character, faceKey, isModelEmpty, family, eye1, eye2, skin, b2, pt, pl, po, alpha);
			case "bioandroid" ->
					renderBioFace(model, poseStack, pass, collector, character, faceKey, isModelEmpty, family, eye1, eye2, pt, pl, po, alpha);
			case "majin" ->
					renderMajinFace(model, poseStack, pass, collector, character, faceKey, eye1, eye2, skin, b2, pt, pl, po, alpha);
		}
	}

	private void renderCustomFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, String faceKey, String race, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/" + race + "/faces/";
		String prefix = faceKey + "_";
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_2.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_" + character.getEyesType() + "_3.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "eye_0_3.png")).getPath(), hair, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "nose_" + character.getNoseType() + ".png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "nose_0.png")).getPath(), skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "mouth_" + character.getMouthType() + ".png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + prefix + "mouth_0.png")).getPath(), skin, pt, pl, po, alpha);
	}

	private void renderHumanFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/humansaiyan/faces/";
		String eyeBase = "humansaiyan_eye_" + character.getEyesType();
        var legendaryGroup = character.getActiveFormGroup().equals("legendaryforms");
		float[] white = {1.0f, 1.0f, 1.0f};

		boolean isMajin = pass.player().hasEffect(MainEffects.MAJIN);

		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "humansaiyan_eye_0_3.png")).getPath(), hair, pt, pl, po, alpha);

		boolean isSsj3 = (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().getHairType().equalsIgnoreCase("ssj3")) ||
				(character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().getHairType().equalsIgnoreCase("ssj3"));
		if (isSsj3) renderColoredLayer(model, poseStack, pass, collector, folder + "ssj3eyebrows_eye_" + character.getEyesType() + ".png", skin, pt, pl, po, alpha);

		String ssj4Eyes = folder + "ssj4_eyes_" + character.getEyesType() + ".png";
		if (isMajin) {
			renderColoredLayer(model, poseStack, pass, collector, ssj4Eyes, ColorUtils.hexToRgb("#292929"), pt, pl, po, alpha);
		} else if (this.currentSsj4Alpha > 0.001f && this.currentSsj4Color != null) {
			renderFadingColoredLayer(model, poseStack, pass, collector, ssj4Eyes, this.currentSsj4Color, pt, pl, po, alpha * this.currentSsj4Alpha);
		}

        if(legendaryGroup && (character.getActiveForm().equals("shiyoken") || character.getActiveForm().equals("shin_shiyoken") || character.getActiveForm().equals("chou_shiyoken"))){

            renderColoredLayer(model, poseStack, pass, collector, folder + "shiyoken_eye0.png", ColorUtils.hexToRgb("#FFFFFF"), pt, pl, po, alpha, true);
            renderColoredLayer(model, poseStack, pass, collector, folder + "shiyoken_eye1.png", eye1, pt, pl, po, alpha, true);

        }

        renderColoredLayer(model, poseStack, pass, collector, folder + "humansaiyan_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha, false);
        renderColoredLayer(model, poseStack, pass, collector, folder + "humansaiyan_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha, false);
	}

	private void renderNamekianFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, float[] eye1, float[] eye2, float[] skin, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/namekian/faces/";
		String eyeBase = "namekian_eye_" + character.getEyesType();
        var bodytype = character.getBodyType();
        var hairColor = character.getRgbHairColor();
		float[] white = {1.0f, 1.0f, 1.0f};

		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_0.png")).getPath(), white, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_2.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + eyeBase + "_3.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "namekian_eye_0_3.png")).getPath(), skin, pt, pl, po, alpha);

        if(bodytype == 1 || bodytype == 2) skin = hairColor;
        renderColoredLayer(model, poseStack, pass, collector, folder + "namekian_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, pass, collector, folder + "namekian_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderFrostFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float[] skin, float[] b2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/frostdemon/faces/";
		int bodyType = character.getBodyType();
		String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";
		boolean isFifth = faceKey.equals("frostdemon_fifth") || currentForm.contains(FrostDemonForms.FIFTH_FORM);
        boolean isMetalCore = faceKey.equals("frostdemon_metalcore");

        float[] eyeBgColor = isFifth ? ColorUtils.hexToRgb("#D11A11") :
                (isMetalCore ? ColorUtils.hexToRgb("#242424") : ColorUtils.hexToRgb("#F2F2F2"));
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_" + character.getEyesType() + "_0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0_0.png")).getPath(), eyeBgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_" + character.getEyesType() + "_1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0_1.png")).getPath(), eye1, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_" + character.getEyesType() + "_2.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "frostdemon_eye_0_2.png")).getPath(), eye2, pt, pl, po, alpha);

		if (isFifth) {
			renderColoredLayer(model, poseStack, pass, collector, folder + "frostdemon_fifth_mouth.png", skin, pt, pl, po, alpha, true);
			return;
		}

		boolean isPrimitiveForm = !character.hasActiveForm() || currentForm.equals("second") || currentForm.equals("third");
		float[] finalDetailColor = (isPrimitiveForm && (faceKey.equals("frostdemon") || faceKey.equals("frostdemon_third") || faceKey.equals("frostdemon_second"))) ? b2 : (bodyType == 1 ? b2 : skin);
        renderColoredLayer(model, poseStack, pass, collector, folder + "frostdemon_nose_" + character.getNoseType() + ".png", finalDetailColor, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, pass, collector, folder + "frostdemon_mouth_" + character.getMouthType() + ".png", finalDetailColor, pt, pl, po, alpha);
	}

	private void renderBioFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/bioandroid/faces/";
		String phase;
		String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";

		if (faceKey.equals("bioandroid_semi")) phase = "semiperfect";
		else if (faceKey.equals("bioandroid_perfect")) phase = "perfect";
		else if (faceKey.equals("bioandroid_base")) phase = "base";
		else if (faceKey.equals("bioandroid") && !isModelEmpty) phase = "base";
		else if (race.equals("bioandroid"))
			phase = (character.hasActiveForm() && currentForm.equals(BioAndroidForms.SEMI_PERFECT)) ? "semiperfect" : (character.hasActiveForm() ? "perfect" : "base");
		else phase = "base";

		String textureBase = folder + phase + "_eye_layer";

		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer0.png")).getPath(), eye2, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, textureBase + "1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "base_eye_layer1.png")).getPath(), eye1, pt, pl, po, alpha);
	}

	private void renderMajinFace(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, Character character, String faceKey, float[] eye1, float[] eye2, float[] skin, float[] b2, float pt, int pl, int po, float alpha) {
		String folder = "textures/entity/races/majin/faces/";

        if ("janemba_fat".equals(faceKey)) {
            renderColoredLayer(model, poseStack, pass, collector, folder + "janemba_eye_0.png", DARK_GRAY, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, pass, collector, folder + "janemba_eye_1.png", eye1, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, pass, collector, folder + "janemba_mouth.png", skin, pt, pl, po, alpha);
            return;
        }

		if ("janemba_super".equals(faceKey)) {
            renderColoredLayer(model, poseStack, pass, collector, folder + "majin_eye_2_0.png", eye1, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, pass, collector, folder + "majin_eye_2_1.png", eye2, pt, pl, po, alpha);
            renderColoredLayer(model, poseStack, pass, collector, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
            return;
        }

		int eyeType = character.getEyesType();
        var bodytype = character.getBodyType();
		float[] bgColor = eyeType == 0 ? skin : DARK_GRAY;
		float[] layer1Color = eyeType == 0 ? skin : eye1;
		String eyePath = folder + "majin_eye_" + eyeType + "_";

		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "0.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_0.png")).getPath(), bgColor, pt, pl, po, alpha);
		renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "1.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_1.png")).getPath(), layer1Color, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, pass, collector, getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, eyePath + "2.png"), Identifier.fromNamespaceAndPath(Reference.MOD_ID, folder + "majin_eye_0_2.png")).getPath(), skin, pt, pl, po, alpha);

        if(bodytype == 1) skin = b2;
        renderColoredLayer(model, poseStack, pass, collector, folder + "majin_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, pass, collector, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
	}

	private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, SkinPass pass, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay, float alpha, boolean applyTransformationTint) {
		if (applyTransformationTint) {
			float[] rgb = {r, g, b};
			tintInPlace(rgb);
			r = rgb[0];
			g = rgb[1];
			b = rgb[2];
		}

		RenderPassInfo<GeoRenderState> info = pass.info();
		submitSkinGeometry(info, collector, renderType, packedLight, packedOverlay,
				RenderBufferUtil.packColor(r, g, b, alpha), scaleInflation);
	}

	static void submitSkinGeometry(RenderPassInfo<GeoRenderState> info,
			net.minecraft.client.renderer.SubmitNodeCollector collector, RenderType renderType,
			int packedLight, int packedOverlay, int packedColor, float scaleInflation) {
		PoseStack layerPose = info.poseStack();
		layerPose.pushPose();
		try {
			if (scaleInflation > 1.0f) layerPose.scale(scaleInflation, scaleInflation, scaleInflation);
			collector.submitCustomGeometry(layerPose, renderType, (pose, vertices) -> {
				layerPose.pushPose();
				try {
					layerPose.last().set(pose);
					info.renderPosed(() -> info.model().render(info, vertices, packedLight, packedOverlay, packedColor));
				} finally {
					layerPose.popPose();
				}
			});
		} finally {
			layerPose.popPose();
		}
	}

	private record SkinPass(RenderPassInfo<GeoRenderState> info, AbstractClientPlayer player) {}

	/** Each overlay owns its bone updates; face/hair masks must never hide the body pass. */
	private SkinPass createSkinPass(DMZLayerContext ctx, boolean headOnly, float headInflation) {
		PoseStack layerPose = new PoseStack();
		layerPose.last().set(ctx.poseStack().last());
		RenderPassInfo<GeoRenderState> info = RenderPassInfo.create(
				getRenderer(), ctx.info().renderState(), layerPose, ctx.info().cameraState(), true);
		info.addBoneUpdater((passInfo, snapshots) -> {
			passInfo.model().boneLookup().get().keySet().forEach(snapshots::get);
			BoneVisibilityHandler.updateVisibility(passInfo.model(), ctx.player(), this);
			configureSkinBones(passInfo, snapshots, headOnly, headInflation);
		});
		return new SkinPass(info, ctx.player());
	}

	static void configureSkinBones(RenderPassInfo<GeoRenderState> info,
			com.geckolib.renderer.base.BoneSnapshots snapshots, boolean headOnly, float headInflation) {
		for (String armorBone : ARMOR_BONES) {
			snapshots.ifPresent(armorBone, snapshot -> snapshot.skipRender(true));
		}
		if (info.renderState() instanceof com.dragonminez.client.render.GeoEntityRenderState state && state.firstPersonBody) {
			snapshots.ifPresent("head", snapshot -> snapshot.skipRender(true).skipChildrenRender(true));
		}
		if (!headOnly) return;
		for (GeoBone bone : info.model().boneLookup().get().values()) {
			GeoBone ancestor = bone;
			while (ancestor != null && !"head".equals(ancestor.name())) ancestor = ancestor.parent();
			if (ancestor == null) snapshots.ifPresent(bone.name(), snapshot -> snapshot.skipRender(true));
		}
		snapshots.ifPresent("head", snapshot -> {
			snapshot.setTranslateZ(snapshot.getTranslateZ() - headInflation);
			snapshot.setScale(snapshot.getScaleX() + headInflation,
					snapshot.getScaleY() + headInflation, snapshot.getScaleZ() + headInflation);
		});
	}

	private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		renderColoredLayer(model, poseStack, pass, collector, path, rgb, partialTick, packedLight, packedOverlay, alpha, false);
	}

	private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha, boolean applyTransformationTint) {
		Identifier loc = getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = alpha < 1.0f ? ModRenderTypes.skinOverlayTranslucent(loc) : ModRenderTypes.skinOverlayCutout(loc);
		renderLayerWholeModel(model, poseStack, collector, pass, renderType, rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, applyTransformationTint);
	}

	public record Ssj4Overlay(String key, float[] color, float target) {}

	public static Ssj4Overlay resolveSsj4Overlay(StatsData stats) {
		var character = stats.getCharacter();

		String activeModel = "";
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && Boolean.TRUE.equals(character.getActiveStackFormData().hasCustomModel())) {
			activeModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && Boolean.TRUE.equals(character.getActiveFormData().hasCustomModel())) {
			activeModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}
		boolean activeSsj4 = isSsj4Model(activeModel);

		boolean chargingSsj4 = false;
		String targetModel = "";
		float chargeFraction = 0.0f;
		FormConfig.FormData nextForm = null;
		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) nextForm = TransformationsHelper.getNextAvailableForm(stats);
			else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) nextForm = TransformationsHelper.getNextAvailableStackForm(stats);

			if (nextForm != null && Boolean.TRUE.equals(nextForm.hasCustomModel())) {
				targetModel = nextForm.getCustomModel().toLowerCase();
				chargingSsj4 = isSsj4Model(targetModel);
				chargeFraction = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
			}
		}

		if (!activeSsj4 && !chargingSsj4) return null;

		String key = activeSsj4 ? activeModel.contains("ssj4gt") ? "ssj4gt" : "ssj4d" : targetModel.contains("ssj4gt") ? "ssj4gt" : "ssj4d";
		float target = activeSsj4 ? 1.0f : chargeFraction;
		float[] color = resolveSsj4OverlayColor(character, chargingSsj4 ? nextForm : null, chargeFraction);
		return new Ssj4Overlay(key, color, target);
	}

	private static boolean isSsj4Model(String model) {
		return model.contains("ssj4d") || model.contains("ssj4gt");
	}

	private static float[] resolveSsj4OverlayColor(Character character, FormConfig.FormData chargeTarget, float chargeFraction) {
		float[] b2 = character.getRgbBodyColor2();
		if (character.hasActiveForm() && character.getActiveFormData() != null && !character.getActiveFormData().getBodyColor2().isEmpty()) {
			b2 = character.getActiveFormData().getRgbBodyColor2();
		}
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && !character.getActiveStackFormData().getBodyColor2().isEmpty()) {
			b2 = character.getActiveStackFormData().getRgbBodyColor2();
		}
		if (chargeTarget != null && chargeTarget.getRgbBodyColor2() != null) {
			b2 = lerpColor(chargeFraction, b2, chargeTarget.getRgbBodyColor2());
		}
		return b2;
	}

	private void renderFadingBodyLayers(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, int entityId, long gameTime, List<BodyLayerFadeTracker.FadingLayer> activeLayers, float partialTick, int packedLight, int packedOverlay, float baseAlpha) {
		List<BodyLayerFadeTracker.RenderEntry> toRender;
		if (PREVIEW_MODE) {
			toRender = new ArrayList<>();
			for (BodyLayerFadeTracker.FadingLayer l : activeLayers) toRender.add(new BodyLayerFadeTracker.RenderEntry(l.texture(), l.color(), l.target()));
		} else {
			toRender = BodyLayerFadeTracker.update(entityId, gameTime, activeLayers);
		}

		for (BodyLayerFadeTracker.RenderEntry entry : toRender) {
			float a = baseAlpha * entry.alpha();
			if (a <= 0.001f) continue;
			float[] color = entry.color();
			RenderType renderType = a < 1.0f ? ModRenderTypes.skinOverlayTranslucent(entry.texture()) : ModRenderTypes.skinOverlayCutout(entry.texture());
			renderLayerWholeModel(model, poseStack, collector, pass, renderType, color[0], color[1], color[2], 1.0f, partialTick, packedLight, packedOverlay, a, true);
		}
	}

	private void renderWounds(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, AbstractClientPlayer player, float partialTick, int packedLight, int packedOverlay, float alpha) {
		float maxHealth = player.getMaxHealth();
		if (maxHealth <= 0.0f) return;
		float healthRatio = Mth.clamp(player.getHealth() / maxHealth, 0.0f, 1.0f);

		float woundsAlpha = Mth.clamp((0.75f - healthRatio) / (0.75f - 0.50f), 0.0f, 1.0f);
		float grievousAlpha = Mth.clamp((0.40f - healthRatio) / (0.40f - 0.20f), 0.0f, 1.0f);

		if (woundsAlpha > 0.001f) {
			renderWoundLayer(model, poseStack, pass, collector, "textures/entity/races/wounds.png", partialTick, packedLight, packedOverlay, alpha * woundsAlpha);
		}
		if (grievousAlpha > 0.001f) {
			renderWoundLayer(model, poseStack, pass, collector, "textures/entity/races/grievous_wounds.png", partialTick, packedLight, packedOverlay, alpha * grievousAlpha);
		}
	}

	private void renderWoundLayer(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, String path, float partialTick, int packedLight, int packedOverlay, float alpha) {
		Identifier loc = getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = ModRenderTypes.skinOverlayTranslucent(loc);
		for (int index = 0; index < WOUND_OPACITY_PASSES; index++) {
			renderLayerWholeModel(model, poseStack, collector, pass, renderType, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha, false);
		}
	}

	private void renderFadingColoredLayer(BakedGeoModel model, PoseStack poseStack, SkinPass pass, net.minecraft.client.renderer.SubmitNodeCollector collector, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
		Identifier loc = getSafeTexture(Identifier.fromNamespaceAndPath(Reference.MOD_ID, path));
		RenderType renderType = alpha < 1.0f ? ModRenderTypes.skinOverlayTranslucent(loc) : ModRenderTypes.skinOverlayCutout(loc);
		renderLayerWholeModel(model, poseStack, collector, pass, renderType, rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha, false);
	}

	public static FormConfig.FormData resolveTintForm(StatsData stats) {
		var character = stats.getCharacter();
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().hasTint()) {
			return character.getActiveStackFormData();
		}
		if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasTint()) {
			return character.getActiveFormData();
		}
		return null;
	}

	private float[] applyColorTint(float[] rgb, StatsData stats) {
		if (rgb == null || rgb.length < 3) return rgb;
		if (this.currentFormTintIntensity <= 0.0f && this.currentTintProgress <= 0.0f) return rgb;

		float[] tinted = rgb.clone();
		tintInPlace(tinted);
		return tinted;
	}

	private void tintInPlace(float[] rgb) {
		if (this.currentFormTintIntensity > 0.0f && this.currentFormTintColor != null) {
			applyFormTintToRgb(rgb, this.currentFormTintColor, this.currentFormTintIntensity);
		} else if (this.currentTintProgress > 0.0f) {
			applyAuraTintToRgb(rgb, this.currentAuraColor, 0.2f * this.currentTintProgress);
		}
	}

	private void applyFormTintToRgb(float[] rgb, float[] tint, float intensity) {
		intensity = Mth.clamp(intensity, 0.0f, 1.0f) * AuraTintTracker.darkTintScale(rgb);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - intensity) + tint[0] * intensity, 0.0f, 1.0f);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - intensity) + tint[1] * intensity, 0.0f, 1.0f);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - intensity) + tint[2] * intensity, 0.0f, 1.0f);
	}

	private void applyAuraTintToRgb(float[] rgb, float[] auraRgb, float intensity) {
		intensity *= AuraTintTracker.darkTintScale(rgb);
		rgb[0] = rgb[0] * (1.0f - intensity) + (auraRgb[0] * intensity);
		rgb[1] = rgb[1] * (1.0f - intensity) + (auraRgb[1] * intensity);
		rgb[2] = rgb[2] * (1.0f - intensity) + (auraRgb[2] * intensity);
	}

	public static float[] lerpColor(float factor, float[] current, float[] target) {
		return new float[]{
				Mth.lerp(factor, current[0], target[0]),
				Mth.lerp(factor, current[1], target[1]),
				Mth.lerp(factor, current[2], target[2])
		};
	}

	public static Identifier getSafeTexture(Identifier originalLoc) {
		return VALIDATED_TEXTURES_CACHE.computeIfAbsent(originalLoc, loc -> {
			System.out.println("Validating texture: " + loc);
			if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
			return BLANK_TEXTURE;
		});
	}

	public static Identifier getSafeTexture(Identifier originalLoc, Identifier fallbackLoc) {
		return VALIDATED_TEXTURES_CACHE.computeIfAbsent(originalLoc, loc -> {
			if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
			return fallbackLoc;
		});
	}

	public static void clearValidatedTexturesCache() {
		VALIDATED_TEXTURES_CACHE.clear();
	}
}
