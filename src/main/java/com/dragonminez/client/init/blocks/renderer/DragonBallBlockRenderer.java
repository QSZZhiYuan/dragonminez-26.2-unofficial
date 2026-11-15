package com.dragonminez.client.init.blocks.renderer;

import com.dragonminez.client.render.GeoBlockEntityRenderState;

import com.dragonminez.client.init.blocks.model.DragonBallBlockModel;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.geckolib.renderer.GeoBlockRenderer;

public class DragonBallBlockRenderer extends GeoBlockRenderer<DragonBallBlockEntity, GeoBlockEntityRenderState> {
	public DragonBallBlockRenderer(BlockEntityRendererProvider.Context context) {
		super(context, new DragonBallBlockModel());
	}

	@Override
	public GeoBlockEntityRenderState createRenderState() {
		return new GeoBlockEntityRenderState();
	}
}

