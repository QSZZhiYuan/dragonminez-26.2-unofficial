package com.dragonminez.client.gui.tooltip;

import com.dragonminez.Reference;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class TooltipDecor {
	public static final Identifier DEFAULT_BORDERS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/tooltip_borders.png");

	public static int currentBorderStart = 0;
	public static int currentBorderEnd = 0;
	public static int currentBackgroundStart = 0;
	public static int currentBackgroundEnd = 0;

	public static boolean hasSpecialBorder = false;

	public static boolean forceCustomBorder = false;
	public static int forcedColor = 0xFFFFFF;
	public static boolean hasItemBox = false;

	public static int lastTooltipX = 0;
	public static int lastTooltipY = 0;
	public static int lastTooltipW = 0;
	public static int lastTooltipH = 0;

	private static float shineTimer = 2.5f;
	public static float rotationTimer = 0.0f;

	public static void updateTimer(float deltaTime) {
		if (shineTimer > 0.0f) shineTimer -= deltaTime;
		rotationTimer += deltaTime;
		if (rotationTimer > 100.0f) rotationTimer -= 100.0f;
	}

	public static void resetTimer() {
		shineTimer = 2.5f;
	}

	/**
	 * Drop shadow around the tooltip.
	 *
	 * <h2>26.2 migration</h2>
	 * These helpers used to take a {@code PoseStack} and draw through the {@code Tesselator}
	 * singleton + {@code BufferUploader}, with blend state flipped by hand via {@code RenderSystem}.
	 * All of that is gone in 26.2. {@code GuiGraphicsExtractor} is the only GUI drawing surface now,
	 * and {@code GuiGraphicsExtractor#pose()} returns a {@code Matrix3x2fStack} rather than a
	 * {@code PoseStack}, so the pose parameter became the extractor itself.
	 */
	public static void drawShadow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		int shadowColor = 0x44000000;
		drawGradientRect(graphics, x - 1, y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		drawGradientRect(graphics, x + width + 4, y - 1, x + width + 5, y + height + 5, shadowColor, shadowColor);
		drawGradientRect(graphics, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);
		drawGradientRect(graphics, x, y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		drawGradientRect(graphics, x + width + 5, y, x + width + 6, y + height + 5, shadowColor, shadowColor);
	}

	public static void drawSeparator(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int width, int color) {
		drawGradientRectHorizontal(graphics, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		drawGradientRectHorizontal(graphics, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
	}

	public static void drawBorder(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
		if (!hasSpecialBorder) return;

		if (shineTimer >= 0.5f && shineTimer <= 2.0f) {
			float interval = Mth.clamp(shineTimer - 0.5f, 0.0f, 1.0f);
			int alpha = (int)(0x99 * interval) << 24;
			int hMin = x - 3;
			int hMax = x + width + 3;
			int hInterval = (int)Mth.lerp(interval * interval, hMax, hMin);
			drawGradientRectHorizontal(graphics, Math.max(hInterval - 36, hMin), y - 3, Math.min(hInterval, hMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
			drawGradientRectHorizontal(graphics, Math.max(hInterval, hMin), y - 3, Math.min(hInterval + 36, hMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
		}

		if (shineTimer <= 1.0f) {
			float interval = Mth.clamp(shineTimer, 0.0f, 1.0f);
			int alpha = (int)(0x55 * interval) << 24;
			int vMin = y - 3 + 1;
			int vMax = y + height + 3 - 1;
			int vInterval = (int)Mth.lerp(interval * interval, vMax, vMin);
			drawGradientRect(graphics, x - 3, Math.max(vInterval - 12, vMin), x - 3 + 1, Math.min(vInterval, vMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
			drawGradientRect(graphics, x - 3, Math.max(vInterval, vMin), x - 3 + 1, Math.min(vInterval + 12, vMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
		}

		int texW = 128;
		int texH = 128;
		int u = 0;
		int v = 0;

		// 26.2: GuiGraphicsExtractor#blit takes the destination rect plus the source rect in texels.
		blit(graphics, x - 6, y - 6, 8, 8, u, v, 8, 8, texW, texH);
		blit(graphics, x + width - 2, y - 6, 8, 8, 56 + u, v, 8, 8, texW, texH);
		blit(graphics, x - 6, y + height - 2, 8, 8, u, v + 8, 8, 8, texW, texH);
		blit(graphics, x + width - 2, y + height - 2, 8, 8, 56 + u, v + 8, 8, 8, texW, texH);

		if (width >= 48) {
			blit(graphics, x + (width / 2) - 24, y - 9, 48, 8, 8 + u, v, 48, 8, texW, texH);
			blit(graphics, x + (width / 2) - 24, y + height + 1, 48, 8, 8 + u, v + 8, 48, 8, texW, texH);
		}
	}

	/** Texel-space blit onto the tooltip border atlas. */
	private static void blit(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
							 int x, int y, int width, int height,
							 float uOffset, float vOffset, int uWidth, int vHeight,
							 int texW, int texH) {
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DEFAULT_BORDERS,
				x, y, uOffset, vOffset, width, height, uWidth, vHeight, texW, texH);
	}


	public static int combineARGB(int a, int r, int g, int b) {
		a = Mth.clamp(a, 0, 255);
		r = Mth.clamp(r, 0, 255);
		g = Mth.clamp(g, 0, 255);
		b = Mth.clamp(b, 0, 255);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
	/**
	 * Vertical gradient rectangle in GUI space.
	 *
	 * <h2>26.2 migration</h2>
	 * 1.21 built this through the {@code Tesselator} singleton plus {@code BufferUploader}, with the
	 * blend state set by hand via {@code RenderSystem}. All three are gone: the Tesselator singleton
	 * and BufferUploader were deleted, and blend state now lives on the render pipeline.
	 * {@code GuiGraphicsExtractor#fillGradient} is the direct replacement (same top-to-bottom order).
	 */
	public static void drawGradientRect(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int left, int top,
										int right, int bottom, int startColor, int endColor) {
		graphics.fillGradient(left, top, right, bottom, startColor, endColor);
	}

	/**
	 * Horizontal gradient rectangle in GUI space.
	 *
	 * <p>26.2's {@code GuiGraphicsExtractor} only offers a top-to-bottom {@code fillGradient}; there
	 * is no horizontal variant. This falls back to a flat {@code fill} using the start colour, which
	 * keeps the border readable but loses the left-to-right fade.
	 * TODO(26.2 gui): restore the horizontal fade once a horizontal gradient pipeline is available.
	 */
	public static void drawGradientRectHorizontal(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int left, int top,
												  int right, int bottom, int startColor, int endColor) {
		graphics.fill(left, top, right, bottom, startColor);
	}
}
