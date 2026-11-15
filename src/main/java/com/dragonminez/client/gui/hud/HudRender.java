package com.dragonminez.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import com.dragonminez.client.gui.render.DmzGuiGradientQuad;
import com.mojang.blaze3d.pipeline.*;
import org.joml.Matrix3x2f;

public final class HudRender {
	private static float alphaScale = 1.0f;
	private static boolean additive;

	private HudRender() {}

	public static void setAdditive(boolean value) {
		additive = value;
	}

	public static void setAlphaScale(float alpha) {
		alphaScale = Mth.clamp(alpha, 0.0f, 1.0f);
	}

	public static float currentAlphaScale() {
		return alphaScale;
	}

	public static void sprite(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height, int rgb, float alpha) {
		spritePart(graphics, sprite, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, rgb, alpha);
	}

	public static void spritePart(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
								  float fromX, float fromY, float toX, float toY, int rgb, float alpha) {
		spritePart(graphics, sprite, x, y, width, height, fromX, fromY, toX, toY, rgb, alpha, false);
	}

	public static void sprite(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height, int rgb, float alpha, boolean flipX) {
		spritePart(graphics, sprite, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f, rgb, alpha, flipX);
	}

	public static void spritePart(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
								  float fromX, float fromY, float toX, float toY, int rgb, float alpha, boolean flipX) {

        float a = Mth.clamp(alpha, 0, 1) * alphaScale;
        if (a <= 0.004f || width <= 0 || height <= 0 || toX <= fromX || toY <= fromY) return;
        float x0 = flipX ? x + width * (1 - toX) : x + width * fromX;
        float y0 = y + height * fromY;
        float u0 = (sprite.u() + sprite.width() * (flipX ? toX : fromX)) / sprite.sheetWidth();
        float u1 = (sprite.u() + sprite.width() * (flipX ? fromX : toX)) / sprite.sheetWidth();
        int tint = argb(a, rgb);
        quad(graphics, sprite.sheet(), x0, y0, x0 + width * (toX - fromX), y0 + height * (toY - fromY),
                u0, (sprite.v() + sprite.height() * fromY) / sprite.sheetHeight(),
                u1, (sprite.v() + sprite.height() * toY) / sprite.sheetHeight(), tint, tint, tint, tint);
	}

	public static void spriteGradient(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
									  float from, float to, float alphaFrom, float alphaTo, int rgb, boolean flipX, boolean vertical) {
		spriteGradient(graphics, sprite, x, y, width, height, from, to, alphaFrom, alphaTo, alphaFrom, alphaTo, rgb, flipX, vertical);
	}

	public static void spriteGradient(GuiGraphicsExtractor graphics, HudSprites.Sprite sprite, float x, float y, float width, float height,
									  float from, float to, float alphaFrom, float alphaTo, float alphaFromBottom, float alphaToBottom,
									  int rgb, boolean flipX, boolean vertical) {
		float startAlpha = Mth.clamp(alphaFrom, 0.0f, 1.0f) * alphaScale;
		float endAlpha = Mth.clamp(alphaTo, 0.0f, 1.0f) * alphaScale;
		float startBottomAlpha = vertical ? startAlpha : Mth.clamp(alphaFromBottom, 0.0f, 1.0f) * alphaScale;
		float endBottomAlpha = vertical ? endAlpha : Mth.clamp(alphaToBottom, 0.0f, 1.0f) * alphaScale;
		if (Math.max(Math.max(startAlpha, endAlpha), Math.max(startBottomAlpha, endBottomAlpha)) <= 0.004f || width <= 0.0f || height <= 0.0f || to <= from) return;

		float fromX = vertical ? 0.0f : from, toX = vertical ? 1.0f : to;
		float fromY = vertical ? 1.0f - to : 0.0f, toY = vertical ? 1.0f - from : 1.0f;
		boolean flip = flipX && !vertical;
		float x0 = flip ? x + width * (1.0f - toX) : x + width * fromX;
		float x1 = x0 + width * (toX - fromX);
		float y0 = y + height * fromY;
		float y1 = y + height * toY;
		float u0 = (sprite.u() + sprite.width() * (flip ? toX : fromX)) / sprite.sheetWidth();
		float u1 = (sprite.u() + sprite.width() * (flip ? fromX : toX)) / sprite.sheetWidth();
		float v0 = (sprite.v() + sprite.height() * fromY) / sprite.sheetHeight();
		float v1 = (sprite.v() + sprite.height() * toY) / sprite.sheetHeight();

		int topLeft, topRight, bottomLeft, bottomRight;
		int start = Math.round(startAlpha * 255.0f);
		int end = Math.round(endAlpha * 255.0f);
		int startBottom = Math.round(startBottomAlpha * 255.0f);
		int endBottom = Math.round(endBottomAlpha * 255.0f);
		if (vertical) {
			topLeft = topRight = end;
			bottomLeft = bottomRight = start;
		} else if (flip) {
			topLeft = end;
			bottomLeft = endBottom;
			topRight = start;
			bottomRight = startBottom;
		} else {
			topLeft = start;
			bottomLeft = startBottom;
			topRight = end;
			bottomRight = endBottom;
		}
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        quad(graphics, sprite.sheet(), x0, y0, x1, y1, u0, v0, u1, v1,
                (topLeft << 24) | (rgb & 0xFFFFFF), (bottomLeft << 24) | (rgb & 0xFFFFFF),
                (bottomRight << 24) | (rgb & 0xFFFFFF), (topRight << 24) | (rgb & 0xFFFFFF));
	}

	public static void blit(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float u, float v, float width, float height, int textureWidth, int textureHeight) {
		blit(graphics, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight);
	}

	public static void blit(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float u, float v, float width, float height,
							float regionWidth, float regionHeight, int textureWidth, int textureHeight) {

        if (width <= 0 || height <= 0) return;
        int tint = argb(alphaScale, 0xFFFFFF);
        quad(graphics, texture, x, y, x + width, y + height, u / textureWidth, v / textureHeight,
                (u + regionWidth) / textureWidth, (v + regionHeight) / textureHeight, tint, tint, tint, tint);
	}

	public static void nineSlice(GuiGraphicsExtractor graphics, Identifier texture, float x, float y, float width, float height,
								 float u, float v, float regionWidth, float regionHeight, float border, int textureWidth, int textureHeight) {
		if (width <= 0.0f || height <= 0.0f) return;
		float destBorder = Math.min(border, Math.min(width, height) / 2.0f);
		float sourceBorder = Math.min(border, Math.min(regionWidth, regionHeight) / 2.0f);
		float[] destX = {x, x + destBorder, x + width - destBorder, x + width};
		float[] destY = {y, y + destBorder, y + height - destBorder, y + height};
		float[] sourceX = {u, u + sourceBorder, u + regionWidth - sourceBorder, u + regionWidth};
		float[] sourceY = {v, v + sourceBorder, v + regionHeight - sourceBorder, v + regionHeight};

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				blit(graphics, texture, destX[column], destY[row], sourceX[column], sourceY[row],
						destX[column + 1] - destX[column], destY[row + 1] - destY[row],
						sourceX[column + 1] - sourceX[column], sourceY[row + 1] - sourceY[row], textureWidth, textureHeight);
			}
		}
	}

	public static final int PANEL_FILL_RGB = 0x141B25;
	public static final float PANEL_FILL_ALPHA = 0.78f;
	public static final int PANEL_FRAME_RGB = 0x070A0F;
	public static final float PANEL_FRAME_ALPHA = 0.92f;
	public static final float PANEL_FRAME_WIDTH = 1.0f;
	private static final Identifier DMZ_FONT = Identifier.fromNamespaceAndPath(com.dragonminez.Reference.MOD_ID, "smooth");

	public static void panel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float alpha) {
		panel(graphics, x, y, width, height, PANEL_FILL_RGB, alpha);
	}

	public static void panel(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int fillRgb, float alpha) {
		float frame = PANEL_FRAME_WIDTH;
		rect(graphics, x + frame, y + frame, width - frame * 2.0f, height - frame * 2.0f, argb(PANEL_FILL_ALPHA * alpha, fillRgb));
		int frameColor = argb(PANEL_FRAME_ALPHA * alpha, PANEL_FRAME_RGB);
		rect(graphics, x, y, width, frame, frameColor);
		rect(graphics, x, y + height - frame, width, frame, frameColor);
		rect(graphics, x, y + frame, frame, height - frame * 2.0f, frameColor);
		rect(graphics, x + width - frame, y + frame, frame, height - frame * 2.0f, frameColor);
	}

	public static net.minecraft.network.chat.MutableComponent dmz(String text) {
		return net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}

	public static net.minecraft.network.chat.MutableComponent dmz(net.minecraft.network.chat.Component text) {
		return text.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}

	public static float dmzWidth(String text, float scale) {
		return Minecraft.getInstance().font.width(dmz(text)) * scale;
	}

	public static void dmzText(GuiGraphicsExtractor graphics, String text, float x, float y, float scale, float align, int rgb, float alpha) {
		if (text == null || text.isEmpty()) return;
		dmzText(graphics, dmz(text), x, y, scale, align, rgb, alpha);
	}

	private static net.minecraft.util.FormattedCharSequence uncolored(net.minecraft.util.FormattedCharSequence sequence) {
		return sink -> sequence.accept((index, style, codePoint) ->
				sink.accept(index, style.withColor((net.minecraft.network.chat.TextColor) null), codePoint));
	}

	public static void dmzText(GuiGraphicsExtractor graphics, net.minecraft.network.chat.Component component, float x, float y, float scale, float align, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale * 255.0f);
		if (alphaChannel <= 3 || component == null) return;

		Font font = Minecraft.getInstance().font;
		int color = (alphaChannel << 24) | (rgb & 0xFFFFFF);
		int border = alphaChannel << 24;
		float offset = -font.width(component) * align;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(offset, 0.0f);
		net.minecraft.util.FormattedCharSequence outline = uncolored(component.getVisualOrderText());
		graphics.text(font, outline, -1, 0, border, false);
		graphics.text(font, outline, 1, 0, border, false);
		graphics.text(font, outline, 0, -1, border, false);
		graphics.text(font, outline, 0, 1, border, false);
		graphics.text(font, component, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	public static void dmzText(GuiGraphicsExtractor graphics, net.minecraft.util.FormattedCharSequence sequence, float x, float y, float scale, float align, int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * alphaScale * 255.0f);
		if (alphaChannel <= 3 || sequence == null) return;

		Font font = Minecraft.getInstance().font;
		int color = (alphaChannel << 24) | (rgb & 0xFFFFFF);
		int border = alphaChannel << 24;
		float offset = -font.width(sequence) * align;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(offset, 0.0f);
		graphics.text(font, uncolored(sequence), -1, 0, border, false);
		graphics.text(font, uncolored(sequence), 1, 0, border, false);
		graphics.text(font, uncolored(sequence), 0, -1, border, false);
		graphics.text(font, uncolored(sequence), 0, 1, border, false);
		graphics.text(font, sequence, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	public static void scissor(GuiGraphicsExtractor graphics, float minX, float minY, float maxX, float maxY) {

        var min = graphics.pose().transformPosition(minX, minY, new org.joml.Vector2f());
        var max = graphics.pose().transformPosition(maxX, maxY, new org.joml.Vector2f());
        graphics.enableScissor(Math.round(Math.min(min.x, max.x)), Math.round(Math.min(min.y, max.y)),
                Math.round(Math.max(min.x, max.x)), Math.round(Math.max(min.y, max.y)));
	}

	public static void rect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
		gradient(graphics, x, y, width, height, color, color, color, color);
	}

	public static void rectHorizontal(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int colorLeft, int colorRight) {
		gradient(graphics, x, y, width, height, colorLeft, colorLeft, colorRight, colorRight);
	}

	public static void rectVertical(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int colorTop, int colorBottom) {
		gradient(graphics, x, y, width, height, colorTop, colorBottom, colorBottom, colorTop);
	}

	private static void gradient(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int topLeft, int bottomLeft, int bottomRight, int topRight) {

        if (width <= 0 || height <= 0) return;
        quad(graphics, null, x, y, x + width, y + height, 0, 0, 1, 1,
                alpha(topLeft), alpha(bottomLeft), alpha(bottomRight), alpha(topRight));
	}

	public static void text(GuiGraphicsExtractor graphics, String text, float x, float y, float scale, float align, int rgb, float alpha) {
		dmzText(graphics, text, x, y, scale, align, rgb, alpha);
	}

    private static final RenderPipeline ADD_TEXTURE = RenderPipelines.GUI_TEXTURED.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "gui_hud_additive_texture"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).build();
    private static final RenderPipeline ADD_COLOR = RenderPipelines.GUI.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "gui_hud_additive_color"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).build();
    private static int alpha(int color) { return (Math.round(((color >>> 24) & 255) * alphaScale) << 24) | (color & 0xFFFFFF); }
    private static void quad(GuiGraphicsExtractor graphics, Identifier texture, float x0, float y0, float x1, float y1,
            float u0, float v0, float u1, float v1, int tl, int bl, int br, int tr) {
        TextureSetup setup = TextureSetup.noTexture();
        if (texture != null) {
            var image = Minecraft.getInstance().getTextureManager().getTexture(texture);
            setup = TextureSetup.singleTexture(image.getTextureView(), image.getSampler());
        }
        graphics.submitGuiElementRenderState(new DmzGuiGradientQuad(
                texture == null ? (additive ? ADD_COLOR : RenderPipelines.GUI) : (additive ? ADD_TEXTURE : RenderPipelines.GUI_TEXTURED),
                setup, new Matrix3x2f(graphics.pose()), x0, y0, x1, y1, u0, v0, u1, v1, tl, bl, br, tr, graphics.peekScissorStack()));
    }

	public static int argb(float alpha, int rgb) {
		return (Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24) | (rgb & 0xFFFFFF);
	}

	public static int rgb(float[] rgb, float brightness) {
		int r = Math.round(Mth.clamp(rgb[0] * brightness, 0.0f, 1.0f) * 255.0f);
		int g = Math.round(Mth.clamp(rgb[1] * brightness, 0.0f, 1.0f) * 255.0f);
		int b = Math.round(Mth.clamp(rgb[2] * brightness, 0.0f, 1.0f) * 255.0f);
		return (r << 16) | (g << 8) | b;
	}

	public static int mix(int rgbFrom, int rgbTo, float amount) {
		float t = Mth.clamp(amount, 0.0f, 1.0f);
		int r = Math.round(((rgbFrom >> 16) & 0xFF) + (((rgbTo >> 16) & 0xFF) - ((rgbFrom >> 16) & 0xFF)) * t);
		int g = Math.round(((rgbFrom >> 8) & 0xFF) + (((rgbTo >> 8) & 0xFF) - ((rgbFrom >> 8) & 0xFF)) * t);
		int b = Math.round((rgbFrom & 0xFF) + ((rgbTo & 0xFF) - (rgbFrom & 0xFF)) * t);
		return (r << 16) | (g << 8) | b;
	}
}
