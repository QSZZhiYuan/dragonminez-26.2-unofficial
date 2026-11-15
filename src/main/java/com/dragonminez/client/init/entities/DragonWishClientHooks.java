package com.dragonminez.client.init.entities;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class DragonWishClientHooks {
	private DragonWishClientHooks() {}

	public static void openWishMenu(DragonWishEntity entity, Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		if (entity.hasGrantedWish() || minecraft.player == null || minecraft.player != player
				|| !entity.getOwnerName().equals(player.getName().getString())) return;

		var definition = entity.getDragonDefinition();
		if (definition == null) return;

		minecraft.gui.setScreen(new WishesScreen(definition.getWishScreenId(), definition.getWishCount()));
		minecraft.player.playSound(MainSounds.UI_MENU_SWITCH.get());
	}
}
