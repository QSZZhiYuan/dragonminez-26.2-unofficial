package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
import com.dragonminez.common.init.entities.FlyingNimbusEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class BlackNimbusModel<T extends BlackNimbusEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/kinton");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/black_kinton.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/kinton");
    }

}
