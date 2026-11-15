package com.dragonminez.client.render.effects;

/** Tick-driven charge visuals; no gameplay resources or persisted state are changed. */
public final class KiChargeVisualState {
    public static final int BURST_TICKS = 30;
    public static final int SHAKE_TICKS = 8;

    private int burstAge = BURST_TICKS;
    private boolean burstTriggered;
    private boolean charging;
    private float previousChargeEnvelope;
    private float chargeEnvelope;
    private float previousGlow;
    private float glow;

    public void triggerBurst() {
        burstAge = 0;
        burstTriggered = true;
    }

    public void tick(boolean charging, float energy, float maxEnergy) {
        if (!Float.isFinite(energy) || !Float.isFinite(maxEnergy) || maxEnergy <= 0) charging = false;
        this.charging = charging;
        previousChargeEnvelope = chargeEnvelope;
        // Keep the aura present for as long as the player holds C, independent of
        // the separate 30-tick burst flash. Only the key release starts contraction.
        chargeEnvelope = charging ? 1.0f : Math.max(0.0f, chargeEnvelope - 1.0f / 10.0f);
        previousGlow = glow;
        glow += ((charging ? 1.0f : 0.0f) - glow) * 0.25f;
        if (burstAge < BURST_TICKS) burstAge++;

        // The server owns surge entities and state. Resource synchronization must not
        // synthesize a second client burst when energy first reaches its maximum.
    }

    public float glow(float partialTick) {
        float partial = Math.clamp(partialTick, 0.0f, 1.0f);
        return previousGlow + (glow - previousGlow) * partial;
    }

    public float burstProgress(float partialTick) {
        return Math.clamp((burstAge + Math.clamp(partialTick, 0.0f, 1.0f)) / BURST_TICKS, 0.0f, 1.0f);
    }

    public float burst(float partialTick) {
        return 1.0f - burstProgress(partialTick);
    }

    public boolean isCharging() {
        return charging;
    }

    public float chargeEnvelope(float partialTick) {
        float partial = Math.clamp(partialTick, 0.0f, 1.0f);
        return previousChargeEnvelope + (chargeEnvelope - previousChargeEnvelope) * partial;
    }

    public boolean hasTriggeredBurst() {
        return burstTriggered;
    }

    public boolean hasCompletedBurst() {
        return burstTriggered && burstAge >= BURST_TICKS;
    }

    public float shake(float partialTick) {
        return Math.max(0.0f, 1.0f - (burstAge + Math.clamp(partialTick, 0.0f, 1.0f)) / SHAKE_TICKS);
    }
}
