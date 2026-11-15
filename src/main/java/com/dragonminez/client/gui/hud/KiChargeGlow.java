package com.dragonminez.client.gui.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Uses the bar's own alpha mask so the glow follows its outline in both HUD layouts. */
final class KiChargeGlow {
    private KiChargeGlow() {}

    static void draw(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v,
                     int width, int height, int textureSize, float strength, float tickTime, float[] rgb) {
        if (width <= 0 || strength < 0.01f) return;
        float pulse = 0.75f + 0.25f * (float) Math.sin(tickTime * 0.35f);
        int halo = ARGB.colorFromFloat(0.18f * strength * pulse, rgb[0], rgb[1], rgb[2]);
        blit(graphics, texture, x - 1, y, u, v, width, height, textureSize, halo);
        blit(graphics, texture, x + 1, y, u, v, width, height, textureSize, halo);
        blit(graphics, texture, x, y - 1, u, v, width, height, textureSize, halo);
        blit(graphics, texture, x, y + 1, u, v, width, height, textureSize, halo);
        int light = ARGB.colorFromFloat(0.4f * strength * pulse,
                0.7f + rgb[0] * 0.3f, 0.7f + rgb[1] * 0.3f, 0.7f + rgb[2] * 0.3f);
        blit(graphics, texture, x, y, u, v, width, height, textureSize, light);

        // A soft highlight sweeps through only the filled part of the ki bar.
        float bandWidth = 12.0f;
        float center = (tickTime * 0.055f % 1.0f) * (width + bandWidth * 2) - bandWidth;
        for (int column = Math.max(0, (int) (center - bandWidth)); column < Math.min(width, center + bandWidth); column++) {
            float alpha = 0.55f * strength * Math.max(0, 1.0f - Math.abs(column - center) / bandWidth);
            blit(graphics, texture, x + column, y, u + column, v, 1, height, textureSize,
                    ARGB.colorFromFloat(alpha, 1, 1, 1));
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v,
                             int width, int height, int textureSize, int tint) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, width, height,
                textureSize, textureSize, tint);
    }
}
