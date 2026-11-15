package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DMZItemTagGenerator extends TagsProvider<Item> {
	public DMZItemTagGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
		super(pOutput, Registries.ITEM, pLookupProvider);
	}

	private static ResourceKey<Item> key(Item item) {
		return item.builtInRegistryHolder().key();
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider pProvider) {
		this.tag(ItemTags.LOGS)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_AJISSA_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get().asItem()));

		this.tag(ItemTags.LOGS_THAT_BURN)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_AJISSA_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_WOOD.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get().asItem()));

		this.tag(Tags.Items.TOOLS)
				.add(key(MainItems.ARMOR_CRAFTING_KIT.get()));

		this.tag(Tags.Items.INGOTS)
				.add(key(MainItems.GETE_SCRAP.get()))
				.add(key(MainItems.GETE_INGOT.get()))
				.add(key(MainItems.KIKONO_SHARD.get()))
				.add(key(MainItems.KIKONO_STRING.get()))
				.add(key(MainItems.KIKONO_CLOTH.get()));

		this.tag(ItemTags.PLANKS)
				.add(key(MainBlocks.NAMEK_AJISSA_PLANKS.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_PLANKS.get().asItem()));

		this.tag(ItemTags.LEAVES)
				.add(key(MainBlocks.NAMEK_AJISSA_LEAVES.get().asItem()))
				.add(key(MainBlocks.NAMEK_SACRED_LEAVES.get().asItem()));

		this.tag(ItemTags.STONE_CRAFTING_MATERIALS)
				.add(key(MainBlocks.NAMEK_STONE.get().asItem()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE.get().asItem()));

		this.tag(ItemTags.STONE_TOOL_MATERIALS)
				.add(key(MainBlocks.NAMEK_STONE.get().asItem()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE.get().asItem()));

		this.tag(ItemTags.COAL_ORES)
				.add(key(MainBlocks.NAMEK_COAL_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COAL.get().asItem()));

		this.tag(ItemTags.IRON_ORES)
				.add(key(MainBlocks.NAMEK_IRON_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_IRON.get().asItem()));

		this.tag(ItemTags.GOLD_ORES)
				.add(key(MainBlocks.NAMEK_GOLD_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_GOLD.get().asItem()));

		this.tag(ItemTags.REDSTONE_ORES)
				.add(key(MainBlocks.NAMEK_REDSTONE_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get().asItem()));

		this.tag(ItemTags.LAPIS_ORES)
				.add(key(MainBlocks.NAMEK_LAPIS_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get().asItem()));

		this.tag(ItemTags.DIAMOND_ORES)
				.add(key(MainBlocks.NAMEK_DIAMOND_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get().asItem()));

		this.tag(ItemTags.EMERALD_ORES)
				.add(key(MainBlocks.NAMEK_EMERALD_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get().asItem()));

		this.tag(ItemTags.COPPER_ORES)
				.add(key(MainBlocks.NAMEK_COPPER_ORE.get().asItem()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COPPER.get().asItem()));

		this.tag(net.minecraft.tags.BlockItemTags.FLOWERS.item())
				.add(key(MainBlocks.CHRYSANTHEMUM_FLOWER.get().asItem()))
				.add(key(MainBlocks.MARIGOLD_FLOWER.get().asItem()))
				.add(key(MainBlocks.AMARYLLIS_FLOWER.get().asItem()))
				.add(key(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get().asItem()))
				.add(key(MainBlocks.TRILLIUM_FLOWER.get().asItem()))
				.add(key(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get().asItem()))
				.add(key(MainBlocks.SACRED_MARIGOLD_FLOWER.get().asItem()))
				.add(key(MainBlocks.SACRED_AMARYLLIS_FLOWER.get().asItem()))
				.add(key(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get().asItem()))
				.add(key(MainBlocks.SACRED_TRILLIUM_FLOWER.get().asItem()));

		this.tag(MainTags.Items.NAMEK_ALOG)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get().asItem()));

		this.tag(MainTags.Items.NAMEK_SLOG)
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get().asItem()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get().asItem()));

		this.tag(MainTags.Items.WEIGHTED_ITEMS)
				.add(key(MainItems.WEIGHT_TURTLE_SHELL.get()))
				.add(key(MainItems.WORKOUT_WEIGHTS.get()))
				.add(key(MainItems.WEIGHT_PICCOLO_CAPE.get()));

//		this.tag(ItemTags.SWORDS)
//				.add(key(MainItems.BACULO_SAGRADO.get()))
//				.add(key(MainItems.TRUNKS_SWORD.get()))
//				.add(key(MainItems.Z_SWORD.get()))
//				.add(key(MainItems.KATANA_YAJIROBE.get()));
	}
}

