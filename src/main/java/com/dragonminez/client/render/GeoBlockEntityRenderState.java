package com.dragonminez.client.render;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * A block-entity render state that also satisfies GeckoLib's {@link GeoRenderState} bound.
 *
 * <h2>Why this exists</h2>
 * Same generic gap as {@link GeoEntityRenderState}, on the block side: GeckoLib 5 declares
 * {@code adjustModelBonesForRender(RenderPassInfo<R>, BoneSnapshots)} on
 * {@code GeoRenderer<T, O, R extends GeoRenderState>}, while {@code GeoBlockRenderer<T, R extends
 * BlockEntityRenderState>} only bounds {@code R} by {@code BlockEntityRenderState}. At runtime
 * GeckoLib's {@code BlockEntityRenderStateMixin} adds the interface, but javac cannot see it, so a
 * renderer declaring {@code GeoBlockRenderer<T, BlockEntityRenderState>} cannot override the hook.
 *
 * <p>Declaring the render-state type with both bounds makes the override legal:
 * {@code R extends BlockEntityRenderState & GeoRenderState}. This class is that concrete type.
 */
public class GeoBlockEntityRenderState extends BlockEntityRenderState implements GeoRenderState {
}
