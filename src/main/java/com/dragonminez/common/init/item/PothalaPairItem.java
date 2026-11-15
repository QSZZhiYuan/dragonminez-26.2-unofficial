package com.dragonminez.common.init.item;

import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class PothalaPairItem extends Item {

	public static final String PAIR_ID_KEY = "PothalaPairId";

	private final Supplier<Item> leftEarring;
	private final Supplier<Item> rightEarring;

	public PothalaPairItem(Properties properties, Supplier<Item> leftEarring, Supplier<Item> rightEarring) {
		super(properties);
		this.leftEarring = leftEarring;
		this.rightEarring = rightEarring;
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slotId) {
		if (!level.isClientSide()) ensurePairId(stack, level.getRandom());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack pair = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			int pairId = ensurePairId(pair, level.getRandom());

			ItemStack left = new ItemStack(leftEarring.get());
			ItemStack right = new ItemStack(rightEarring.get());
			setPairId(left, pairId);
			setPairId(right, pairId);

			giveOrDrop(player, left);
			giveOrDrop(player, right);
			if (!player.getAbilities().instabuild) pair.shrink(1);
		}
		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.dragonminez.pothala.pair.tooltip.split").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.dragonminez.pothala.pair.tooltip.aura").withStyle(ChatFormatting.GRAY));
		appendPairIdTooltip(stack, tooltip);
		super.appendHoverText(stack, context, display, tooltip, flag);
	}

	/** 26.2: the shared helper now takes the same Consumer the hover-text hook uses. */
	private static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.addItem(stack)) player.drop(stack, false);
	}

	public static int ensurePairId(ItemStack stack, RandomSource random) {
		int pairId = getPairId(stack);
		if (pairId == 0) {
			pairId = random.nextInt();
			if (pairId == 0) pairId = 1;
			setPairId(stack, pairId);
		}
		return pairId;
	}

	public static void setPairId(ItemStack stack, int id) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(PAIR_ID_KEY, id));
	}

	public static int getPairId(ItemStack stack) {
		CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag tag = data.copyTag();
		return tag.contains(PAIR_ID_KEY) ? tag.getIntOr(PAIR_ID_KEY, 0) : 0;
	}
	public static void appendPairIdTooltip(ItemStack stack, java.util.function.Consumer<Component> tooltip) {
		int pairId = getPairId(stack);
		if (pairId != 0) {
			tooltip.accept(Component.translatable("item.dragonminez.pothala.pair_id",
					String.format("%08X", pairId)).withStyle(ChatFormatting.GRAY));
		}
	}
}


