package com.dragonminez.mixin.client;

import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read access to {@link EntityRenderDispatcher}'s private collaborators.
 *
 * <p>{@code EntityRendererProvider.Context} needs ten arguments; {@code Minecraft} exposes six of
 * them publicly, and the remaining four live here. They are the exact instances vanilla gave to the
 * dispatcher, so reading them back lets DMZ build a complete {@code Context} for its first-person
 * hand renderer.
 *
 * <p>Kept as a standalone interface rather than folded into {@code EntityRenderDispatcherMixin}:
 * a mixin method body cannot cast to another {@code @Mixin} type — the mixin transformer fails with
 * {@code Resolution error: unable to find corresponding type ... in hierarchy of ...}.
 */
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {

    @Accessor("blockModelResolver")
    BlockModelResolver dragonminez$getBlockModelResolver();

    @Accessor("itemModelResolver")
    ItemModelResolver dragonminez$getItemModelResolver();

    @Accessor("equipmentAssets")
    EquipmentAssetManager dragonminez$getEquipmentAssets();

    @Accessor("playerSkinRenderCache")
    PlayerSkinRenderCache dragonminez$getPlayerSkinRenderCache();
}
