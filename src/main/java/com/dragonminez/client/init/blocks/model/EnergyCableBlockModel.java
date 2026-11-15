package com.dragonminez.client.init.blocks.model;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.EnergyCableBlockEntity;
import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public class EnergyCableBlockModel extends GeoModel<EnergyCableBlockEntity> {

	/**
	 * Per-direction connection flags handed from {@code EnergyCableBlockRenderer#addRenderData} to
	 * {@code adjustModelBonesForRender}. GeckoLib 5 removed the {@code preRender} hook that used to
	 * carry the live block entity into the render pass, so the six flags travel on the render state.
	 */
	private static final java.util.Map<net.minecraft.core.Direction,
			com.geckolib.constant.dataticket.DataTicket<Boolean>> CONNECTION_TICKETS =
			new java.util.EnumMap<>(net.minecraft.core.Direction.class);

	private static com.geckolib.constant.dataticket.DataTicket<Boolean> ticketFor(net.minecraft.core.Direction dir) {
		return CONNECTION_TICKETS.computeIfAbsent(dir, d ->
				com.geckolib.constant.dataticket.DataTicket.create(
						"dmz_energy_cable_" + d.getName().toLowerCase(), Boolean.class));
	}

	public static void putConnection(GeoRenderState state, net.minecraft.core.Direction dir, boolean connected) {
		state.addGeckolibData(ticketFor(dir), connected);
	}

	public static Boolean getConnection(GeoRenderState state, net.minecraft.core.Direction dir) {
		return state.getGeckolibData(ticketFor(dir));
	}

	@Override
	public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/energy_cable");
	}

	@Override
	public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/custom/energy_cable.png");
	}

	@Override
	public Identifier getAnimationResource(EnergyCableBlockEntity animatable) {
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "block/energy_cable");
	}
}