package com.dragonminez.client.render.effects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KiChargeVisualStateTest {
    @Test
    void resourceSyncAtFullKiDoesNotDuplicateTheServerBurst() {
        var state = new KiChargeVisualState();
        state.tick(true, 20, 100);
        assertEquals(0, state.burst(0));
        state.tick(true, 100, 100);
        assertEquals(0, state.burst(0));
        assertEquals(0, state.shake(0));
        for (int tick = 0; tick < KiChargeVisualState.SHAKE_TICKS; tick++) state.tick(true, 100, 100);
        assertEquals(0, state.shake(0));
        assertEquals(0, state.burst(0));
        for (int tick = 0; tick < 80; tick++) state.tick(true, 100, 100);
        assertEquals(0, state.burst(0), "Holding the key at full ki must not repeat the burst");
    }

    @Test
    void passiveRegenerationAndChargingFromFullDoNotCauseFalseBursts() {
        var state = new KiChargeVisualState();
        state.tick(false, 20, 100);
        state.tick(false, 100, 100);
        state.tick(true, 100, 100);
        state.tick(false, 100, 100);
        state.tick(true, 100, 100);
        assertEquals(0, state.burst(0));
    }

    @Test
    void explicitlyTriggeredFlashFadesAfterKeyReleaseWithoutRepeating() {
        var state = new KiChargeVisualState();
        state.tick(true, 25, 100);
        state.tick(true, 100, 100);
        state.triggerBurst();
        assertEquals(1, state.burst(0));
        assertEquals(1, state.shake(0));
        state.tick(false, 100, 100);
        assertTrue(state.burst(0) > 0);
        for (int tick = 0; tick < 40; tick++) state.tick(false, 50, 100);
        state.tick(true, 50, 100);
        state.tick(true, 100, 100);
        assertEquals(0, state.burst(0));
        state.triggerBurst();
        assertEquals(1, state.burst(0));
    }

    @Test
    void glowFadesAndInvalidPoolsNeverTriggerABurst() {
        var state = new KiChargeVisualState();
        for (int tick = 0; tick < 20; tick++) state.tick(true, 50, 100);
        assertTrue(state.glow(1) > 0.99f);
        for (int tick = 0; tick < 30; tick++) state.tick(false, 50, 100);
        assertTrue(state.glow(1) < 0.001f);
        state.tick(true, 0, 0);
        state.tick(true, Float.NaN, 100);
        state.tick(true, 100, Float.POSITIVE_INFINITY);
        assertEquals(0, state.burst(0));
    }
}
