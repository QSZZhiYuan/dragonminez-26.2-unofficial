package com.dragonminez.server.world.biome;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.*;

public class OtherworldBiomes {
	public static final ResourceKey<Biome> OTHERWORLD = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "other_world"));

	public static void bootstrap(BootstrapContext<Biome> context) {
		context.register(OTHERWORLD, otherworld(context));
	}

	private static Biome otherworld(BootstrapContext<Biome> context) {
		MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

		BiomeGenerationSettings.Builder biomeBuilder = new BiomeGenerationSettings.Builder(context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));

		return new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.downfall(0.0f)
				.temperature(1.0f)
                .putAttributes(net.minecraft.world.attribute.EnvironmentAttributeMap.builder()
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, 0xBE55AA)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR, 0xCE7EBD)
                        .set(net.minecraft.world.attribute.EnvironmentAttributes.WATER_FOG_COLOR, 4214120)
                        .build())
				.generationSettings(biomeBuilder.build())
				.mobSpawnSettings(spawnBuilder.build())
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(4214155)
						.grassColorOverride(0xDCF2FF)
						.foliageColorOverride(0xDCF2FF)
						.build())
				.build();
	}
}

