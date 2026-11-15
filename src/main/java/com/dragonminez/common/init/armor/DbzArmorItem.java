package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import net.minecraft.core.Holder;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Custom DBZ armor piece.
 *
 * <p>26.2 note: vanilla {@code ArmorItem} was removed together with the
 * {@code getArmorTexture} hook. Armor behaviour now lives entirely in the
 * {@code EQUIPPABLE} data component, which {@code Item.Properties.humanoidArmor}
 * installs. Armor layer textures are resolved through the {@link
 * net.minecraft.world.item.equipment.EquipmentAsset} referenced by that
 * component rather than per-item code.
 *
 * <p>Client model registration is handled by {@code DMZClientItemExtensions}.
 */
public class DbzArmorItem extends Item implements DbzArmorTextured {
    private final String modId;
    private final String itemId;

    public DbzArmorItem(ArmorMaterial pMaterial, ArmorType pType, Properties pProperties, String itemId) {
        this(pMaterial, pType, pProperties, Reference.MOD_ID, itemId);
    }

    public DbzArmorItem(ArmorMaterial pMaterial, ArmorType pType, Properties pProperties, String modId, String itemId) {
        super(pProperties.humanoidArmor(pMaterial, pType));
        this.modId = modId;
        this.itemId = itemId;
    }

    @java.lang.SuppressWarnings("all")
    public String getModId() {
        return this.modId;
    }

    @java.lang.SuppressWarnings("all")
    public String getItemId() {
        return this.itemId;
    }
}
