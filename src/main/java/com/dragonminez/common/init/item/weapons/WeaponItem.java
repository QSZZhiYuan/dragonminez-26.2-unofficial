package com.dragonminez.common.init.item.weapons;

import net.minecraft.world.item.component.TooltipDisplay;
import com.dragonminez.common.init.item.tools.ToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base class for DMZ's GeckoLib-animated weapons.
 *
 * <h2>26.2 migration</h2>
 * {@code SwordItem} was deleted in 26.2. Swords are no longer a class: vanilla builds them with
 * {@code new Item(new Item.Properties().sword(material, damage, speed))}, and the sword behaviour
 * (cobweb cutting, sweep, sweep particle, {@code WEAPON} component) lives on the
 * {@link ToolTiers#BLANK_WEAPON_TIER material} + properties rather than on a subclass.
 *
 * <p>This class therefore extends plain {@link Item} and applies the sword properties in its
 * constructor. All DMZ-specific behaviour (translated name, tooltip, enchantability) is unchanged.
 */
public class WeaponItem extends Item {
	private final String tag;
	private final int enchantability;

	/**
	 * @param damageBase absolute attack damage the weapon deals
	 * @param attackSpeed attack speed modifier (vanilla swords use -2.4F)
	 * @param durability uses before the weapon breaks
	 * @param enchantability enchantment value
	 * @param tag translation-key suffix under {@code item.dragonminez.}
	 */
	public WeaponItem(int damageBase, float attackSpeed, int durability, int enchantability, String tag) {
		this(damageBase, attackSpeed, durability, enchantability, tag,
				new Item.Properties());
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public WeaponItem(int damageBase, float attackSpeed, int durability, int enchantability, String tag,
			net.minecraft.world.item.Item.Properties properties) {
		super(ToolTiers.swordProperties(properties, damageBase, attackSpeed, durability)
				.fireResistant().enchantable(enchantability));
		this.tag = tag;
		this.enchantability = enchantability;
	}

	// 26.2: Item#getEnchantmentValue() and Item#isEnchantable(ItemStack) are gone - both are
	// driven by the ENCHANTABLE / enchantment data components on the item stack now.

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		return Component.translatable("item.dragonminez." + this.tag);
	}

	@Override
	public void appendHoverText(ItemStack pStack, Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
		pTooltipComponents.accept(Component.translatable("item.dragonminez." + this.tag + ".tooltip").withStyle(ChatFormatting.GRAY));
	}
}
