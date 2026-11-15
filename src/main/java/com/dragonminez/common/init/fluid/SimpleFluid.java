package com.dragonminez.common.init.fluid;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A colourised water-like fluid.
 *
 * <h2>26.2 migration</h2>
 * Two things changed shape here:
 * <ul>
 *   <li>{@code FluidType#initializeClient(Consumer)} was removed. Client fluid extensions are now
 *       registered through NeoForge's {@link RegisterClientExtensionsEvent} (mod bus), which is why
 *       the old anonymous-class factory became {@link #registerClientExtensions}.</li>
 *   <li>{@link IClientFluidTypeExtensions} lost the texture and tint hooks. Fluid appearance now
 *       needs a FluidModel registered through RegisterFluidModelsEvent. Both fog hooks changed:
 *       {@code modifyFogColor} writes into a {@code Vector4f}, and {@code modifyFogRender} takes a
 *       {@code FogData} instead of near/far floats plus a {@code FogShape}.</li>
 * </ul>
 */
public class SimpleFluid extends FluidType {
    private final Identifier stillTexture;
    private final Identifier flowTexture;
    private final Identifier overlayTexture;
    private final int tintColor;
    private final Vector3f fogColor;
    private final int fogStart;
    private final int fogEnd;

    public SimpleFluid(int color, FluidType.Properties properties) {
        super(properties);
        this.stillTexture = Identifier.withDefaultNamespace("block/water_still");
        this.flowTexture = Identifier.withDefaultNamespace("block/water_flow");
        this.overlayTexture = Identifier.withDefaultNamespace("block/water_overlay");

        this.tintColor = toAlpha(color);
        this.fogColor = new Vector3f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
        this.fogStart = -8;
        this.fogEnd = 48;
    }

    private int toAlpha(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return (0xA1 << 24) | (red << 16) | (green << 8) | blue;
    }

    public net.minecraft.client.renderer.block.FluidModel.Unbaked createClientModel() {
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                new net.minecraft.client.resources.model.sprite.Material(stillTexture, true),
                new net.minecraft.client.resources.model.sprite.Material(flowTexture, true),
                new net.minecraft.client.resources.model.sprite.Material(overlayTexture, true),
                net.neoforged.neoforge.client.fluid.FluidTintSources.constant(tintColor));
    }

    /**
     * Registers this fluid's client-side fog behaviour.
     *
     * <p>Call from a mod-bus subscriber:
     * {@code @SubscribeEvent public static void onRegisterClientExtensions(RegisterClientExtensionsEvent e)}.
     */
    public void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                       int renderDistance, float darkenWorldAmount,
                                       org.joml.Vector4f fluidFogColor) {
                fluidFogColor.set(fogColor.x, fogColor.y, fogColor.z, 1.0f);
            }

            @Override
            public void modifyFogRender(Camera camera,
                                        net.minecraft.client.renderer.fog.environment.@Nullable FogEnvironment environment,
                                        float renderDistance, float partialTick,
                                        net.minecraft.client.renderer.fog.FogData fogData) {
                fogData.environmentalStart = fogStart;
                fogData.environmentalEnd = fogEnd;
            }
        }, this);
    }

    @Override
    public boolean canSwim(Entity entity) {
        return true;
    }

    @Override
    public boolean canDrownIn(LivingEntity entity) {
        return !(entity instanceof WaterAnimal);
    }

    @Override
    public @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return PathType.WATER;
    }

    @Override
    public @Nullable PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, PathType originalType) {
        return null;
    }

    @Override
    public boolean supportsBoating(FluidState state, net.minecraft.world.entity.vehicle.boat.AbstractBoat boat) {
        return true;
    }

    @Override
    public boolean canExtinguish(FluidState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
