package com.dragonminez.client.render.layer;

import net.minecraft.client.renderer.item.ItemStackRenderState;

import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.util.WeaponGripProfile;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.geckolib.animatable.GeoItem;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Captures held items on their grip bones for GeckoLib's deferred render pass. */
public class DMZPlayerItemInHandLayer<T extends AbstractClientPlayer & GeoAnimatable>
		extends BlockAndItemGeoLayer<T, Void, GeoRenderState> {

	private static final String RIGHT_GRIP = "right_hand_item";
	private static final String LEFT_GRIP = "left_hand_item";
	private static final DataTicket<GripPoses> GRIPS = DataTicket.create("dmz_held_item_grips", GripPoses.class);

	public DMZPlayerItemInHandLayer(GeoRenderer<T, ?, ?> renderer,
									net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super(context, (GeoRenderer) renderer);
	}

	@Override
	public void addRenderData(T animatable, Void unused, GeoRenderState renderState, float partialTick) {
		renderState.addGeckolibData(CONTENTS, getRelevantBones(animatable, unused, renderState, partialTick));
	}

	@Override
	protected List<RenderData> getRelevantBones(T animatable, Void unused,
													GeoRenderState renderState, float partialTick) {
		if (animatable == null || animatable.isInvisible()) return List.of();
		List<RenderData> bones = new ArrayList<>(2);

		boolean twoHanded = PlayerAttackHelper.isTwoHandedWielding(animatable);
		HumanoidArm mainArm = animatable.getMainArm();

		ItemStack right = twoHanded && mainArm != HumanoidArm.RIGHT
				? ItemStack.EMPTY
				: (mainArm == HumanoidArm.RIGHT ? animatable.getMainHandItem() : animatable.getOffhandItem());
		ItemStack left = twoHanded && mainArm != HumanoidArm.LEFT
				? ItemStack.EMPTY
				: (mainArm == HumanoidArm.LEFT ? animatable.getMainHandItem() : animatable.getOffhandItem());
		float itemScale = resolveItemScale(animatable);
		renderState.addGeckolibData(GRIPS, new GripPoses(
				captureGrip(animatable, right, false, itemScale),
				captureGrip(animatable, left, true, itemScale)));

		addIfPresent(bones, RIGHT_GRIP, right, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, animatable);
		addIfPresent(bones, LEFT_GRIP, left, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, animatable);
		return List.copyOf(bones);
	}

	private void addIfPresent(List<RenderData> bones, String bone, ItemStack stack, ItemDisplayContext context, T animatable) {
		if (stack == null || stack.isEmpty()) return;

		ItemStackRenderState state = new ItemStackRenderState();
		this.itemModelResolver.updateForLiving(state, stack, context, animatable);

		bones.add(RenderData.item(bone, context, state));
	}

	@Override
	protected void submitItemStackRender(PoseStack poseStack, GeoBone bone, ItemStackRenderState stack,
			ItemDisplayContext displayContext, GeoRenderState renderState, SubmitNodeCollector collector, int packedLight) {
		GripPoses grips = renderState.getGeckolibData(GRIPS);
		GripPose grip = grips == null ? null : (LEFT_GRIP.equals(bone.name()) ? grips.left() : grips.right());
		poseStack.pushPose();
		try {
			if (grip != null) {
				// Keep the animated grip rotation already supplied by GeckoLib. The original
				// layer adds a second, eased grip rotation for resting placement; cancelling the
				// first one makes the blade stop following the hand during combat.
				float x = bone.baseRotX() + (bone.frameSnapshot == null ? 0 : bone.frameSnapshot.getRotX());
				float y = bone.baseRotY() + (bone.frameSnapshot == null ? 0 : bone.frameSnapshot.getRotY());
				float z = bone.baseRotZ() + (bone.frameSnapshot == null ? 0 : bone.frameSnapshot.getRotZ());
				float restWeight = 1 - grip.combatWeight();
				if (restWeight > 0) {
					poseStack.mulPose(Axis.ZP.rotation(z * restWeight));
					poseStack.mulPose(Axis.YP.rotation(y * restWeight));
					poseStack.mulPose(Axis.XP.rotation(x * restWeight));
				}
				poseStack.mulPose(grip.transform());
			}
			super.submitItemStackRender(poseStack, bone, stack, displayContext, renderState, collector, packedLight);
		} finally {
			poseStack.popPose();
		}
	}

	private GripPose captureGrip(T player, ItemStack stack, boolean isLeft, float itemScale) {
		if (stack.isEmpty()) return null;
		float combatWeight = 0;
		if (player instanceof IPlayerAnimatable animated) {
			boolean offhand = isLeft == (player.getMainArm() == HumanoidArm.RIGHT);
			if (offhand == animated.dragonminez$isAttackingWithOffhand())
				combatWeight = animated.dragonminez$getCombatPlacementWeight();
		}
		boolean selfPosed = stack.getItem() instanceof GeoItem;
		float gripWeight = selfPosed && isLeft ? 1 : 1 - combatWeight;
		PoseStack transform = new PoseStack();
		if (gripWeight > 0) {
			var attributes = WeaponRegistry.getAttributes(stack);
			WeaponGripProfile profile = WeaponGripProfile.resolve(stack.getItem(),
					player.isUsingItem() && player.getUseItem() == stack,
					attributes == null ? null : attributes.category());
			boolean restFlip = !selfPosed && combatWeight <= 0 && player.getMainArm() == HumanoidArm.LEFT;
			PoseStack profilePose = new PoseStack();
			profile.apply(profilePose, selfPosed ? false : (restFlip ? !isLeft : isLeft));
			Matrix4f matrix = profilePose.last().pose();
			Vector3f translation = matrix.getTranslation(new Vector3f());
			Quaternionf rotation = new Quaternionf().slerp(matrix.getNormalizedRotation(new Quaternionf()), gripWeight);
			transform.translate(translation.x * gripWeight, translation.y * gripWeight, translation.z * gripWeight);
			transform.mulPose(rotation);
			if (restFlip) transform.translate(0.1 * gripWeight, 0, 0);
		}
		transform.scale(itemScale, itemScale, itemScale);
		return new GripPose(new Matrix4f(transform.last().pose()), combatWeight);
	}

	private float resolveItemScale(T player) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return 1;
		Float[] scaling = stats.getCharacter().getResolvedModelScaling();
		if (scaling == null || scaling.length < 3) return 1;
		float uniform = (scaling[0] + scaling[1] + scaling[2]) / 3;
		float vanillaScale = 0.9375F;
		float dampening = (3.8F / 4 - vanillaScale) / (float) Math.sqrt(3.8F - vanillaScale);
		float dampened = uniform <= vanillaScale ? uniform : vanillaScale + (float) Math.sqrt(uniform - vanillaScale) * dampening;
		return Math.max(0.25F, Math.min(dampened, 8));
	}

	private record GripPose(Matrix4f transform, float combatWeight) {}
	private record GripPoses(GripPose right, GripPose left) {}
}
