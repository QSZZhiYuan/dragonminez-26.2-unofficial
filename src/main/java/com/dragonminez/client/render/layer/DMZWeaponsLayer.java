package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderData;
import com.dragonminez.common.combat.logic.weapon.KiWeaponHelper;
import com.dragonminez.common.stats.StatsData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the ki-manipulation weapon (a separate geo model) parented to the player's arms.
 *
 * <h2>GeckoLib 5 migration</h2>
 * The old implementation hooked {@code renderForBone(...)} and called
 * {@code renderer.renderRecursively(...)} against a live {@code VertexConsumer}. Neither exists in
 * GL5: per-bone work is registered through
 * {@code addPerBoneRender(RenderPassInfo, BiConsumer<GeoBone, PerBoneRender>)} and the actual
 * drawing is deferred to a {@code SubmitNodeCollector}.
 *
 * <p>The nested weapon model is submitted by {@code geometryRenderer.submitCustomGeometry} with the
 * bone's pose applied — the weapon model's own draw calls go through the
 * {@link net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer} callback, which
 * still hands us a {@code VertexConsumer}.
 */
public class DMZWeaponsLayer<T extends AbstractClientPlayer & GeoAnimatable>
		extends GeoRenderLayer<T, Void, GeoRenderState> {

	private static final float OOZARU_WEAPON_SCALE = 3.8f;
	private static final float[] HUMAN_ARM_RIGHT = {-5f, 22f, 0f};
	private static final float[] HUMAN_ARM_LEFT = {5f, 22f, 0f};
	private static final float[] OOZARU_ARM_RIGHT = {-12f, 74f, 0f};
	private static final float[] OOZARU_ARM_LEFT = {21f, 74f, 0f};

	public DMZWeaponsLayer(GeoRenderer<T, ?, ?> entityRendererIn) {
		super((GeoRenderer) entityRendererIn);
	}

	@Override
	public void addPerBoneRender(RenderPassInfo<GeoRenderState> info,
								 java.util.function.BiConsumer<GeoBone, com.geckolib.renderer.base.PerBoneRender<GeoRenderState>> consumer) {
		DMZPlayerRenderData data = info.renderState().getGeckolibData(DMZPlayerRenderData.TICKET);
		if (data == null || data.spectator()) return;

		StatsData stats = data.stats();
		if (stats == null || !stats.getSkills().isSkillActive("kimanipulation")) return;

		String weaponType = stats.getStatus().getKiWeaponType();
		if (weaponType == null || weaponType.equalsIgnoreCase("none")) return;

		var player = data.player();
		if (player == null || !player.getMainHandItem().isEmpty()) return;

		String type = weaponType.toLowerCase();
		Identifier modelLoc = weaponModel(type);
		if (modelLoc == null) return;

		BakedGeoModel weaponModel = getGeoModel().getBakedModel(modelLoc);
		if (weaponModel == null) return;

		Identifier texture = weaponTexture(type);
		if (texture == null) return;

		boolean isRight = player.getMainArm() == HumanoidArm.RIGHT;
		String armBoneName = isRight ? "right_arm" : "left_arm";
		GeoBone armBone = boneByName(info, armBoneName);
		if (armBone == null) return;
		consumer.accept(armBone, (passInfo, bone, collector) -> {
			var character = stats.getCharacter();
			boolean isOozaru = character != null && character.isOozaruCached();

			PoseStack localTransform = new PoseStack();

			if (type.equals("clawlance")) {
				localTransform.mulPose(Axis.YP.rotationDegrees(35.0F));
				localTransform.mulPose(Axis.XP.rotationDegrees(35.0F));
				localTransform.translate(0.0F / 16f, -0.1F, -1.0F);
			}

			if (isOozaru) {
				float[] humanPivot = isRight ? HUMAN_ARM_RIGHT : HUMAN_ARM_LEFT;
				float[] oozaruPivot = isRight ? OOZARU_ARM_RIGHT : OOZARU_ARM_LEFT;
				float k = OOZARU_WEAPON_SCALE;
				localTransform.translate(oozaruPivot[0] / 16f, oozaruPivot[1] / 16f, oozaruPivot[2] / 16f);
				localTransform.scale(k, k, k);
				localTransform.translate(-humanPivot[0] / 16f, -humanPivot[1] / 16f, -humanPivot[2] / 16f);
			}

			float[] color = KiWeaponHelper.resolveColorForType(weaponType, getKiColor(stats));
			var weaponRenderType = com.dragonminez.client.render.util.ModRenderTypes.energy2(texture);

            int tint = ARGB.colorFromFloat(1, color[0], color[1], color[2]);
            for (GeoBone root : weaponModel.topLevelBones()) {
                com.dragonminez.client.render.util.GeoAttachmentRenderer.submitAttached(passInfo,
                        passInfo.poseStack(), collector, root, weaponRenderType,
                        15728880, OverlayTexture.NO_OVERLAY, tint, 1.0f,
                        localTransform.last().pose(), java.util.Set.of());
            }
		});
	}

	/** Resolves a bone by name from the baked model, or the root bone as a fallback. */
	private static GeoBone boneByName(RenderPassInfo<GeoRenderState> info, String name) {
		return info.model().getBone(name).orElseGet(() ->
				info.model().getBone("root").orElse(null));
	}

	private static Identifier weaponModel(String type) {
		Identifier loc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/kiweapon_" + type );
        Identifier file = loc.withPath(path -> "geckolib/models/" + path + ".geo.json");
		return Minecraft.getInstance().getResourceManager().getResource(file).isPresent() ? loc : null;
	}

	private static Identifier weaponTexture(String type) {
		Identifier loc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/weapons/kiweapon_" + type + ".png");
		return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : null;
	}

	private static float[] getKiColor(StatsData stats) {
		var character = stats.getCharacter();
		float[] kiColor = character.getRgbAuraColor();
		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			String formColor = character.getActiveFormData().getAuraColor();
			if (formColor != null && !formColor.isEmpty()) kiColor = character.getActiveFormData().getRgbAuraColor();
		}
		return kiColor;
	}
}
