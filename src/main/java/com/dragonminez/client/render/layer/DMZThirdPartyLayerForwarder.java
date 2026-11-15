package com.dragonminez.client.render.layer;

import net.minecraft.client.player.AbstractClientPlayer;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Forwards third-party (non-vanilla, non-DMZ) {@code RenderLayer}s onto the DMZ geo model.
 *
 * <h2>Status in 26.2 — temporarily a no-op</h2>
 * The GeckoLib 4 implementation drove vanilla {@code RenderLayer#render(PoseStack,
 * SubmitNodeCollector, int, T, ...)} directly from {@code renderForBone}. In 26.2 both sides of
 * that bridge were removed:
 * <ul>
 *   <li>{@code SubmitNodeCollector} no longer exists — vanilla layers now submit to a
 *       {@code SubmitNodeCollector}</li>
 *   <li>{@code RenderLayer#render} was replaced by {@code submit(..., SubmitNodeCollector, ...)}</li>
 *   <li>{@code GeoRenderLayer} dropped {@code renderForBone} in favour of per-bone
 *       {@code PerBoneRender} tasks</li>
 * </ul>
 * Re-implementing the forwarder requires re-deriving each foreign layer's {@code RenderState}
 * before it can be submitted, which is bespoke per mod. Until that is done this layer registers
 * but draws nothing, so third-party cosmetic layers no longer appear on DMZ models — the rest of
 * the player render path is unaffected.
 *
 * <p>Tracked as a post-M3 task; see HANDOVER §10 for the migration context.
 */
public class DMZThirdPartyLayerForwarder<T extends AbstractClientPlayer & GeoAnimatable>
		extends GeoRenderLayer<T, Void, GeoRenderState> {

	public DMZThirdPartyLayerForwarder(GeoRenderer<T, ?, ?> renderer) {
		super((GeoRenderer) renderer);
	}
}
