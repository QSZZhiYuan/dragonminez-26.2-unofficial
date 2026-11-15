package com.dragonminez.common.init.item.consumables;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class FoodItem extends Item {
    public FoodItem(int hunger, float saturation, int maxStack) {
        this(hunger, saturation, maxStack, new Item.Properties());
	}

	/** Keep both the registry id and the food components on the registered item. */
	public FoodItem(int hunger, float saturation, int maxStack, Item.Properties properties) {
		super(properties.stacksTo(maxStack).food(
                new FoodProperties.Builder()
                        .nutrition(hunger)
                        .saturationModifier(saturation)
                        .alwaysEdible()
                        .build()
        ));
	}
}

