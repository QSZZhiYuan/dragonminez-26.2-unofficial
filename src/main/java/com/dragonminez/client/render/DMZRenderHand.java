package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.model.KiWeaponModelLoader;
import com.dragonminez.common.combat.logic.weapon.KiWeaponHelper;
import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.render.layer.AuraTintTracker;
import com.dragonminez.client.render.layer.BodyLayerFadeTracker;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * First-person hand/arm renderer for DMZ's custom body layers.
 *
 * <h2>26.2 migration</h2>
 * This used to extend {@code LivingEntityRenderer<AbstractClientPlayer, AvatarRenderState,
 * PlayerModel<AvatarRenderState>>} purely as a convenient host for a {@link PlayerModel} instance and
 * a couple of inherited helpers. Three 1.21-isms made that impossible in 26.2:
 * <ul>
 *   <li>{@code PlayerModel} is no longer generic ({@code PlayerModel extends
 *       HumanoidModel<AvatarRenderState>}),</li>
 *   <li>{@code EntityRenderer} dropped {@code render(...)} in favour of {@code submit(state, ...)}
 *       and gained an abstract {@code createRenderState()}, and</li>
 *   <li>{@code getTextureLocation} now takes the render state, not the entity.</li>
 * </ul>
 * None of the inherited behaviour was actually used: the class is driven exclusively from
 * {@code HeldItemRendererMixin}, which calls {@link #renderRightHand} / {@link #renderLeftHand}
 * directly. So this is now a plain helper that owns its {@link PlayerModel} — no renderer
 * superclass, no abstract contract to satisfy.
 */
@OnlyIn(Dist.CLIENT)
public class DMZRenderHand {

	/** The humanoid model this renderer drives. */
	private final PlayerModel model;
	private static final float KI_GEO_TX = -0.06F;
	private static final float KI_GEO_TY = -0.03F;
	private static final float KI_GEO_TZ = -0.1F;
	private static final float KI_GEO_RX = 0.0F;
	private static final float KI_GEO_RY = 15.0F;
	private static final float KI_GEO_RZ = 0.0F;

	private static final float[] WHITE_COLOR = {1.0F, 1.0F, 1.0F};
	private final float[] colorBuffer = new float[3];

	public DMZRenderHand(EntityRendererProvider.Context pContext) {
		this.model = new PlayerModel(pContext.bakeLayer(ModelLayers.PLAYER), false);
	}

	public void renderRightHand(PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));

		if (stats.getStatus().isBlocking()) {
			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, 1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();

			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, -1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		} else {
			pPoseStack.pushPose();
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();
		}

		this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.RIGHT);

		if ((stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura()) && !stats.getStatus().isAndroidUpgraded())
			queueFirstPersonAura(pPlayer, pPoseStack, pCombinedLight);
	}

	public void renderLeftHand(PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer) {
		var statsCap = StatsProvider.get(StatsCapability.INSTANCE, pPlayer);
		var stats = statsCap.orElse(new StatsData(pPlayer));

		if (stats.getStatus().isBlocking()) {
			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, 1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.rightArm, this.model.rightSleeve);
			pPoseStack.popPose();

			pPoseStack.pushPose();
			applyBlockingTransform(pPoseStack, -1.0F);
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		} else {
			pPoseStack.pushPose();
			this.renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, this.model.leftArm, this.model.leftSleeve);
			pPoseStack.popPose();
		}

		this.renderKiWeapon(pPoseStack, pBuffer, pCombinedLight, pPlayer, stats, HumanoidArm.LEFT);
	}

	private void applyBlockingTransform(PoseStack stack, float side) {
		stack.translate(side * -0.25F, -0.15F, -0.4F);
		stack.mulPose(Axis.XP.rotationDegrees(-20.0F));
		stack.mulPose(Axis.YP.rotationDegrees(100.0F));
		stack.mulPose(Axis.ZP.rotationDegrees(side * 330.0F));
	}

	private void renderHand(PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, ModelPart pRendererArm, ModelPart pRendererArmwear) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, pPlayer).orElse(new StatsData(pPlayer));
		FormConfig.FormData tintForm = DMZSkinLayer.resolveTintForm(stats);
		final float[] formTintColor = tintForm != null ? tintForm.getRgbTintColor() : null;
		final float formTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;

		// 26.2: HumanoidModel#setupAnim now takes a HumanoidRenderState instead of the raw
		// (entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch) tuple, and
		// attackTime/crouching/swimAmount are fields of that state.
		net.minecraft.client.renderer.entity.state.AvatarRenderState handState =
				new net.minecraft.client.renderer.entity.state.AvatarRenderState();
		handState.attackTime = 0.0F;
		handState.isCrouching = false;
		handState.swimAmount = 0.0F;
		this.model.setupAnim(handState);
		pRendererArm.xRot = 0.0F;

		String raceName = stats.getCharacter().getRaceName().toLowerCase();
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);

		final List<BodyLayerFadeTracker.FadingLayer> fadingLayers = new ArrayList<>();
		SkinGathererProvider.BodyLayerSink layerConsumer = new SkinGathererProvider.BodyLayerSink() {
			@Override
			public void base(Identifier texture, float[] color) {
				applyFormTint(color, formTintColor, formTintIntensity, colorBuffer);
				renderPart(pPoseStack, pBuffer, pCombinedLight, pRendererArm, texture, colorBuffer);
			}

			@Override
			public void fading(String layerId, Identifier texture, float[] color, float targetAlpha) {
				fadingLayers.add(new BodyLayerFadeTracker.FadingLayer(layerId, texture, color, targetAlpha));
			}
		};

		float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		SkinGathererProvider.INSTANCE.gatherBodyLayers(pPlayer, stats, pt, layerConsumer);
		addSsj4HandFur(stats, fadingLayers);
		SkinGathererProvider.INSTANCE.gatherAndroidLayers(pPlayer, stats, pt, layerConsumer);
		SkinGathererProvider.INSTANCE.gatherTattooLayers(pPlayer, stats, pt, layerConsumer);
		SkinGathererProvider.INSTANCE.gatherEffectLayers(pPlayer, stats, pt, layerConsumer);
		renderFadingHandLayers(pPoseStack, pBuffer, pCombinedLight, pPlayer, pRendererArm, formTintColor, formTintIntensity, fadingLayers);

		renderDbzArmor(pPoseStack, pBuffer, pCombinedLight, pPlayer, pRendererArm);
	}

	private void renderKiWeapon(PoseStack ps, SubmitNodeCollector buffer, int light, AbstractClientPlayer player, StatsData stats, HumanoidArm arm) {
		if (!stats.getSkills().isSkillActive("kimanipulation")) return;
		String type = stats.getStatus().getKiWeaponType();
		if (type == null || type.equalsIgnoreCase("none")) return;
		float[] color = KiWeaponHelper.resolveColorForType(type, getKiColor(stats));
		String lower = type.toLowerCase();

		ModelPart geoPart = KiWeaponModelLoader.get(lower);
		if (geoPart == null) return;

		Identifier tex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/weapons/kiweapon_" + lower + ".png");
		ps.pushPose();
		ps.translate(KI_GEO_TX, KI_GEO_TY, KI_GEO_TZ);
		if (KI_GEO_RX != 0.0F) ps.mulPose(Axis.XP.rotationDegrees(KI_GEO_RX));
		if (KI_GEO_RY != 0.0F) ps.mulPose(Axis.YP.rotationDegrees(KI_GEO_RY));
		if (KI_GEO_RZ != 0.0F) ps.mulPose(Axis.ZP.rotationDegrees(KI_GEO_RZ));
		renderKiPartTex(ps, buffer, light, geoPart, color, tex);
		ps.popPose();
	}

    private void renderDbzArmor(PoseStack ps, SubmitNodeCollector pBuffer, int pCombinedLight, AbstractClientPlayer player, ModelPart pRendererArm) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (CosmeticArmorCompat.isLoaded()) {
            ItemStack cosmeticStack = CosmeticArmorCompat.getCosmeticStack(player, EquipmentSlot.CHEST);
            if (cosmeticStack != null) {
                if (cosmeticStack.isEmpty()) return;
                chestStack = cosmeticStack;
            }
        }
        if (chestStack.isEmpty()) return;

        String modId = Reference.MOD_ID;
        String itemId = null;

        if (chestStack.getItem() instanceof DbzArmorItem dbzItem) {
            modId = dbzItem.getModId();
            itemId = dbzItem.getItemId();
        } else if (chestStack.getItem() instanceof DbzArmorTextured textured) {
            itemId = textured.getItemId();
        }

        if (itemId != null) {
            Identifier armorResource = ArmorTextureResolver.resolve(modId, itemId, EquipmentSlot.CHEST, chestStack);

            ps.pushPose();

            boolean isRightArm = (pRendererArm == this.model.rightArm);

            float armorInflation = 1.05F;
            ps.scale(armorInflation, armorInflation, armorInflation);

            ps.translate(isRightArm ? 0.02D : -0.01, -0.04D, 0.0D);

            renderPart(ps, pBuffer, pCombinedLight, pRendererArm, armorResource, WHITE_COLOR);

            ps.popPose();
        }
    }

	private void addSsj4HandFur(StatsData stats, List<BodyLayerFadeTracker.FadingLayer> out) {
		DMZSkinLayer.Ssj4Overlay ssj4 = DMZSkinLayer.resolveSsj4Overlay(stats);
		if (ssj4 == null) return;
		Identifier tex = DMZSkinLayer.getSafeTexture(SkinGathererProvider.getCachedTexture("textures/entity/races/humansaiyan/" + ssj4.key() + "_layer1.png"));
		out.add(new BodyLayerFadeTracker.FadingLayer("ssj4fur", tex, ssj4.color(), ssj4.target()));
	}

	private void renderFadingHandLayers(PoseStack ps, SubmitNodeCollector buffer, int light, AbstractClientPlayer player, ModelPart arm, float[] formTintColor, float formTintIntensity, List<BodyLayerFadeTracker.FadingLayer> active) {
		int id = player.getId();
		long gameTime = player.level().getGameTime();
		for (BodyLayerFadeTracker.RenderEntry entry : BodyLayerFadeTracker.update(id, gameTime, active)) {
			if (entry.alpha() <= 0.001F) continue;
			applyFormTint(entry.color(), formTintColor, formTintIntensity, colorBuffer);
			renderPart(ps, buffer, light, arm, entry.texture(), colorBuffer, entry.alpha());
		}
	}

	private void renderPart(PoseStack stack, SubmitNodeCollector buffer, int light, ModelPart part, Identifier texture, float[] rgb) {
		renderPart(stack, buffer, light, part, texture, rgb, 1.0F);
	}

	private void renderPart(PoseStack stack, SubmitNodeCollector buffer, int light, ModelPart part, Identifier texture, float[] rgb, float alpha) {
		// 26.2: SubmitNodeCollector has no getBuffer - model parts are submitted directly and the
		// tint travels as the submitted colour instead of a per-vertex override.
		buffer.submitModelPart(part, stack, RenderTypes.entityTranslucent(texture), light, OverlayTexture.NO_OVERLAY,
				null, net.minecraft.util.ARGB.color((int)((alpha) * 255.0F), (int)((rgb[0]) * 255.0F), (int)((rgb[1]) * 255.0F), (int)((rgb[2]) * 255.0F)),
				null, 0);
	}

	private Identifier loc(String path) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, path);
	}

	private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer pPlayer, InteractionHand pHand) {
		ItemStack itemstack = pPlayer.getItemInHand(pHand);
		if (itemstack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
		else {
			if (pPlayer.getUsedItemHand() == pHand && pPlayer.getUseItemRemainingTicks() > 0) {
				ItemUseAnimation useanim = itemstack.getUseAnimation();
				if (useanim == ItemUseAnimation.BLOCK) return HumanoidModel.ArmPose.BLOCK;
				if (useanim == ItemUseAnimation.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
				if (useanim == ItemUseAnimation.SPEAR) return HumanoidModel.ArmPose.SPEAR;
				if (useanim == ItemUseAnimation.CROSSBOW && pHand == pPlayer.getUsedItemHand())
					return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
				if (useanim == ItemUseAnimation.SPYGLASS) return HumanoidModel.ArmPose.SPYGLASS;
				if (useanim == ItemUseAnimation.TOOT_HORN) return HumanoidModel.ArmPose.TOOT_HORN;
				if (useanim == ItemUseAnimation.BRUSH) return HumanoidModel.ArmPose.BRUSH;
			} else if (!pPlayer.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
				return HumanoidModel.ArmPose.CROSSBOW_HOLD;
			}
			HumanoidModel.ArmPose forgeArmPose = IClientItemExtensions.of(itemstack).getArmPose(pPlayer, pHand, itemstack);
			return forgeArmPose != null ? forgeArmPose : HumanoidModel.ArmPose.ITEM;
		}
	}


	private void renderKiPartTex(PoseStack ps, SubmitNodeCollector buffer, int light, ModelPart part, float[] color, Identifier texture) {
		// 26.2: see renderPart above - submit the part, carry the tint in the submit colour.
		buffer.submitModelPart(part, ps, ModRenderTypes.kiblast(texture), light, OverlayTexture.NO_OVERLAY,
				null, net.minecraft.util.ARGB.color((int)((0.85F) * 255.0F), (int)((color[0]) * 255.0F), (int)((color[1]) * 255.0F), (int)((color[2]) * 255.0F)),
				null, 0);
	}

	private float[] getKiColor(StatsData stats) {
		var character = stats.getCharacter();
		float[] kiColor = character.getRgbAuraColor();
		if (character.hasActiveForm() && character.getActiveFormData() != null) {
			float[] formColor = character.getActiveFormData().getRgbAuraColor();
			if (formColor != null) kiColor = formColor;
		}
		return kiColor;
	}

	private void applyFormTint(float[] source, float[] tint, float intensity, float[] dest) {
		if (intensity <= 0.0f || tint == null) {
			dest[0] = source[0];
			dest[1] = source[1];
			dest[2] = source[2];
			return;
		}

		float i = Mth.clamp(intensity, 0.0f, 1.0f) * AuraTintTracker.darkTintScale(source);
		dest[0] = Mth.clamp(source[0] * (1.0f - i) + tint[0] * i, 0.0f, 1.0f);
		dest[1] = Mth.clamp(source[1] * (1.0f - i) + tint[1] * i, 0.0f, 1.0f);
		dest[2] = Mth.clamp(source[2] * (1.0f - i) + tint[2] * i, 0.0f, 1.0f);
	}

	private void queueFirstPersonAura(AbstractClientPlayer player, PoseStack poseStack, int packedLight) {
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		PlayerEffectQueue.addFirstPersonAura(player, poseStack, partialTick, packedLight);
	}
}
