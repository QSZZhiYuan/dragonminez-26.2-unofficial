package com.dragonminez.common.wish.wishes;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

public class MultiItemWish extends Wish {
	private final List<Pair<String, Integer>> items;

	public MultiItemWish(String name, String description, List<Pair<String, Integer>> items) {
		super(name, description, "multi_wish");
		this.items = items;
	}

	@Override
	public void grant(ServerPlayer player) {
		for (Pair<String, Integer> itemInfo : items) {
			Item item = BuiltInRegistries.ITEM.get(Identifier.parse(itemInfo.getFirst())).map(net.minecraft.core.Holder::value).orElse(null);
			if (item != null) {
				giveOrDrop(player, new ItemStack(item, itemInfo.getSecond()));
			} else {
				LogUtil.warn(Env.COMMON, "Item with id " + itemInfo.getFirst() + " not found.");
			}
		}
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
	}

	@Override
	public String toJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this, MultiItemWish.class);
	}
}

