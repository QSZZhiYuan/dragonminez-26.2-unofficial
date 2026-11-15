package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.render.GeoBlockEntityRenderState;

import com.dragonminez.client.init.blocks.model.GravityDeviceBlockModel;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.geckolib.renderer.GeoBlockRenderer;

public class GravityDeviceBlockRenderer extends GeoBlockRenderer<GravityDeviceBlockEntity, GeoBlockEntityRenderState> {
	public GravityDeviceBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(context, new GravityDeviceBlockModel());
	}

	@Override
	public GeoBlockEntityRenderState createRenderState() {
		return new GeoBlockEntityRenderState();
	}
}

