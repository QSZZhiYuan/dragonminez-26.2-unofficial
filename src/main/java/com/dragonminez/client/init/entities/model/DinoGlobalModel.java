package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class DinoGlobalModel<T extends DinoGlobalEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/animal/" + name );
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/animal/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/animal/" + name );
    }
}