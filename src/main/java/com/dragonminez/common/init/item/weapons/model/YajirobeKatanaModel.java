package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.YajirobeKatanaItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class YajirobeKatanaModel extends GeoModel<YajirobeKatanaItem> {
    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/yajirobe_katana");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/yajirobe_katana.png");
    }

    @Override
    public Identifier getAnimationResource(YajirobeKatanaItem animatable) {
        return null;
    }
}
