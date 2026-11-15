package com.dragonminez.common.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Normalizes deferred mod-effect holders before hybrid servers bridge them to Bukkit. */
public final class MobEffectCompatibility {
	private MobEffectCompatibility() {}

	public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier) {
		return new MobEffectInstance(resolve(effect), duration, amplifier);
	}

	public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier,
										   boolean ambient, boolean showParticles) {
		return new MobEffectInstance(resolve(effect), duration, amplifier, ambient, showParticles);
	}

	public static MobEffectInstance create(Holder<MobEffect> effect, int duration, int amplifier,
										   boolean ambient, boolean showParticles, boolean showIcon) {
		return new MobEffectInstance(resolve(effect), duration, amplifier, ambient, showParticles, showIcon);
	}

	public static Holder<MobEffect> resolve(Holder<MobEffect> effect) {
		var key = effect.unwrapKey();
		if (key.isEmpty()) return effect;
		return BuiltInRegistries.MOB_EFFECT.getOptional(key.get())
				.map(BuiltInRegistries.MOB_EFFECT::wrapAsHolder)
				.orElse(effect);
	}
}
