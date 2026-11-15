package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;

public class GravityDeviceBlockModel extends GeoModel<GravityDeviceBlockEntity> {
	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/gravitydevice");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/gravitydevice.png");
	}

	@Override
	public Identifier getAnimationResource(GravityDeviceBlockEntity blockEntity) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/gravitydevice");
	}
}
