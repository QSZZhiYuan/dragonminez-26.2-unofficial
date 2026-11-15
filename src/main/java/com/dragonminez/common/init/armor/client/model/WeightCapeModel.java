package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.WeightItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class WeightCapeModel extends GeoModel<WeightItem> {
    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armor/weighted_cape");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/weighted_items.png");
    }

    @Override
    public Identifier getAnimationResource(WeightItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armorcape");
    }
}
