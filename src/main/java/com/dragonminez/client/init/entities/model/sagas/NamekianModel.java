package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.NamekFrogEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PathfinderMob;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class NamekianModel<T extends PathfinderMob & GeoAnimatable> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/namekian");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/namek_trader_0.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/namekian");
    }
}
