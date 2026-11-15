package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.server.world.feature.NamekPlacedFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class NamekBiomes {
	public static final ResourceKey<Biome> AJISSA_PLAINS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "ajissa_plains"));
	public static final ResourceKey<Biome> SACRED_LAND = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "sacred_land"));
	public static final ResourceKey<Biome> NAMEKIAN_RIVERS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "namekian_rivers"));

	public static void bootstrap(BootstrapContext<Biome> context) {
		context.register(AJISSA_PLAINS, ajissaPlains(context));
		context.register(SACRED_LAND, sacredLand(context));
		context.register(NAMEKIAN_RIVERS, namekRiver(context));
	}

    private static void addWaterMobs(MobSpawnSettings.Builder builder) {
        builder.addSpawn(MobCategory.WATER_CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityTypes.SQUID, 1, 4));

        builder.addSpawn(MobCategory.WATER_AMBIENT, 15, new MobSpawnSettings.SpawnerData(EntityTypes.SALMON, 1, 5));
        builder.addSpawn(MobCategory.WATER_AMBIENT, 10, new MobSpawnSettings.SpawnerData(EntityTypes.COD, 1, 5));
        builder.addSpawn(MobCategory.WATER_AMBIENT, 5, new MobSpawnSettings.SpawnerData(EntityTypes.PUFFERFISH, 1, 3));
        builder.addSpawn(MobCategory.WATER_AMBIENT, 8, new MobSpawnSettings.SpawnerData(EntityTypes.TROPICAL_FISH, 1, 4));

        builder.addSpawn(MobCategory.AXOLOTLS, 5, new MobSpawnSettings.SpawnerData(EntityTypes.AXOLOTL, 1, 2));

        builder.addMobCharge(EntityTypes.SQUID, 0.7D, 0.15D);
        builder.addMobCharge(EntityTypes.AXOLOTL, 1.0D, 0.12D);
    }

	private static Biome ajissaPlains(BootstrapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		addWaterMobs(spawnBuilder);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), 0.9, 0.1D);

        biomeBuilder.addFeature(GenerationStep.Decoration.LAKES, NamekPlacedFeatures.NAMEK_LAKE_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_LARGE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_SMALL_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PATCH_GRASS_PLAIN);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PLAINS_FLOWERS);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.AJISSA_TREE_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome sacredLand(BootstrapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		addWaterMobs(spawnBuilder);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER2.get(), 0.9, 0.1D);
        spawnBuilder.addMobCharge(MainEntities.SAGA_FRIEZA_SOLDIER3.get(), 0.9, 0.1D);

		biomeBuilder.addFeature(GenerationStep.Decoration.LAKES, NamekPlacedFeatures.NAMEK_LAKE_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_LAVA_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_LARGE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_SMALL_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_PATCH_SACRED_GRASS_PLAIN);
		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.NAMEK_SACRED_FLOWERS);

		biomeBuilder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, NamekPlacedFeatures.SACRED_TREE_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome namekRiver(BootstrapContext<Biome> context) {
		var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
		var carvers = context.lookup(Registries.CONFIGURED_CARVER);

		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(placedFeatures, carvers);

		addWaterMobs(spawnBuilder);

		biomeBuilder.addFeature(GenerationStep.Decoration.FLUID_SPRINGS, NamekPlacedFeatures.NAMEK_SPRING_WATER_PLACED);

		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COAL_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_COPPER_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_IRON_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GOLD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_REDSTONE_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_EMERALD_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_LAPIS_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_DIAMOND_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_KIKONO_ORE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_LARGE_PLACED);
		biomeBuilder.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, NamekPlacedFeatures.NAMEK_GETE_DEBRIS_SMALL_PLACED);

		return biome(spawnBuilder, biomeBuilder, 6530427);
	}

	private static Biome biome(MobSpawnSettings.Builder spawnBuilder, BiomeGenerationSettings.Builder biomeBuilder, int skyColor) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(2.0f)
				.downfall(0.0f)
				.putAttributes(net.minecraft.world.attribute.EnvironmentAttributeMap.builder()
						.set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, skyColor)
						.set(net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR, 12638463)
						.set(net.minecraft.world.attribute.EnvironmentAttributes.WATER_FOG_COLOR, 329011))
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(0x97ED72)
						.build())
				.mobSpawnSettings(spawnBuilder.build())
				.generationSettings(biomeBuilder.build())
				.build();
	}
}
