package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DinoFlyModel;
import com.dragonminez.client.init.entities.model.DmzHeadLook;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import com.dragonminez.client.render.GeoEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;

public class DinoFlyRenderer extends GeoEntityRenderer<DinoFlyEntity, com.dragonminez.client.render.GeoEntityRenderState> {

    public DinoFlyRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DinoFlyModel());
        this.shadowRadius = 0.8f;
    }

    /**
     * Stage 1: capture the look angles while the live entity is still reachable.
     *
     * <p>GeckoLib 4 did this in {@code GeoModel#setCustomAnimations}; GeckoLib 5 removed that hook.
     * See {@link DmzHeadLook} for the full rationale.
     */
    @Override
    public void addRenderData(DinoFlyEntity animatable, Void unused, com.dragonminez.client.render.GeoEntityRenderState renderState, float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        renderState.addGeckolibData(DmzHeadLook.HEAD_PITCH, animatable.getXRot());
        renderState.addGeckolibData(DmzHeadLook.HEAD_YAW, animatable.getYHeadRot() - animatable.yBodyRot);
    }

    /** Stage 2: write the captured angles onto the head bone snapshot. */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<com.dragonminez.client.render.GeoEntityRenderState> info, BoneSnapshots bones) {
        super.adjustModelBonesForRender(info, bones);
        DmzHeadLook.apply(bones,
                info.renderState().getGeckolibData(DmzHeadLook.HEAD_PITCH),
                info.renderState().getGeckolibData(DmzHeadLook.HEAD_YAW));
    }

	/**
	 * GeckoLib's {@code GeoEntityRenderer#createRenderState(T, Void)} default implementation builds a
	 * plain vanilla {@code LivingEntityRenderState}. This renderer declares {@code GeoEntityRenderState} as its render
	 * state, and {@code fillRenderState} casts to it, so the default produced
	 * <pre>ClassCastException: LivingEntityRenderState cannot be cast to GeoEntityRenderState</pre>
	 * the first time an entity using it was drawn. The single-argument overload is {@code final} and
	 * delegates here, so overriding this one is enough.
	 */
	@Override
	public GeoEntityRenderState createRenderState(DinoFlyEntity entity, Void unused) {
		return new GeoEntityRenderState();
	}
}
