package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.client.render.GeoEntityRenderState;
import com.dragonminez.client.init.entities.model.sagas.DBSaibamanModel;
import com.dragonminez.client.init.entities.renderer.sagas.layer.DMZSagaArmorLayer;
import com.dragonminez.client.init.entities.renderer.sagas.layer.DMZSagaItemInHandLayer;
import com.dragonminez.common.init.entities.sagas.SagaSaibamanEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoEntityRenderer;

public class SagaSaibamanRenderer<T extends SagaSaibamanEntity> extends GeoEntityRenderer<T, com.dragonminez.client.render.GeoEntityRenderState> {

    public SagaSaibamanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSaibamanModel<>());
        this.shadowRadius = 0.4f;

        this.withRenderLayer(new DMZSagaItemInHandLayer<T, com.dragonminez.client.render.GeoEntityRenderState>(this));
        this.withRenderLayer(new DMZSagaArmorLayer<T, com.dragonminez.client.render.GeoEntityRenderState>(this, renderManager));
    }

    @Override
    public RenderType getRenderType(com.dragonminez.client.render.GeoEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addBridgedLayer(Object layer) {
        this.renderLayers.addLayer((com.geckolib.renderer.layer.GeoRenderLayer) layer);
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
	public GeoEntityRenderState createRenderState(T entity, Void unused) {
		return new GeoEntityRenderState();
	}
}

