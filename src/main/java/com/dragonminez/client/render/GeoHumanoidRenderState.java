package com.dragonminez.client.render;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * A humanoid render state that also satisfies GeckoLib's {@link GeoRenderState} bound.
 *
 * <h2>Why this exists</h2>
 * Same generic gap as {@link GeoEntityRenderState}, for the armor path: GeckoLib 5 declares
 * {@code GeoArmorRenderer<T, R extends HumanoidRenderState>} (because it needs the base model), while
 * {@code GeoRenderer<T, O, R extends GeoRenderState>} requires {@code R} to be a {@code GeoRenderState}.
 * A renderer declaring {@code GeoArmorRenderer<Item, HumanoidRenderState>} therefore cannot override
 * any hook that mentions {@code RenderPassInfo<R>} or the geckolib data accessors.
 *
 * <p>At runtime GeckoLib's {@code EntityRenderStateMixin} makes every entity render state implement
 * {@code GeoRenderState}; declaring both bounds makes that visible to javac.
 */
public class GeoHumanoidRenderState extends HumanoidRenderState implements GeoRenderState {
}
