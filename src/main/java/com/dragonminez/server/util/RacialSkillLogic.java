package com.dragonminez.server.util;

import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;

public final class RacialSkillLogic {
    private RacialSkillLogic() {}
    public static void attemptRacialAction(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data ->
            RacialRegistry.forPlayer(data).ifPresent(ability -> {
                var context = new RacialContext(player, data);
                if (ability.canActivate(context)) ability.onActivate(context);
            }));
    }
}
