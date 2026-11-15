package com.dragonminez.client.render.effects;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Charge effects are derived from the existing server-synchronized resources. */
public final class KiChargeEffects {
    private static final Map<UUID, KiChargeVisualState> STATES = new HashMap<>();
    private static ClientLevel level;
    private static boolean localInputCharging;

    private KiChargeEffects() {}

    public static void tick(Minecraft mc) {
        if (level != mc.level || mc.player == null) {
            STATES.clear();
            level = mc.level;
            localInputCharging = false;
        }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        var present = new HashSet<UUID>();
        for (Player player : mc.level.players()) {
            var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data == null || !data.getStatus().isHasCreatedCharacter() || !player.isAlive() || player.isSpectator()) continue;
            present.add(player.getUUID());
            var status = data.getStatus();
            boolean charging = (player == mc.player ? localInputCharging : status.isChargingKi())
                    && !status.isDescending() && !status.isStunned();
            float maxEnergy = data.getMaxEnergy();
            if (status.hasActiveShadowDummy()) {
                maxEnergy *= 1.0f - status.getShadowDummyPercent() / 100.0f;
            }
            STATES.computeIfAbsent(player.getUUID(), key -> new KiChargeVisualState())
                    .tick(charging, data.getResources().getCurrentEnergy(), maxEnergy);
        }
        STATES.keySet().retainAll(present);
    }

    public static KiChargeVisualState state(Player player) {
        return player == null ? null : STATES.get(player.getUUID());
    }

    public static void setLocalInputCharging(boolean charging) {
        localInputCharging = charging;
    }

    public static boolean isCharging(Player player) {
        var state = state(player);
        return player == Minecraft.getInstance().player && localInputCharging
                || state != null && state.isCharging();
    }

    public static float chargeEnvelope(Player player, float partialTick) {
        if (player == Minecraft.getInstance().player && localInputCharging) return 1.0f;
        var state = state(player);
        return state == null ? 0.0f : state.chargeEnvelope(partialTick);
    }

    public static boolean hasTriggeredBurst(Player player) {
        var state = state(player);
        return state != null && state.hasTriggeredBurst();
    }

    public static boolean hasChargeVisual(Player player, float partialTick) {
        if (player == Minecraft.getInstance().player && localInputCharging) return true;
        var state = state(player);
        return state != null && (state.isCharging() || state.chargeEnvelope(partialTick) > 0.001f);
    }

    public static void triggerBurst(int entityId) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (level != mc.level) {
            STATES.clear();
            level = mc.level;
        }
        if (mc.level.getEntity(entityId) instanceof Player player)
            STATES.computeIfAbsent(player.getUUID(), ignored -> new KiChargeVisualState()).triggerBurst();
    }

    public static float burst(Player player, float partialTick) {
        var state = state(player);
        return state == null ? 0.0f : state.burst(partialTick);
    }

    public static boolean hasCompletedBurst(Player player) {
        var state = state(player);
        return state != null && state.hasCompletedBurst();
    }

    public static float glow(Player player, float partialTick) {
        var state = state(player);
        return state == null ? 0.0f : state.glow(partialTick);
    }
}
