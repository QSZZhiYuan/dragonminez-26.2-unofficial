package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.SpacePodEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class SpacePodModel<T extends SpacePodEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/spacepod");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/spacepod.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/spacepod");
    }

}
