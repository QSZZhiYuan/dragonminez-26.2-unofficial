package com.dragonminez.server.world.structure.processor;

import com.dragonminez.Reference;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registers DMZ's structure processors.
 *
 * <h2>26.2 migration</h2>
 * The {@code worldgen/structure_processor} registry no longer stores
 * {@code StructureProcessorType<T>} instances — {@code StructureProcessorType} itself is now a plain
 * marker interface holding the shared codecs. The registry value type is
 * {@code MapCodec<? extends StructureProcessor>}:
 *
 * <pre>{@code
 * public static final ResourceKey<Registry<MapCodec<? extends StructureProcessor>>> STRUCTURE_PROCESSOR
 *         = createRegistryKey("worldgen/structure_processor");
 * }</pre>
 *
 * <p>So a processor registers its own {@code MapCodec} under a name instead of wrapping it in a
 * {@code StructureProcessorType} lambda.
 */
public class MainStructureProcessors {

	public static final DeferredRegister<MapCodec<? extends StructureProcessor>> PROCESSORS =
			DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Reference.MOD_ID);

	public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<? extends StructureProcessor>> FOUNDATION =
			PROCESSORS.register("foundation", () -> FoundationProcessor.CODEC);

	public static void register(IEventBus eventBus) {
		PROCESSORS.register(eventBus);
	}
}
