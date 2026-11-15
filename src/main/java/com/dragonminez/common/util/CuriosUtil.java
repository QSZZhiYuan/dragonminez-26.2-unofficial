package com.dragonminez.common.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public final class CuriosUtil {

	private CuriosUtil() {}

	public static ItemStack getStack(LivingEntity entity, String slotId, int index) {
		if (entity == null) return ItemStack.EMPTY;
		return CuriosApi.getCuriosInventory(entity)
				.map(inv -> inv.findCurio(slotId, index).map(SlotResult::stack).orElse(ItemStack.EMPTY))
				.orElse(ItemStack.EMPTY);
	}

	public static ItemStack getFirstStack(LivingEntity entity, String slotId) {
		return getStack(entity, slotId, 0);
	}

	public static ItemStack getFirstStackForItem(LivingEntity entity, String slotId, String itemName) {
		if (entity == null) return ItemStack.EMPTY;
		return CuriosApi.getCuriosInventory(entity).map(inventory -> {
			var handler = inventory.getCurios().get(slotId);
			if (handler == null) return ItemStack.EMPTY;
			for (int index = 0; index < handler.getStacks().getSlots(); index++) {
				ItemStack stack = handler.getStacks().getStackInSlot(index);
				if (!stack.isEmpty() && stack.getItem().getDescriptionId().contains(itemName)) return stack;
			}
			return ItemStack.EMPTY;
		}).orElse(ItemStack.EMPTY);
	}
}

