package com.dragonminez.client.render.layer;

import net.minecraft.client.player.AbstractClientPlayer;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import java.util.List;

/**
 * Renders the DBZ cape (a {@code DbzArmorCapeItem} worn in the chest slot) on the {@code armorBody} bone.
 *
 * <h2>GeckoLib 5 migration</h2>
 * {@code ItemArmorGeoLayer} was rebuilt around {@link ItemArmorGeoLayer.RenderData}: the old
 * {@code getArmorItemForBone / getEquipmentSlotForBone / getModelForItem} overrides are gone and a
 * subclass now declares bone → armor-segment bindings through {@link #getRelevantBones}.
 *
 * <p><b>Known compile issue</b> — {@code getRelevantBones(RenderPassInfo<R>)} cannot be overridden
 * with a concrete vanilla render state here: the method's inherited {@code R} resolves to
 * {@code GeoRenderState}, while {@code ItemArmorGeoLayer} itself is bound to {@code EntityRenderState}.
 * The two bounds are irreconcilable in source (see HANDOVER §10.7), so this layer currently
 * declares the binding through the raw type instead. Functionally the cape still resolves through
 * GeckoLib's own armor pass; only the explicit bone declaration is affected.
 */
public class DMZCapeLayer<T extends AbstractClientPlayer & GeoAnimatable>
        extends ItemArmorGeoLayer<T, Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState> {

    /**
	 * @param geoRenderer the owning renderer
	 * @param context     vanilla's renderer context — must not be null
	 *
	 * <p>Same requirement as {@link DMZPlayerArmorLayer}: GeckoLib's {@code ItemArmorGeoLayer}
	 * constructor reads {@code equipmentRenderer} / {@code equipmentAssets} /
	 * {@code playerSkinRenderCache} / {@code itemModelResolver} off the context immediately, so a
	 * {@code null} here crashes the player render on the first frame.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public DMZCapeLayer(GeoRenderer<T, ?, ?> geoRenderer,
			  net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super((GeoRenderer) geoRenderer, context);
	}

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected List getRelevantBones(RenderPassInfo info) {
        return List.of(RenderData.body("armorBody"));
    }
}
