package com.dragonminez.client.init.entities.model.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.animal.DinoGlobalEntity;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.DataTickets;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
public class SPSkillsModel<T extends SPBlueHurricaneEntity> extends GeoModel<T> {

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/" + name );
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/skills/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/skills/" + name );
    }
}