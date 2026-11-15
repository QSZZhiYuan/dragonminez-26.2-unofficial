package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;

/**
 * Otherworld keeps the original permanent noon and pink sky/fog using environment attributes.
 */
public class OtherworldDimension {
	public static final ResourceKey<Level> OTHERWORLD_KEY =
			ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));
	public static final ResourceKey<DimensionType> OTHERWORLD_TYPE =
			ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));

	public static void bootstrap(BootstrapContext<DimensionType> context) {
		var blocks = context.lookup(Registries.BLOCK);
		var timelines = context.lookup(Registries.TIMELINE);

		context.register(OTHERWORLD_TYPE, new DimensionType(
				true,                                    // permanent noon
				true,                                    // hasSkyLight
				false,                                   // hasCeiling
				false,                                   // hasEnderDragonFight
				10.0,                                   // coordinateScale
				0,                                   // minY
				320,                                  // height
				320,                                  // logicalHeight
				blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
				0.0F,                                  // ambientLight
				new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
				DimensionType.Skybox.OVERWORLD,
				net.minecraft.world.level.CardinalLighting.Type.DEFAULT,
				net.minecraft.world.attribute.EnvironmentAttributeMap.builder()
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, 0xBE55AA)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR, 0xCE7EBD)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.CLOUD_HEIGHT, 192.0F)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, 0.0F)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_FACTOR, 1.0F)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_LEVEL, 15.0F)
                        .build(),
				timelines.getOrThrow(net.minecraft.tags.TimelineTags.UNIVERSAL),
				Optional.empty()
		));
	}
}
