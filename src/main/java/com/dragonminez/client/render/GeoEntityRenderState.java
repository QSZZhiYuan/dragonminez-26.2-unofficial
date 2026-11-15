package com.dragonminez.client.render;

import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * A living-entity render state that also satisfies GeckoLib's {@link GeoRenderState} bound.
 *
 * <h2>Why this exists</h2>
 * GeckoLib 5 declares {@code adjustRenderPose(RenderPassInfo<R>)} on
 * {@code GeoRenderer<T, O, R extends GeoRenderState>}, while {@code GeoEntityRenderer<T, R extends
 * EntityRenderState>} only bounds {@code R} by {@code EntityRenderState}. At runtime
 * {@code EntityRenderState} does implement {@code GeoRenderState} — GeckoLib injects it through
 * {@code EntityRenderStateMixin} — but that relationship is invisible to javac, so a renderer that
 * declares {@code GeoEntityRenderer<T, LivingEntityRenderState>} cannot override
 * {@code adjustRenderPose} at all:
 *
 * <pre>{@code
 * // both rejected:
 * adjustRenderPose(RenderPassInfo<LivingEntityRenderState>)  // LivingEntityRenderState is not a GeoRenderState
 * adjustRenderPose(RenderPassInfo<GeoRenderState>)           // name clash: same erasure, neither overrides
 * }</pre>
 *
 * <p>Declaring the render-state type with <em>both</em> bounds makes the override legal:
 * {@code R extends LivingEntityRenderState & GeoRenderState}. This class is that concrete type, for
 * renderers that are not generic over {@code R} themselves.
 */
public class GeoEntityRenderState extends LivingEntityRenderState implements GeoRenderState {
    // Keep extraction and submission paired even if the race/cache changes later in this frame.
    public DMZPlayerRenderer<?> dmzPlayerRenderer;
    public boolean firstPersonBody;
    public org.joml.Vector3f firstPersonOffset;
    public boolean fastFlight;
    public float flightPitch;
    public float flightRoll;
    public float flightPivotY;
    public com.dragonminez.client.systems.kishare.KiSharePose.ArmPose kiSharePose;
}
