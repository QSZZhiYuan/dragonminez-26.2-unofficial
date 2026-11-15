package com.dragonminez.server.world.gen;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.server.world.biome.OverworldBiomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class OverworldSurfaceRules {
	private static final SurfaceRules.RuleSource ROCKY_DIRT = SurfaceRules.state(MainBlocks.ROCKY_DIRT.get().defaultBlockState());
	private static final SurfaceRules.RuleSource ROCKY_STONE = SurfaceRules.state(MainBlocks.ROCKY_STONE.get().defaultBlockState());

	private static final SurfaceRules.RuleSource BEDROCK = SurfaceRules.state(Blocks.BEDROCK.defaultBlockState());

	public static SurfaceRules.RuleSource makeRules(net.minecraft.core.HolderGetter<net.minecraft.world.level.biome.Biome> biomes) {
		// 26.2: SurfaceRules#isBiome takes the biome HolderGetter first, so the condition is built
		// from the registry the rules are applied against.
		SurfaceRules.ConditionSource isRockyWasteland = SurfaceRules.isBiome(biomes, OverworldBiomes.ROCKY);

		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
				SurfaceRules.ifTrue(isRockyWasteland, SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(50), 1),
						SurfaceRules.sequence(
								SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, ROCKY_DIRT),
								SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ROCKY_STONE),
								SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, ROCKY_STONE)
						))
				)
		);
	}
}

