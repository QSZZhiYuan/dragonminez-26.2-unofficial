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
 * 26.2 迁移（方案 A）——结构说明见 NamekDimension，四个维度的处理方式完全一致：
 * <ul>
 *   <li>原 {@code fixedTime}/{@code ultraWarm}/{@code natural}/{@code bedWorks}/{@code respawnAnchorWorks}
 *       这些布尔被 {@code EnvironmentAttributeMap} 取代，DMZ 未自定义，用 {@code EMPTY} 即取默认；</li>
 *   <li>原 {@code effectsLocation}（指向 CustomSpecialEffects）槽位消失，改用 {@code Skybox} 枚举。
 *       DMZ 原先的 effects 只关天空/云/雨，等价于 {@code Skybox.NONE}；</li>
 *   <li>{@code MonsterSettings} 从 4 字段收窄为 {@code (monsterSpawnLightTest, monsterSpawnBlockLightLimit)}。</li>
 * </ul>
 */
public class SacredKaiDimension {
	public static final ResourceKey<Level> SACREDKAI_KEY =
			ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "sacredkaiplanet"));
	public static final ResourceKey<DimensionType> SACREDKAI_TYPE =
			ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "sacredkaiplanet"));

	public static void bootstrap(BootstrapContext<DimensionType> context) {
		var blocks = context.lookup(Registries.BLOCK);
		var timelines = context.lookup(Registries.TIMELINE);

		context.register(SACREDKAI_TYPE, new DimensionType(
				false,                                   // hasFixedTime
				true,                                    // hasSkyLight
				false,                                   // hasCeiling
				false,                                   // hasEnderDragonFight
				1.0,                                   // coordinateScale
				-64,                                   // minY
				384,                                  // height
				384,                                  // logicalHeight
				blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
				0.0F,                                  // ambientLight
				new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
				DimensionType.Skybox.NONE,
				net.minecraft.world.level.CardinalLighting.Type.DEFAULT,
				net.minecraft.world.attribute.EnvironmentAttributeMap.EMPTY,
				timelines.getOrThrow(net.minecraft.tags.TimelineTags.UNIVERSAL),
				Optional.empty()
		));
	}
}