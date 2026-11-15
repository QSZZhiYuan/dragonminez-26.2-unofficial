package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;

public class RacialStatusHandler implements IStatusEffectHandler {
    private final java.util.Map<java.util.UUID, RacialAbility> active = new java.util.HashMap<>();

	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
        var ability = RacialRegistry.forPlayer(data).orElse(null);
        var previous = active.get(serverPlayer.getUUID());
        if (previous != null && previous != ability) previous.onLogout(new RacialContext(serverPlayer, data));
        if (ability == null) active.remove(serverPlayer.getUUID());
        else {
            active.put(serverPlayer.getUUID(), ability);
            ability.onTick(new RacialContext(serverPlayer, data));
        }
        active.keySet().removeIf(id -> serverPlayer.level().getServer().getPlayerList().getPlayer(id) == null);
    }

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
		RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onSecond(new RacialContext(serverPlayer, data)));
	}
}
