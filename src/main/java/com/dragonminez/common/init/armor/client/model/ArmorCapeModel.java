package com.dragonminez.common.init.armor.client.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class ArmorCapeModel extends GeoModel<DbzArmorCapeItem> {
    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armor/armorcape");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/armor/blank.png");
    }

    @Override
    public Identifier getAnimationResource(DbzArmorCapeItem animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "armorcape");
    }
}
