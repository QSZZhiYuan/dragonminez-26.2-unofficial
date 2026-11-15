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
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;

import java.util.Optional;

/**
 * 那美克星维度类型。
 *
 * <h2>26.2 迁移（方案 A）</h2>
 * 1.21 的 {@code DimensionType} 是
 * {@code (fixedTime, hasSkylight, hasCeiling, ultraWarm, natural, coordinateScale, bedWorks,
 * respawnAnchorWorks, minY, height, logicalHeight, infiniburn, effectsLocation, ambientLight,
 * monsterSettings)}。
 *
 * <p>26.2 把它重排成 16 个分量，并做了三处结构性删除：
 * <ul>
 *   <li>{@code ultraWarm} / {@code natural} / {@code bedWorks} / {@code respawnAnchorWorks}
 *       这几个布尔**全部移出**，改由 {@link net.minecraft.world.attribute.EnvironmentAttributeMap}
 *       表达（{@code BED_RULE}、{@code RESPAWN_ANCHOR_WORKS}、{@code NETHER_PORTAL_SPAWNS_PIGLINS} 等）；</li>
 *   <li>{@code fixedTime} 变成 {@code hasFixedTime}（布尔），真实时间走 {@code WorldClock}；</li>
 *   <li>{@code effectsLocation}（原指向 {@code CustomSpecialEffects} 的 Identifier）**槽位消失**，
 *       换成 {@code Skybox} 枚举 + {@code CardinalLighting.Type} + 属性表 + {@code timelines}。</li>
 * </ul>
 *
 * <p>The old sky callback returned false to allow vanilla sky rendering. Preserve that sky
 * and move the fixed daytime, green sky and clouds to 26.2 environment attributes.
 */
public class NamekDimension {
	public static final ResourceKey<Level> NAMEK_KEY =
			ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "namek"));
	public static final ResourceKey<DimensionType> NAMEK_TYPE =
			ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "namek"));

	public static void bootstrap(BootstrapContext<DimensionType> context) {
		var blocks = context.lookup(Registries.BLOCK);
		var timelines = context.lookup(Registries.TIMELINE);

		context.register(NAMEK_TYPE, new DimensionType(
				true,                                    // Namek has permanent daylight.
				true,                                    // hasSkyLight
				false,                                   // hasCeiling
				false,                                   // hasEnderDragonFight
				3.0,                                     // coordinateScale
				-64,                                     // minY
				384,                                     // height
				384,                                     // logicalHeight
				blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
				0.0F,                                    // ambientLight
				// 原 (false, false, ConstantInt.of(0), 0) → 26.2 只剩 (光照测试, 方块光上限)
				new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
				DimensionType.Skybox.OVERWORLD,
				net.minecraft.world.level.CardinalLighting.Type.DEFAULT,
				EnvironmentAttributeMap.builder()
						.set(EnvironmentAttributes.SKY_COLOR, 6530427)
						.set(EnvironmentAttributes.FOG_COLOR, 12638463)
						.set(EnvironmentAttributes.CLOUD_COLOR, 0xCCA8EB71)
						.set(EnvironmentAttributes.CLOUD_HEIGHT, 192.0F)
						.set(EnvironmentAttributes.SUN_ANGLE, 22.5F)
						.set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 1.0F)
						.set(EnvironmentAttributes.SKY_LIGHT_LEVEL, 15.0F)
						.build(),
				timelines.getOrThrow(net.minecraft.tags.TimelineTags.UNIVERSAL),
				Optional.empty()                         // defaultClock
		));
	}
}
