package com.dragonminez.client.util;

import net.minecraft.core.component.DataComponents;

import com.dragonminez.client.render.layer.DMZCustomArmorLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;

import java.util.Objects;
import com.geckolib.renderer.layer.GeoRenderLayer;

public class BoneVisibilityHandler {

    /**
     * GeckoLib 5 replacement for the removed GeoBone#setHidden(boolean).
     *
     * In GeckoLib 4 visibility was a mutable flag on the bone itself. In GeckoLib 5 the bone is
     * immutable and every per-frame render decision lives on com.geckolib.animation.state.BoneSnapshot
     * (GeoBone#frameSnapshot), which AnimationProcessor#createBoneSnapshots rebuilds before each
     * render pass. skipRender is the direct equivalent of setHidden; skipChildrenRender hides a
     * whole subtree.
     *
     * frameSnapshot is null outside a render pass, so this is a no-op there - callers in render or
     * extract code always run after the snapshots are built.
     */
    public static void setBoneHidden(GeoBone bone, boolean hidden) {
        if (bone == null || bone.frameSnapshot == null) return;
        bone.frameSnapshot.skipRender(hidden);
    }

    /** True when the bone is skipped this frame. Mirrors the old GeoBone#isHidden(). */
    public static boolean isBoneHidden(GeoBone bone) {
        return bone != null && bone.frameSnapshot != null && bone.frameSnapshot.isHidden();
    }
	public static void updateVisibility(BakedGeoModel model, AbstractClientPlayer player, GeoRenderLayer<?, ?, ?> renderLayer) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		if (shouldShowBodyOnly(renderLayer, player)) {
			applyBodyOnlyVisibility(model);
			return;
		}

		var character = stats.getCharacter();
		String race = character.getRaceName().toLowerCase();
		String gender = character.getGender().toLowerCase();
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();

		boolean isFemale = gender.equals(Character.GENDER_FEMALE) || bodyType == 1;
		boolean isMajin = race.equals("majin");
		boolean isSaiyan = race.equals("saiyan");
		boolean isHuman = race.equals("human");
		boolean isNamekian = race.equals("namekian");
		boolean isSuperOrUltra = Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA);

		final String logicKey = character.getRenderLogicKey();


		boolean isSpectator = player.isSpectator();
		setBonesHidden(model, isSpectator, "body", "right_arm", "left_arm", "right_leg", "left_leg");
		model.getBone("head").ifPresent(head -> setBoneHidden(head, false));

		ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
		boolean hasChestplate = !chestStack.isEmpty();
		boolean isCape = hasChestplate && (chestStack.getItem() instanceof DbzArmorCapeItem);
		ItemStack legsStack = player.getItemBySlot(EquipmentSlot.LEGS);
		boolean hasLeggings = !legsStack.isEmpty();

		boolean isStandardBody = (isSaiyan || isHuman) && (bodyType == 0 || bodyType == 1);
		hideBone(model, "body_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.JACKET)));
		hideBone(model, "right_arm_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)));
		hideBone(model, "left_arm_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)));
		hideBone(model, "right_leg_layer", hasLeggings || (isStandardBody && !player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)));
		hideBone(model, "left_leg_layer", hasLeggings || (isStandardBody && !player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)));
		hideBone(model, "hat_layer", (isStandardBody && !player.isModelPartShown(PlayerModelPart.HAT)));

		hideBone(model, "boobas", isCape || !isFemale);


		model.getBone("tail1m").ifPresent(bone -> {
			boolean isTargetMajinModel = logicKey.equals("majin_kid") || logicKey.equals("majin_ultra") || logicKey.equals("majin");
			boolean showAntenna = (isMajin && isFemale && isSuperOrUltra) || (isTargetMajinModel && isFemale);
			setHiddenRecursive(bone, !showAntenna && !logicKey.equals("glinddark"));
		});

		model.getBone("tail1").ifPresent(bone -> {
			boolean showNormalTail;
            if (logicKey.equals("janemba_super") || logicKey.equals("janemba_fat")) return;

			boolean isTaillessRace = isHuman || isNamekian || isMajin;
			boolean isTaillessModel = logicKey.equals("human") ||
					logicKey.equals("namekian") ||
					logicKey.equals("namekian_orange") ||
					logicKey.equals("majin") ||
					logicKey.equals("majin_kid") ||
					logicKey.equals("majin_ultra");

			boolean configHasSaiyanTail = ConfigManager.getRaceCharacter(race) != null && ConfigManager.getRaceCharacter(race).getHasSaiyanTail();

			var form = character.getActiveFormData();
            var stack = character.getActiveStackFormData();
            if ((form != null && form.hidesTail()) || (stack != null && stack.hidesTail())) {
                showNormalTail = false;
            } else if (currentForm.contains("supersaiyan4") || logicKey.contains("ssj4")) {
				showNormalTail = true;
			} else if (isSaiyan || configHasSaiyanTail) {
				showNormalTail = stats.getStatus().isTailVisible() && stats.getCharacter().isHasSaiyanTail();
			} else if (logicKey.equals("glindtrueform") || logicKey.equals("trascended")) {
                showNormalTail = true;
            } else if (isTaillessRace || isTaillessModel) {
				showNormalTail = false;
			} else {
				showNormalTail = true;
			}

			setHiddenRecursive(bone, !showNormalTail);
		});

		setBonesHidden(model, true, "armorHead", "armorBody", "armorBody2", "armorLeggingsBody", "armorRightArm", "armorLeftArm",
				"armorLeftLeg", "armorLeftBoot", "armorRightLeg", "armorRightBoot");
		hideAllArmorPrefixBones(model);
	}

	private static void hideAllArmorPrefixBones(BakedGeoModel model) {
		for (GeoBone topLevelBone : model.topLevelBones()) {
			hideArmorPrefixRecursively(topLevelBone);
		}
	}

	private static void hideArmorPrefixRecursively(GeoBone bone) {
		String name = bone.name();
		if (name != null && name.startsWith("armor")) {
			setBoneHidden(bone, true);
		}

		for (GeoBone child : bone.children()) {
			hideArmorPrefixRecursively(child);
		}
	}

	public static boolean shouldShowBodyOnly(GeoRenderLayer<?, ?, ?> renderLayer, AbstractClientPlayer player) {
		return renderLayer instanceof DMZCustomArmorLayer<?> && hasModerfokinArmor(player);
	}

	public static boolean hasModerfokinArmor(AbstractClientPlayer player) {
		final ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
		if (stack.isEmpty() || !(stack.has(DataComponents.EQUIPPABLE))) return false;
		return stack.getItem() instanceof DbzArmorTextured;
	}

	public static void applyBodyOnlyVisibility(BakedGeoModel model) {
		for (GeoBone bone : model.topLevelBones()) {
			setHiddenRecursive(bone, true);
		}

		model.getBone("body").ifPresent(body -> {
			setBoneHidden(body, false);

			GeoBone parent = body.parent();
			while (parent != null) {
				setBoneHidden(parent, false);
				parent = parent.parent();
			}

			for (GeoBone child : body.children()) {
				if(child.name().equals("armorBody") || child.name().equals("armorLeggingsBody") || child.name().equals("body_layer")) {
					setBoneHidden(child, true);
					continue;
				}
				setHiddenRecursive(child, false);
			}
		});
	}

	public static void setBonesHidden(BakedGeoModel model, boolean shouldHide, String... boneNames) {
		for (String boneName : boneNames) {
			hideBone(model, boneName, shouldHide);
		}
	}

	public static void hideBone(BakedGeoModel model, String boneName, boolean shouldHide) {
		model.getBone(boneName).ifPresent(bone -> setBoneHidden(bone, shouldHide));
	}

	public static void setHiddenRecursive(GeoBone bone, boolean shouldHide) {
		setBoneHidden(bone, shouldHide);
		for (GeoBone child : bone.children()) {
			setHiddenRecursive(child, shouldHide);
		}
	}
}
