package com.dragonminez.common.init.item;

import com.dragonminez.common.init.item.render.GravityDeviceItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class GravityDeviceItem extends BlockItem implements GeoItem, com.geckolib.animatable.client.GeoRenderProvider {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public GravityDeviceItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public void createGeoRenderer(java.util.function.Consumer<com.geckolib.animatable.client.GeoRenderProvider> consumer) {
		consumer.accept(this);
	}

	/** Lazily created GeckoLib renderer; 26.2 resolves it through GeoRenderProvider. */
	private GravityDeviceItemRenderer renderer;

	@Override
	public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
		if (this.renderer == null) this.renderer = new GravityDeviceItemRenderer();
		return this.renderer;
	}
	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}

