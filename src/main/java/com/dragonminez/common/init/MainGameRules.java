package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.server.world.dimension.KaioPlanetProtection;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

/**
 * DMZ's custom game rules.
 *
 * <h2>26.2 migration</h2>
 * Game rules became registry entries. The 1.21 shape
 * <pre>{@code
 * GameRules.register("id", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true))
 * }</pre>
 * is gone: {@code GameRules.register} is now private, the nested {@code Key} / {@code BooleanValue}
 * types were replaced by a flat {@link GameRule}{@code <T>}, and rules live in the
 * {@link Registries#GAME_RULE} registry. Reading a value changed from
 * {@code getBoolean(rule)} to {@code get(rule)}.
 *
 * <p>Registration therefore goes through a {@link DeferredRegister} and each rule is built with the
 * same constructor arguments vanilla's private {@code registerBoolean} helper uses.
 */
public class MainGameRules {

	private static final int MASTER_STRUCTURE_MARGIN = 2;

	public static final DeferredRegister<GameRule<?>> GAME_RULES =
			DeferredRegister.create(Registries.GAME_RULE, Reference.MOD_ID);

	/**
	 * Builds a boolean game rule exactly the way vanilla's private {@code registerBoolean} does.
	 * The id comes from the deferred-register key, so it is not passed here.
	 */
	private static GameRule<Boolean> booleanRule(GameRuleCategory category, boolean defaultValue) {
		return new GameRule<>(
				category,
				GameRuleType.BOOL,
				BoolArgumentType.bool(),
				GameRuleTypeVisitor::visitBoolean,
				Codec.BOOL,
				b -> b ? 1 : 0,
				defaultValue,
				FeatureFlagSet.of()
		);
	}

	public static final DeferredHolder<GameRule<?>, GameRule<?>> ALLOW_KI_GRIEFING_MOBS =
			GAME_RULES.register("allow_ki_griefing_mobs",
					() -> booleanRule(GameRuleCategory.PLAYER, true));

	public static final DeferredHolder<GameRule<?>, GameRule<?>> ALLOW_KI_GRIEFING_PLAYERS =
			GAME_RULES.register("allow_ki_griefing_players",
					() -> booleanRule(GameRuleCategory.PLAYER, true));

	public static final DeferredHolder<GameRule<?>, GameRule<?>> ALLOW_KI_GRIEFING_MASTER_STRUCTURES =
			GAME_RULES.register("allow_ki_griefing_master_structures",
					() -> booleanRule(GameRuleCategory.PLAYER, false));

	/**
	 * Reads a boolean rule out of a level.
	 *
	 * <p>The deferred holders are {@code GameRule<?>}; the cast to {@code GameRule<Boolean>} is safe
	 * because {@link #booleanRule} is the only factory used above, and it narrows the wildcard so
	 * {@code GameRules#get(GameRule<T>)} can be called without an unchecked surprise at every call
	 * site.
	 */
	@SuppressWarnings("unchecked")
	private static boolean getBoolean(Level level, DeferredHolder<GameRule<?>, GameRule<?>> rule) {
		// 26.2: Level has no getGameRules() - the rules live on the server level.
		if (!(level instanceof ServerLevel serverLevel)) return false;
		return serverLevel.getGameRules().get((GameRule<Boolean>) rule.get());
	}

	public static boolean canKiGrief(Level level, BlockPos pos, Entity source) {
		if (KaioPlanetProtection.isProtected(level, pos)) return false;
		boolean gameruleAllows;
		if (source instanceof Player || source instanceof ServerPlayer) gameruleAllows = getBoolean(level, ALLOW_KI_GRIEFING_PLAYERS);
		else gameruleAllows = getBoolean(level, ALLOW_KI_GRIEFING_MOBS);
		if (!gameruleAllows) return false;
		if (!getBoolean(level, ALLOW_KI_GRIEFING_MASTER_STRUCTURES) && isInMasterStructure(level, pos)) return false;
		return WorldGuardCompat.canGrief(level, pos, source);
	}

	private static boolean isInMasterStructure(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) return false;
		var registry = serverLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		for (Structure structure : serverLevel.structureManager().getAllStructuresAt(pos).keySet()) {
			Holder<Structure> holder = registry.wrapAsHolder(structure);
			if (!holder.is(MainTags.Structures.KI_GRIEFING_PROTECTED)) continue;
			StructureStart start = serverLevel.structureManager().getStructureAt(pos, structure);
			if (start.isValid() && start.getBoundingBox().inflatedBy(MASTER_STRUCTURE_MARGIN).isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	public static void register(IEventBus modEventBus) {
		GAME_RULES.register(modEventBus);
	}
}
