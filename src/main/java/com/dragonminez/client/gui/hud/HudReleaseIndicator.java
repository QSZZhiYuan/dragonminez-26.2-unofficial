package com.dragonminez.client.gui.hud;

/** The numeric release and its pixel fill must agree after rounding. */
public record HudReleaseIndicator(int percentage, int fillHeight, int chargeHeight) {
    public static HudReleaseIndicator of(float release, int actionCharge, boolean charging) {
        int percentage = Math.max(0, Math.round(release));
        int fillHeight = Math.round(16 * Math.clamp(percentage / 100.0f, 0, 1));
        int chargeHeight = charging ? Math.round(17 * Math.clamp(actionCharge / 100.0f, 0, 1)) : 0;
        return new HudReleaseIndicator(percentage, fillHeight, chargeHeight);
    }
}
