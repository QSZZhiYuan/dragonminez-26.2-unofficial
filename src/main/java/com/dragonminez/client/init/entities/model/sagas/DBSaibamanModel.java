package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class DBSaibamanModel<T extends DBSagasEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/sagas/saga_saibaman");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/sagas/saga_saibaman");
    }

}