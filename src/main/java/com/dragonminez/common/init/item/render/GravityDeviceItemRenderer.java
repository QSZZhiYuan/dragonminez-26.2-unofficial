package com.dragonminez.common.init.item.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.dragonminez.common.init.item.GravityDeviceItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import com.geckolib.renderer.GeoItemRenderer;

public class GravityDeviceItemRenderer extends GeoItemRenderer<GravityDeviceItem> {

	public GravityDeviceItemRenderer() {
		super(new GravityDeviceItemModel());
	}

	@Override
	public RenderType getRenderType(com.geckolib.renderer.base.GeoRenderState renderState, Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}
}

