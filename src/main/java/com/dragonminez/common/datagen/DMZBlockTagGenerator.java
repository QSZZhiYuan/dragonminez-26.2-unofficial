package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DMZBlockTagGenerator extends TagsProvider<Block> {
	public DMZBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, Registries.BLOCK, lookupProvider);
	}

	private static ResourceKey<Block> key(Block block) {
		return block.builtInRegistryHolder().key();
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider pProvider) {
		this.tag(BlockTags.FROG_PREFER_JUMP_TO)
				.add(key(MainBlocks.LOTUS_FLOWER.get()));
		this.tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS)
				.add(key(MainBlocks.LOTUS_FLOWER.get()));
		this.tag(BlockTags.CLIMBABLE)
				.add(key(MainBlocks.INVISIBLE_LADDER_BLOCK.get()));

		//Tags para los Bloques
		this.tag(BlockTags.NEEDS_STONE_TOOL)
				.add(key(MainBlocks.NAMEK_IRON_ORE.get()))
				.add(key(MainBlocks.NAMEK_COPPER_ORE.get()))
				.add(key(MainBlocks.NAMEK_LAPIS_ORE.get()))
				.add(key(MainBlocks.NAMEK_COAL_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_IRON.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COPPER.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COAL.get()));

		this.tag(BlockTags.NEEDS_IRON_TOOL)
				.add(key(MainBlocks.NAMEK_DIAMOND_ORE.get()))
				.add(key(MainBlocks.NAMEK_GOLD_ORE.get()))
				.add(key(MainBlocks.NAMEK_REDSTONE_ORE.get()))
				.add(key(MainBlocks.NAMEK_EMERALD_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_GOLD.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get()))
				.add(key(MainBlocks.FUEL_GENERATOR.get()));

		this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
				.add(key(MainBlocks.NAMEK_KIKONO_ORE.get()))
				.add(key(MainBlocks.KIKONO_BLOCK.get()))
				.add(key(MainBlocks.KIKONO_STATION.get()))
				.add(key(MainBlocks.GRAVITY_DEVICE.get()));

		this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL)
				.add(key(MainBlocks.GETE_ORE.get()))
				.add(key(MainBlocks.GETE_BLOCK.get()));

		//this.tag(MainTags.Blocks.NEEDS_GETE_TOOL)
		//		.add(key(MainBlocks.INVISIBLE_LADDER_BLOCK.get()));

		this.tag(BlockTags.MINEABLE_WITH_AXE)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_WOOD.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_PLANKS.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_DOOR.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_SLAB.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_FENCE.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_FENCE_GATE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get()))
				.add(key(MainBlocks.NAMEK_SACRED_WOOD.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get()))
				.add(key(MainBlocks.NAMEK_SACRED_PLANKS.get()))
				.add(key(MainBlocks.NAMEK_SACRED_DOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_TRAPDOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_SLAB.get()))
				.add(key(MainBlocks.NAMEK_SACRED_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_SACRED_FENCE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_FENCE_GATE.get()));

		this.tag(BlockTags.MINEABLE_WITH_HOE)
				.add(key(MainBlocks.NAMEK_AJISSA_LEAVES.get()))
				.add(key(MainBlocks.NAMEK_SACRED_LEAVES.get()));

		this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.add(key(MainBlocks.NAMEK_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_STONE.get()))
				.add(key(MainBlocks.NAMEK_STONE_SLAB.get()))
				.add(key(MainBlocks.NAMEK_STONE_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_STONE_WALL.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE_SLAB.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_SLAB.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_WALL.get()))
				.add(key(MainBlocks.ROCKY_STONE.get()))
				.add(key(MainBlocks.ROCKY_STONE_SLAB.get()))
				.add(key(MainBlocks.ROCKY_STONE_STAIRS.get()))
				.add(key(MainBlocks.ROCKY_STONE_WALL.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_SLAB.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_STAIRS.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_WALL.get()))
				.add(key(MainBlocks.NAMEK_COAL_ORE.get()))
				.add(key(MainBlocks.NAMEK_IRON_ORE.get()))
				.add(key(MainBlocks.NAMEK_COPPER_ORE.get()))
				.add(key(MainBlocks.NAMEK_LAPIS_ORE.get()))
				.add(key(MainBlocks.NAMEK_GOLD_ORE.get()))
				.add(key(MainBlocks.NAMEK_DIAMOND_ORE.get()))
				.add(key(MainBlocks.NAMEK_REDSTONE_ORE.get()))
				.add(key(MainBlocks.NAMEK_EMERALD_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COAL.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_IRON.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COPPER.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_GOLD.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get()))
				.add(key(MainBlocks.GETE_ORE.get()))
				.add(key(MainBlocks.GETE_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_KIKONO_ORE.get()))
				.add(key(MainBlocks.KIKONO_BLOCK.get()))
				.add(key(MainBlocks.KIKONO_STATION.get()))
				.add(key(MainBlocks.FUEL_GENERATOR.get()))
				.add(key(MainBlocks.GRAVITY_DEVICE.get()));


		for (DragonBallSetDefinition setDefinition : DragonBallDefinitions.getBallSets()) {
			for (DeferredHolder<Block, ? extends Block> block : MainBlocks.getDragonBallBlocks(setDefinition.getId()).values()) {
				this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(key(block.get()));
			}
		}

		this.tag(BlockTags.MINEABLE_WITH_SHOVEL)
				.add(key(MainBlocks.NAMEK_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_DIRT.get()))
				.add(key(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get()))
				.add(key(MainBlocks.ROCKY_DIRT.get()));

		this.tag(BlockTags.LOGS)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get()));

		this.tag(BlockItemTags.LOGS_THAT_BURN.block())
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get()));

		this.tag(BlockTags.PLANKS)
				.add(key(MainBlocks.NAMEK_AJISSA_PLANKS.get()))
				.add(key(MainBlocks.NAMEK_SACRED_PLANKS.get()));

		this.tag(BlockTags.WOODEN_BUTTONS)
				.add(key(MainBlocks.NAMEK_AJISSA_BUTTON.get()))
				.add(key(MainBlocks.NAMEK_SACRED_BUTTON.get()));

		this.tag(BlockTags.BUTTONS)
				.add(key(MainBlocks.NAMEK_AJISSA_BUTTON.get()))
				.add(key(MainBlocks.NAMEK_SACRED_BUTTON.get()));

		this.tag(BlockTags.LEAVES)
				.add(key(MainBlocks.NAMEK_AJISSA_LEAVES.get()))
				.add(key(MainBlocks.NAMEK_SACRED_LEAVES.get()));

		this.tag(BlockTags.WOODEN_DOORS)
				.add(key(MainBlocks.NAMEK_AJISSA_DOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_DOOR.get()));

		this.tag(BlockTags.DOORS)
				.add(key(MainBlocks.NAMEK_AJISSA_DOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_DOOR.get()));

		this.tag(BlockTags.WOODEN_SLABS)
				.add(key(MainBlocks.NAMEK_AJISSA_SLAB.get()))
				.add(key(MainBlocks.NAMEK_SACRED_SLAB.get()));

		this.tag(BlockTags.SLABS)
				.add(key(MainBlocks.NAMEK_AJISSA_SLAB.get()))
				.add(key(MainBlocks.NAMEK_SACRED_SLAB.get()))
				.add(key(MainBlocks.NAMEK_STONE_SLAB.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE_SLAB.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_SLAB.get()))
				.add(key(MainBlocks.ROCKY_STONE_SLAB.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_SLAB.get()));

		this.tag(BlockTags.WOODEN_STAIRS)
				.add(key(MainBlocks.NAMEK_AJISSA_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_SACRED_STAIRS.get()));

		this.tag(BlockTags.STAIRS)
				.add(key(MainBlocks.NAMEK_AJISSA_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_SACRED_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_STONE_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE_STAIRS.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_STAIRS.get()))
				.add(key(MainBlocks.ROCKY_STONE_STAIRS.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_STAIRS.get()));

		this.tag(BlockTags.WOODEN_TRAPDOORS)
				.add(key(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_TRAPDOOR.get()));

		this.tag(BlockTags.TRAPDOORS)
				.add(key(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get()))
				.add(key(MainBlocks.NAMEK_SACRED_TRAPDOOR.get()));

		this.tag(BlockTags.WOODEN_FENCES)
				.add(key(MainBlocks.NAMEK_AJISSA_FENCE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_FENCE.get()));

		this.tag(BlockTags.FENCES)
				.add(key(MainBlocks.NAMEK_AJISSA_FENCE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_FENCE.get()));

		this.tag(BlockTags.WOODEN_PRESSURE_PLATES)
				.add(key(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get()));

		this.tag(BlockTags.PRESSURE_PLATES)
				.add(key(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get()))
				.add(key(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get()));

		this.tag(BlockTags.WALLS)
				.add(key(MainBlocks.NAMEK_STONE_WALL.get()))
				.add(key(MainBlocks.NAMEK_COBBLESTONE_WALL.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_WALL.get()))
				.add(key(MainBlocks.ROCKY_STONE_WALL.get()))
				.add(key(MainBlocks.ROCKY_COBBLESTONE_WALL.get()));

		this.tag(BlockTags.ANIMALS_SPAWNABLE_ON)
				.add(key(MainBlocks.NAMEK_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_DIRT.get()))
				.add(key(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get()))
				.add(key(MainBlocks.ROCKY_DIRT.get()));

		this.tag(BlockTags.FROGS_SPAWNABLE_ON)
				.add(key(MainBlocks.NAMEK_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get()))
				.add(key(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get()));

		this.tag(BlockTags.BEACON_BASE_BLOCKS)
				.add(key(MainBlocks.GETE_BLOCK.get()))
				.add(key(MainBlocks.KIKONO_BLOCK.get()));

		this.tag(BlockTags.DIRT)
				.add(key(MainBlocks.NAMEK_DIRT.get()))
				.add(key(MainBlocks.NAMEK_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get()))
				.add(key(MainBlocks.ROCKY_DIRT.get()))
				.add(key(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get()));

		this.tag(BlockItemTags.COAL_ORES.block())
				.add(key(MainBlocks.NAMEK_COAL_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COAL.get()));

		this.tag(BlockTags.IRON_ORES)
				.add(key(MainBlocks.NAMEK_IRON_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_IRON.get()));

		this.tag(BlockTags.COPPER_ORES)
				.add(key(MainBlocks.NAMEK_COPPER_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_COPPER.get()));

		this.tag(BlockItemTags.LAPIS_ORES.block())
				.add(key(MainBlocks.NAMEK_LAPIS_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get()));

		this.tag(BlockTags.GOLD_ORES)
				.add(key(MainBlocks.NAMEK_GOLD_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_GOLD.get()));

		this.tag(BlockItemTags.DIAMOND_ORES.block())
				.add(key(MainBlocks.NAMEK_DIAMOND_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get()));

		this.tag(BlockItemTags.REDSTONE_ORES.block())
				.add(key(MainBlocks.NAMEK_REDSTONE_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get()));

		this.tag(BlockItemTags.EMERALD_ORES.block())
				.add(key(MainBlocks.NAMEK_EMERALD_ORE.get()))
				.add(key(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get()));

		this.tag(BlockTags.FLOWERS)
				.add(key(MainBlocks.CHRYSANTHEMUM_FLOWER.get()))
				.add(key(MainBlocks.MARIGOLD_FLOWER.get()))
				.add(key(MainBlocks.AMARYLLIS_FLOWER.get()))
				.add(key(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get()))
				.add(key(MainBlocks.TRILLIUM_FLOWER.get()))
				.add(key(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get()))
				.add(key(MainBlocks.SACRED_MARIGOLD_FLOWER.get()))
				.add(key(MainBlocks.SACRED_AMARYLLIS_FLOWER.get()))
				.add(key(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get()))
				.add(key(MainBlocks.SACRED_TRILLIUM_FLOWER.get()));

		this.tag(BlockTags.FLOWER_POTS)
				.add(key(MainBlocks.POTTED_CHRYSANTHEMUM_FLOWER.get()))
				.add(key(MainBlocks.POTTED_MARIGOLD_FLOWER.get()))
				.add(key(MainBlocks.POTTED_AMARYLLIS_FLOWER.get()))
				.add(key(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get()))
				.add(key(MainBlocks.POTTED_TRILLIUM_FLOWER.get()))
				.add(key(MainBlocks.POTTED_SACRED_CHRYSANTHEMUM_FLOWER.get()))
				.add(key(MainBlocks.POTTED_SACRED_MARIGOLD_FLOWER.get()))
				.add(key(MainBlocks.POTTED_AMARYLLIS_FLOWER.get()))
				.add(key(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get()))
				.add(key(MainBlocks.POTTED_TRILLIUM_FLOWER.get()));

		this.tag(BlockItemTags.SAPLINGS.block())
				.add(key(MainBlocks.NAMEK_SACRED_SAPLING.get()))
				.add(key(MainBlocks.NAMEK_AJISSA_SAPLING.get()));

		this.tag(BlockTags.VALID_SPAWN)
				.add(key(MainBlocks.NAMEK_DIRT.get()))
				.add(key(MainBlocks.NAMEK_GRASS_BLOCK.get()))
				.add(key(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get()))
				.add(key(MainBlocks.ROCKY_DIRT.get()))
				.add(key(MainBlocks.SACRED_PLANET_GRASS_BLOCK.get()))
				.add(key(MainBlocks.ROCKY_STONE.get()));

		this.tag(MainTags.Blocks.NAMEK_ALOG)
				.add(key(MainBlocks.NAMEK_AJISSA_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get()));

		this.tag(MainTags.Blocks.NAMEK_SLOG)
				.add(key(MainBlocks.NAMEK_SACRED_LOG.get()))
				.add(key(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get()));

		this.tag(MainTags.Blocks.NAMEKSTONE_REPLACEABLES)
				.replace(false)
				.add(key(MainBlocks.NAMEK_STONE.get()));

		this.tag(MainTags.Blocks.NAMEKDEEPSLATE_REPLACEABLES)
				.replace(false)
				.add(key(MainBlocks.NAMEK_DEEPSLATE.get()));
	}
}

