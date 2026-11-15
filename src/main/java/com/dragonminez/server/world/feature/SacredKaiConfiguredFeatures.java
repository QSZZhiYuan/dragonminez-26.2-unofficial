package com.dragonminez.server.world.feature;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;

import java.util.ArrayList;
import java.util.List;

public class SacredKaiConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_CLUSTER = key("sacredkai_rock_cluster");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASSY_PEAK = key("sacredkai_grassy_peak");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASSY_CLIFF = key("sacredkai_grassy_cliff");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SAND_PATCH = key("sacredkai_sand_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> WATER_LAKE = key("sacredkai_water_lake");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS_PATCH = key("sacredkai_grass_patch");
	public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWERS = key("sacredkai_flowers");

	public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
		register(context, ROCK_CLUSTER, SacredKaiFeatures.ROCK_CLUSTER.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, GRASSY_PEAK, SacredKaiFeatures.GRASSY_PEAK.get(), NoneFeatureConfiguration.INSTANCE);
		register(context, GRASSY_CLIFF, SacredKaiFeatures.GRASSY_CLIFF.get(), NoneFeatureConfiguration.INSTANCE);

		register(context, SAND_PATCH, SacredKaiFeatures.SAND_PATCH.get(), NoneFeatureConfiguration.INSTANCE);

		// 26.2: LakeFeature.Configuration gained three BlockPredicate slots; ALWAYS_TRUE keeps the
		// previous two-argument behaviour.
		register(context, WATER_LAKE, Feature.LAKE, new LakeFeature.Configuration(
				BlockStateProvider.simple(Blocks.WATER.defaultBlockState()),
				BlockStateProvider.simple(MainBlocks.ROCKY_STONE.get().defaultBlockState()),
				net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.alwaysTrue(),
				net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.alwaysTrue(),
				net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.alwaysTrue()));

		register(context, GRASS_PATCH, Feature.SEQUENCE, patchSequence(32, 7, 3,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						BlockStateProvider.simple(Blocks.SHORT_GRASS)))));

		register(context, FLOWERS, Feature.SEQUENCE, patchSequence(64, 6, 2,
				PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
						new WeightedStateProvider(WeightedList.<BlockState>builder()
								.add(Blocks.DANDELION.defaultBlockState(), 2)
								.add(Blocks.POPPY.defaultBlockState(), 2)
								.add(Blocks.AZURE_BLUET.defaultBlockState(), 1)
								.add(Blocks.OXEYE_DAISY.defaultBlockState(), 1)
								.add(Blocks.CORNFLOWER.defaultBlockState(), 1)
								.build())))));
	}

	/**
	 * Port of the removed {@code RandomPatchConfiguration}: 26.2 expresses a random patch as a
	 * sequence of count + spread placement modifiers around a single placed feature.
	 */
	private static CompositeFeatureConfiguration patchSequence(int tries, int xzSpread, int ySpread, Holder<PlacedFeature> feature) {
		PlacedFeature placed = feature.value();
		List<PlacementModifier> modifiers = new ArrayList<>(placed.placement());
		modifiers.add(0, RandomOffsetPlacement.ofTriangle(xzSpread, ySpread));
		modifiers.add(0, CountPlacement.of(tries));
		return new CompositeFeatureConfiguration(HolderSet.direct(Holder.direct(new PlacedFeature(placed.feature(), List.copyOf(modifiers)))));
	}

	public static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Reference.MOD_ID, name));
	}

	private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
		context.register(key, new ConfiguredFeature<>(feature, configuration));
	}
}

