package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class KikonoStationBlockModel extends GeoModel<KikonoStationBlockEntity> {
	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/kikono_station");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/kikono_station.png");
	}

	@Override
	public Identifier getAnimationResource(KikonoStationBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/kikono_station");
	}
}