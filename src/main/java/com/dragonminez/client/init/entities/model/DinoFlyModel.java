package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoFlyEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class DinoFlyModel extends GeoModel<DinoFlyEntity> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/animal/dino3");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/dino3.png");
    }

    @Override
    public Identifier getAnimationResource(DinoFlyEntity animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/animal/dino3");
    }
}
