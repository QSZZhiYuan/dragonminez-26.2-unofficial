package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.render.GeoBlockEntityRenderState;

import com.dragonminez.client.init.blocks.model.FuelGeneratorBlockModel;
import com.dragonminez.common.init.block.entity.FuelGeneratorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.geckolib.renderer.GeoBlockRenderer;

public class FuelGeneratorBlockRenderer extends GeoBlockRenderer<FuelGeneratorBlockEntity, GeoBlockEntityRenderState> {
	public FuelGeneratorBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(context, new FuelGeneratorBlockModel());
	}

	@Override
	public GeoBlockEntityRenderState createRenderState() {
		return new GeoBlockEntityRenderState();
	}
}


