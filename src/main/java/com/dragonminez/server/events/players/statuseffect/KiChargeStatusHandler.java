package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.MobEffectCompatibility;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

public class KiChargeStatusHandler implements IStatusEffectHandler {
	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		Holder<MobEffect> kiChargeEffect = MobEffectCompatibility.resolve(MainEffects.KICHARGE);
		if (data.getStatus().isChargingKi()) {
			if (!player.hasEffect(kiChargeEffect)) {
				player.addEffect(MobEffectCompatibility.create(kiChargeEffect, -1, 0, false, false, true));
			}
		} else if (player.hasEffect(kiChargeEffect)) {
			player.removeEffect(kiChargeEffect);
		}
	}

	@Override
	public void onPlayerTick(ServerPlayer player, StatsData data) {
	}

	@Override
	public void onPlayerSecond(ServerPlayer player, StatsData data) {
	}
}
