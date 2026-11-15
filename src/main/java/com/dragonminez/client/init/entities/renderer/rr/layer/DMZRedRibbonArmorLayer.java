package com.dragonminez.client.init.entities.renderer.rr.layer;

import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemArmorGeoLayer;

import java.util.List;

/**
 * Renders worn armour on Red Ribbon soldiers by mapping the model's armour bones onto equipment slots.
 *
 * <h2>GeckoLib 5 migration</h2>
 * 1.20.1 expressed this by overriding three per-bone hooks:
 * {@code getArmorItemForBone}, {@code getEquipmentSlotForBone} and {@code getModelPartForBone}.
 * GeckoLib 5 replaced all of them with a single abstract method,
 * {@link ItemArmorGeoLayer#getRelevantBones(RenderPassInfo)}, which returns the bones that carry
 * armour together with the segment each one represents. The base class then resolves the equipped
 * stack, the model part and the render type itself.
 *
 * <p>The constructor also gained an {@link EntityRendererProvider.Context} argument, which the layer
 * needs for the vanilla equipment renderer and skin cache.
 */
public class DMZRedRibbonArmorLayer<T extends RedRibbonEntity, R extends net.minecraft.client.renderer.entity.state.EntityRenderState & com.geckolib.renderer.base.GeoRenderState>
        extends ItemArmorGeoLayer<T, Void, R> {

    public DMZRedRibbonArmorLayer(GeoRenderer<T, Void, R> geoRenderer,
                                  EntityRendererProvider.Context context) {
        super(geoRenderer, context);
    }

    @Override
    protected List<RenderData> getRelevantBones(RenderPassInfo<R> info) {
        return List.of(
                RenderData.head("armorHead"),
                RenderData.body("armorBody"),
                RenderData.rightArm("armorRightArm"),
                RenderData.leftArm("armorLeftArm"),
                RenderData.rightLeg("armorRightLeg"),
                RenderData.leftLeg("armorLeftLeg"),
                RenderData.rightFoot("armorRightBoot"),
                RenderData.leftFoot("armorLeftBoot")).stream()
                .filter(binding -> info.model().getBone(binding.boneName()).isPresent()).toList();
    }
}
