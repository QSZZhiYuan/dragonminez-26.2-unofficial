package com.dragonminez.common.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registration helpers that keep 26.2's mandatory {@code id} wiring in one place.
 *
 * <h2>Why this exists</h2>
 * In 26.2 {@code BlockBehaviour} and {@code Item} cache their registry id on the object and throw
 * {@code NullPointerException: Block id not set} / {@code Item id not set} the first time anything
 * asks for the description id or the loot table. The id only gets filled in when the properties are
 * built with {@link BlockBehaviour.Properties#setId(ResourceKey)} (resp.
 * {@code Item.Properties#setId}).
 *
 * <p>NeoForge exposes that through the {@code Function<Identifier, T>} register overload. Passing a
 * plain {@code Supplier<T>} bypasses it, because by then the properties object has already been
 * built without an id. Every DMZ block/item goes through the helpers below so that cannot happen.
 */
public final class RegistrationHelper {

    private RegistrationHelper() {}

    /** Registers a block whose properties are derived per-id. */
    public static <B extends Block> DeferredHolder<Block, B> block(DeferredRegister<Block> register,
                                                                 String name,
                                                                 Function<BlockBehaviour.Properties, B> factory,
                                                                 Supplier<BlockBehaviour.Properties> properties) {
        return register.register(name, id -> factory.apply(
                properties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    /** Registers a block using a fixed properties instance, cloned with the id applied. */
    public static <B extends Block> DeferredHolder<Block, B> block(DeferredRegister<Block> register,
                                                                 String name,
                                                                 Function<BlockBehaviour.Properties, B> factory,
                                                                 BlockBehaviour.Properties properties) {
        return register.register(name, id -> factory.apply(
                properties.setId(ResourceKey.create(Registries.BLOCK, id))));
    }

    /** Registers an item whose properties are derived per-id. */
    public static <I extends Item> DeferredHolder<Item, I> item(DeferredRegister<Item> register,
                                                               String name,
                                                               Function<Item.Properties, I> factory,
                                                               Supplier<Item.Properties> properties) {
        return register.register(name, id -> factory.apply(
                properties.get().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    /** Registers an item using a fixed properties instance, cloned with the id applied. */
    public static <I extends Item> DeferredHolder<Item, I> item(DeferredRegister<Item> register,
                                                               String name,
                                                               Function<Item.Properties, I> factory,
                                                               Item.Properties properties) {
        return register.register(name, id -> factory.apply(
                properties.setId(ResourceKey.create(Registries.ITEM, id))));
    }

    /** Registers a {@link BlockItem} for an already-registered block. */
    public static DeferredHolder<Item, BlockItem> blockItem(DeferredRegister<Item> register,
                                                           String name,
                                                           Supplier<? extends Block> block) {
        return register.register(name, id -> new BlockItem(block.get(),
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
