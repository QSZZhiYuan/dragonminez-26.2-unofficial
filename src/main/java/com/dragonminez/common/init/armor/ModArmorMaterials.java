package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * DMZ armor materials.
 *
 * <h2>26.2 migration</h2>
 * {@link ArmorMaterial} is no longer a registry entry in 26.2: {@code Registries.ARMOR_MATERIAL} was
 * removed and the type became a plain record
 * {@code (durability, defense, enchantmentValue, equipSound, toughness, knockbackResistance,
 * repairIngredient, assetId)}. The 1.21 shape differed in more than packaging:
 * <ul>
 *   <li>the nested {@code ArmorMaterial.Layer} type was deleted,</li>
 *   <li>the repair ingredient is now a {@code TagKey<Item>} rather than an {@code Ingredient},</li>
 *   <li>the visual layer set moved out entirely into an {@link EquipmentAsset} identified by a
 *       {@link ResourceKey} ({@code equipment_asset} registry), referenced here via {@code assetId}.</li>
 * </ul>
 * Because there is no registry, the materials are built as plain constants and consumed directly by
 * {@code Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)}.
 *
 * <h2>Assets</h2>
 * {@code assets/dragonminez/equipment/<name>.json} supplies the humanoid layers for these assets.
 * Client item extensions resolve each piece's texture, including its damaged variant.
 */
public final class ModArmorMaterials {

	public static final ResourceKey<EquipmentAsset> DMZ_BASIC_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "basic"));
	public static final ResourceKey<EquipmentAsset> DMZ_KIKONO_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "kikono"));

	private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helm, int body) {
		EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, boots);
		map.put(ArmorType.LEGGINGS, legs);
		map.put(ArmorType.CHESTPLATE, chest);
		map.put(ArmorType.HELMET, helm);
		map.put(ArmorType.BODY, body);
		return map;
	}

	public static final ArmorMaterial BASIC = new ArmorMaterial(
			10,                       // durability multiplier
			defense(1, 2, 3, 1, 3),
			10,                       // enchantment value
			SoundEvents.ARMOR_EQUIP_IRON,
			0.0F,                     // toughness
			0.0F,                     // knockback resistance
			com.dragonminez.common.init.MainTags.Items.WEIGHTED_ITEMS,
			DMZ_BASIC_ASSET
	);

	public static final ArmorMaterial KIKONO = new ArmorMaterial(
			25,
			defense(16, 26, 35, 2, 35),
			25,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			5.0F,
			0.1F,
			com.dragonminez.common.init.MainTags.Items.WEIGHTED_ITEMS,
			DMZ_KIKONO_ASSET
	);

	private ModArmorMaterials() {}
}
