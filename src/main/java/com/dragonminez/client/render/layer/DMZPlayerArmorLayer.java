package com.dragonminez.client.render.layer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.client.render.DMZPlayerRenderData;
import com.dragonminez.client.render.GeoEntityRenderState;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders worn armor on the player's geo armor bones, applying DMZ's per-form visibility rules.
 *
 * <h2>GeckoLib 5 migration</h2>
 * The old implementation hooked {@code renderForBone} and manually drove
 * {@code ModelPart#render} against a {@code SubmitNodeCollector} buffer, overriding
 * {@code getArmorItemForBone / getEquipmentSlotForBone / getModelPartForBone /
 * getModelForItem / getVanillaArmorBuffer}. All six are gone in GL5 — {@code ItemArmorGeoLayer}
 * now owns the whole armor submission path and only exposes {@code getRelevantBones}.
 *
 * <p>What survives is the valuable part: the <b>per-form armor visibility rules</b>
 * (restricted Majin body types, female human/saiyan bodies, Oozaru, buffed forms, custom models,
 * cosmetic-armor overlay). Those are evaluated in {@link #getRelevantBones} — which still receives
 * the live animatable — and produce the bone → armor-segment bindings for the frame.
 *
 * <p>Custom DBZ armor textures are resolved by {@code ArmorTextureResolver} through the equipment
 * asset path; the explicit {@code RenderType.armorCutoutNoCull} plumbing no longer exists.
 */
public class DMZPlayerArmorLayer<T extends AbstractClientPlayer & GeoAnimatable>
		extends ItemArmorGeoLayer<T, Void, LivingEntityRenderState> {

	/**
	 * @param geoRenderer the owning renderer
	 * @param context     vanilla's renderer context — <b>must not be null</b>
	 *
	 * <p>GeckoLib's {@code ItemArmorGeoLayer} constructor immediately reads four collaborators off
	 * this context ({@code getEquipmentRenderer}, {@code getEquipmentAssets},
	 * {@code getPlayerSkinRenderCache}, {@code getItemModelResolver}), so passing {@code null} threw
	 * a NullPointerException the moment the player entity was first rendered:
	 * <pre>
	 * Cannot invoke "...Context.getEquipmentRenderer()" because "context" is null
	 *     at ItemArmorGeoLayer.&lt;init&gt;
	 *     at DMZPlayerArmorLayer.&lt;init&gt;
	 *     at DMZPlayerRenderer.&lt;init&gt;
	 * </pre>
	 * {@code DMZPlayerItemInHandLayer} already forwarded it correctly; this layer was the only one
	 * that dropped it.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public DMZPlayerArmorLayer(GeoRenderer<T, ?, ?> geoRenderer,
							   net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super((GeoRenderer) geoRenderer, context);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected List getRelevantBones(RenderPassInfo info) {
		DMZPlayerRenderData data = (DMZPlayerRenderData) info.renderState().getGeckolibData(DMZPlayerRenderData.TICKET);
		StatsData stats = data == null ? null : data.stats();
		List<RenderData> bones = new ArrayList<>(8);
		boolean firstPerson = info.renderState() instanceof GeoEntityRenderState state && state.firstPersonBody;
		if (!firstPerson) addBone(info, bones, RenderData.head("armorHead"), EquipmentSlot.HEAD, stats);
		addBone(info, bones, RenderData.body("armorBody"), EquipmentSlot.CHEST, stats);
		addBone(info, bones, RenderData.rightArm("armorRightArm"), EquipmentSlot.CHEST, stats);
		addBone(info, bones, RenderData.leftArm("armorLeftArm"), EquipmentSlot.CHEST, stats);
		addBone(info, bones, RenderData.rightLeg("armorRightLeg"), EquipmentSlot.LEGS, stats);
		addBone(info, bones, RenderData.leftLeg("armorLeftLeg"), EquipmentSlot.LEGS, stats);
		addBone(info, bones, RenderData.rightFoot("armorRightBoot"), EquipmentSlot.FEET, stats);
		addBone(info, bones, RenderData.leftFoot("armorLeftBoot"), EquipmentSlot.FEET, stats);
		return bones;
	}

	private void addBone(RenderPassInfo info, List<RenderData> bones, RenderData binding, EquipmentSlot slot, StatsData stats) {
		if (info.model().getBone(binding.boneName()).isEmpty()
				|| !shouldRenderArmorOn(binding.boneName(), slot, stats)) return;
		bones.add(binding);
	}

	/** DMZ's per-form armor visibility rules, keyed on the bone being drawn. */
	private boolean shouldRenderArmorOn(String boneName, EquipmentSlot slot, StatsData stats) {
		if (stats == null) return true;

		var character = stats.getCharacter();
		if (character == null) return true;

		String logicKey = character.getRenderLogicKey();
		if (logicKey.equals("candy")) return false;

		if (!"armorBody".equals(boneName)) return true;

		String race = character.getRaceName().toLowerCase();
		String gender = character.getGender().toLowerCase();

		boolean isArmored = character.getArmored();
		boolean isMajin = logicKey.equals("majin");
		boolean isFemaleHumanOrSaiyan = (race.equals("human") || race.equals("saiyan"))
				&& gender.equals(Character.GENDER_FEMALE);
		boolean isOozaru = character.isOozaruCached() || logicKey.contains("oozaru");

		boolean isBuffed = logicKey.contains("buffed") || logicKey.contains("frostdemon_fp")
				|| logicKey.contains("majin_ultra") || logicKey.contains("namekian_orange")
				|| logicKey.contains("bioandroid_ultra") || logicKey.contains("ssj4d")
				|| logicKey.contains("ssj4gt") || logicKey.contains("frostdemon_fifth")
				|| logicKey.contains("frostdemon_metalcore") || logicKey.contains("namekian_buffed")
				|| logicKey.contains("4arms") || logicKey.contains("bioandroid_xeno")
				|| logicKey.contains("janemba_super");

		boolean isRestrictedMajin = (isMajin && character.getBodyType() != 2) || logicKey.equals("janemba_fat");
		boolean isCustomModel = SkinGathererProvider.modelFamily(logicKey).equals("custom");

		if (isRestrictedMajin || isFemaleHumanOrSaiyan || isOozaru) {
			return isArmored;
		}
		if (isBuffed || isCustomModel) {
			// Custom/buffed bodies hide vanilla-style armor; DBZ armor is drawn by DMZSkinLayer.
			return false;
		}
		return true;
	}

	/** Cosmetic-armor aware stack lookup, retained for callers outside the render pass. */
	protected ItemStack resolveArmorStack(AbstractClientPlayer player, EquipmentSlot slot) {
		ItemStack stack = player.getItemBySlot(slot);

		if (CosmeticArmorCompat.isLoaded()) {
			ItemStack cosStack = CosmeticArmorCompat.getCosmeticStack(player, slot);
			if (cosStack != null) stack = cosStack;
		}

		if (stack.isEmpty()) return ItemStack.EMPTY;
		if (!stack.has(DataComponents.EQUIPPABLE) && !(stack.getItem() instanceof DbzArmorItem)) return ItemStack.EMPTY;
		return stack;
	}

	/** Whether this stack is drawn with DMZ's own armor model rather than a vanilla one. */
	protected boolean usesDmzModel(ItemStack stack) {
		return stack.getItem() instanceof DbzArmorTextured;
	}
}
