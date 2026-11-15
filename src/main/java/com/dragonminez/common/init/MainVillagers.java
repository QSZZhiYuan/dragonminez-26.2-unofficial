package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

/**
 * Registro del oficio "Capsule Corp Assistant" y su punto de inters (POI).
 *
 * <p>El bloque de trabajo es el {@code fuel_generator} del mod: cualquier aldeano
 * desempleado que est cerca de uno sin dueño adoptará este oficio. Los intercambios
 * (mapas de estructuras) se añaden en {@code ForgeCommonEvents#onVillagerTrades}.
 */
public final class MainVillagers {

	public static final DeferredRegister<PoiType> POI_TYPES =
			DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Reference.MOD_ID);

	public static final DeferredRegister<VillagerProfession> PROFESSIONS =
			DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Reference.MOD_ID);

	/** POI asociado a todos los blockstates del fuel generator. */
	public static final DeferredHolder<PoiType, PoiType> CAPSULE_CORP_POI = POI_TYPES.register("capsule_corp",
			() -> new PoiType(fuelGeneratorStates(), 1, 1));

	/** El oficio en s. Usa el POI de arriba como sitio de trabajo. */
	public static final DeferredHolder<VillagerProfession, VillagerProfession> CAPSULE_CORP_ASSISTANT =
			PROFESSIONS.register("capsule_corp_assistant",
					() -> new VillagerProfession(
							// 26.2: the first field is the display Component, and trade sets moved
							// onto the profession as an Int2ObjectMap (empty = no level-based trades).
							Component.translatable("entity.dragonminez.capsule_corp_assistant"),
							holder -> holder.is(CAPSULE_CORP_POI.getKey()),
							holder -> holder.is(CAPSULE_CORP_POI.getKey()),
							ImmutableSet.of(),
							ImmutableSet.of(),
							SoundEvents.VILLAGER_WORK_CARTOGRAPHER,
							new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));

	private static Set<BlockState> fuelGeneratorStates() {
		return ImmutableSet.copyOf(MainBlocks.FUEL_GENERATOR.get().getStateDefinition().getPossibleStates());
	}

	public static void register(IEventBus modEventBus) {
		POI_TYPES.register(modEventBus);
		PROFESSIONS.register(modEventBus);
	}

	private MainVillagers() {
	}
}

