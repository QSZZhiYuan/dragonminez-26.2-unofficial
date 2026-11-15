package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.SpacePodModel;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.dragonminez.client.render.GeoEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class SpacePodRenderer extends GeoEntityRenderer<SpacePodEntity, com.dragonminez.client.render.GeoEntityRenderState> {

    public SpacePodRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpacePodModel<>());
    }

    /**
     * 26.2 splits entity rendering into extractRenderState + submit, so the old
     * render(T, ...) override no longer exists. GeckoLib's adjustRenderPose hook is the
     * place to apply the model-space scale/offset that used to live in render().
     */
    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<com.dragonminez.client.render.GeoEntityRenderState> info) {
        super.adjustRenderPose(info);
        info.poseStack().scale(1.1f, 1.1f, 1.1f);
    }

    @Override
    public RenderType getRenderType(com.dragonminez.client.render.GeoEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
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
	public GeoEntityRenderState createRenderState(SpacePodEntity entity, Void unused) {
		return new GeoEntityRenderState();
	}
}

