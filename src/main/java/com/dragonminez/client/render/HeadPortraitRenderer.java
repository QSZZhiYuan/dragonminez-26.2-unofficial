package com.dragonminez.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;

/** The new HUD portrait uses the same deferred character renderer as menus. */
public final class HeadPortraitRenderer {
    private HeadPortraitRenderer() {}

    public static void render(GuiGraphicsExtractor graphics, AbstractClientPlayer player,
            float x, float y, float size, float partialTick, boolean mirrored) {
        if (player != null && size > 0)
            EntityPreviewRenderContext.renderHeadPortrait(graphics, player, x, y, size, partialTick, mirrored);
    }
}
