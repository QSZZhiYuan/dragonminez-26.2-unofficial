package com.dragonminez.client.init.entities.model.rr;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.redribbon.RedRibbonEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class RedRibbonRobotModel<T extends RedRibbonEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/robot1");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/robot1");
    }

}
