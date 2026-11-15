package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.item.weapons.render.BraveSwordRenderer;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public class BraveSwordItem extends WeaponItem implements GeoItem, com.geckolib.animatable.client.GeoRenderProvider {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public BraveSwordItem() {
		this(new net.minecraft.world.item.Item.Properties());
	}

	/** 26.2: the registry id must be on the properties before super() runs. */
	public BraveSwordItem(net.minecraft.world.item.Item.Properties properties) {
		super(75, -2.4f, 2600, 20, "brave_sword", properties);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public void createGeoRenderer(java.util.function.Consumer<com.geckolib.animatable.client.GeoRenderProvider> consumer) {
		consumer.accept(this);
	}

	/** Lazily created GeckoLib renderer; 26.2 resolves it through GeoRenderProvider. */
	private BraveSwordRenderer renderer;

	@Override
	public com.geckolib.renderer.GeoItemRenderer<?> getGeoItemRenderer() {
		if (this.renderer == null) this.renderer = new BraveSwordRenderer();
		return this.renderer;
	}
	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}

