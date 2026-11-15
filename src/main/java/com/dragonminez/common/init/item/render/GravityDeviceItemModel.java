package com.dragonminez.common.init.item.render;

import com.dragonminez.Reference;
import com.dragonminez.common.init.item.GravityDeviceItem;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class GravityDeviceItemModel extends GeoModel<GravityDeviceItem> {
	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/gravitydevice");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public Identifier getAnimationResource(GravityDeviceItem animatable) {
		return null;
	}
}
