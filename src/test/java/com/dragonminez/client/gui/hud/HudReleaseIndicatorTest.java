package com.dragonminez.client.gui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudReleaseIndicatorTest {
    @Test
    void aDisplayedHundredPercentAlwaysFillsTheWholeIcon() {
        for (float release : new float[]{99.5f, 99.99f, 100, 150}) {
            var indicator = HudReleaseIndicator.of(release, 0, false);
            assertTrue(indicator.percentage() >= 100);
            assertEquals(16, indicator.fillHeight());
            assertEquals(0, indicator.chargeHeight(), "Idle players must not show phantom charge");
        }
    }

    @Test
    void releaseAndTransformationChargeRemainIndependent() {
        var charging = HudReleaseIndicator.of(100, 50, true);
        assertEquals(100, charging.percentage());
        assertEquals(16, charging.fillHeight());
        assertEquals(9, charging.chargeHeight());
        assertEquals(0, HudReleaseIndicator.of(100, 50, false).chargeHeight());
        assertEquals(17, HudReleaseIndicator.of(0, 100, true).chargeHeight());
    }

    @Test
    void emptyAndOverchargedIndicatorsStayWithinTheirTextureRegions() {
        assertEquals(0, HudReleaseIndicator.of(-10, 0, false).fillHeight());
        int previous = -1;
        for (int release = 0; release <= 200; release++) {
            int height = HudReleaseIndicator.of(release, release, true).fillHeight();
            assertTrue(height >= previous && height <= 16);
            previous = height;
        }
    }
}
