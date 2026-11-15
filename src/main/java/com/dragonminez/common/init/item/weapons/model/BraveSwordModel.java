package com.dragonminez.common.init.item.weapons.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.weapons.BraveSwordItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class BraveSwordModel extends GeoModel<BraveSwordItem> {
	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapons/brave_sword");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/item/weapons/brave_sword.png");
	}

	@Override
	public Identifier getAnimationResource(BraveSwordItem animatable) {
		return null;
	}
}
