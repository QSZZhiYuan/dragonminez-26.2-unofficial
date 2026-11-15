package com.dragonminez.client.render;

import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityPreviewLayoutTest {
    @Test
    void previewKeepsFeetAtRequestedAnchorAndIncludesHairAboveHead() {
        var layout = EntityPreviewRenderState.layout(new Matrix3x2f(), 200, 190, 75, 0.6F, 1.8F);
        assertEquals(200, (layout.x0() + layout.x1()) / 2);
        assertEquals(190, layout.y1() - layout.bottomPadding());
        assertTrue(190 - layout.y0() > 75 * 2.5F, "Room for the full body and tall hair");
        assertTrue(layout.x1() - layout.x0() > 75, "Arms must fit outside the old scale-wide box");
    }

    @Test
    void menuPoseScalesTextureDensityAndMovesAnchorExactlyOnce() {
        var pose = new Matrix3x2f().translation(10, 20).scale(2);
        var layout = EntityPreviewRenderState.layout(pose, 200, 190, 75, 0.6F, 1.8F);
        assertEquals(410, (layout.x0() + layout.x1()) / 2);
        assertEquals(400, layout.y1() - layout.bottomPadding());
        assertEquals(150, layout.scale());
    }
}
