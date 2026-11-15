package com.dragonminez.common.init.item.tools;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;

/**
 * DMZ's tool materials, expressed as 26.2 {@link ToolMaterial} records.
 *
 * <h2>26.2 migration</h2>
 * {@code net.minecraft.world.item.Tier} and NeoForge's {@code SimpleTier} were both removed.
 * Materials are now plain {@link ToolMaterial} records holding
 * {@code (incorrectBlocksForDrops, durability, speed, attackDamageBonus, enchantmentValue, repairItems)}.
 *
 * <p>The old DMZ types carried an {@code Ingredient} repair item; the record only stores a
 * {@code TagKey<Item>}, so {@link #BLANK_WEAPON_TIER} repairs from the empty tag (unrepairable) and
 * the custom tiers name a real item tag.
 *
 * <p>Note the attack-damage semantics also changed: the value passed to
 * {@code Item.Properties#sword(material, damage, speed)} is the <i>baseline</i>, and the material's
 * {@code attackDamageBonus} is added on top. DMZ weapon stats are authored as absolute values, so
 * {@link #BLANK_WEAPON_TIER} keeps its bonus at 0 to preserve the original numbers.
 */
public final class ToolTiers {

    /**
     * The neutral material used by DMZ weapons: durability, damage and speed all come from the
     * caller, so the bonus terms are zeroed and the "incorrect for drops" tag is the wooden one
     * (DMZ weapons are not intended to be mining tools).
     */
    public static final ToolMaterial BLANK_WEAPON_TIER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            0,
            0.0F,
            0.0F,
            15,
            ItemTags.PICKAXES
    );

    /** Gete metal: the high-end DMZ tool material. */
    public static final ToolMaterial GETE_TIER = new ToolMaterial(
            MainTags.Blocks.NEEDS_GETE_TOOL,
            3250,
            12.0F,
            6.0F,
            25,
            ItemTags.PICKAXES
    );

    /**
     * Generic helper for DMZ weapons that used to build a {@code SwordItem} subclass.
     *
     * @param damageBaseline absolute attack damage the weapon should deal
     * @param attackSpeedBaseline attack speed modifier (vanilla swords are -2.4)
     * @param durability how many uses before breaking
     */
    public static Item.Properties swordProperties(float damageBaseline, float attackSpeedBaseline, int durability) {
        return swordProperties(new Item.Properties(), damageBaseline, attackSpeedBaseline, durability);
    }

    /** Applies weapon components without discarding the registry id supplied by registration. */
    public static Item.Properties swordProperties(Item.Properties properties, float damageBaseline, float attackSpeedBaseline, int durability) {
        properties.sword(BLANK_WEAPON_TIER, damageBaseline, attackSpeedBaseline).stacksTo(1);
        // sword() applies the neutral material's durability, so override it afterwards.
        if (durability > 0) return properties.durability(durability);
        // Zero in DMZ's weapon definitions means unlimited durability, not MAX_DAMAGE=0.
        return properties.component(DataComponents.MAX_DAMAGE, null).component(DataComponents.DAMAGE, null);
    }

    private ToolTiers() {
    }
}
