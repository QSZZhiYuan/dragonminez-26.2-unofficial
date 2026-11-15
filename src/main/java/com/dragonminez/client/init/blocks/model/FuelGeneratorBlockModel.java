package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.custom.FuelGeneratorBlock;
import com.dragonminez.common.init.block.entity.FuelGeneratorBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import com.geckolib.model.GeoModel;

public class FuelGeneratorBlockModel extends GeoModel<FuelGeneratorBlockEntity> {

	/**
	 * GeckoLib 5: {@code getTextureResource} only receives the render state, so the lit/unlit flag is
	 * captured from the live block entity while it is still reachable (see
	 * {@link #addAdditionalStateData}) and read back at texture time.
	 */
	private static final com.geckolib.constant.dataticket.DataTicket<Boolean> DMZ_FUEL_LIT =
			com.geckolib.constant.dataticket.DataTicket.create("dmz_fuel_generator_lit", Boolean.class);

	@Override
	public void addAdditionalStateData(FuelGeneratorBlockEntity animatable, Object unused,
			com.geckolib.renderer.base.GeoRenderState renderState) {
		BlockState state = animatable.getBlockState();
		boolean isLit = state.hasProperty(FuelGeneratorBlock.LIT) && state.getValue(FuelGeneratorBlock.LIT);
		renderState.addGeckolibData(DMZ_FUEL_LIT, isLit);
	}

	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/fuel_generator");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		String textureState = Boolean.TRUE.equals(renderState.getGeckolibData(DMZ_FUEL_LIT)) ? "_on" : "_off";
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/fuel_generator" + textureState + ".png");
	}

	@Override
	public Identifier getAnimationResource(FuelGeneratorBlockEntity fuelGeneratorBlockEntity) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/fuel_generator");
	}
}
