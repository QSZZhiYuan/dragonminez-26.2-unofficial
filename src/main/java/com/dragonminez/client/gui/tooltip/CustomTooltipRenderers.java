package com.dragonminez.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class CustomTooltipRenderers {

	public static class HeaderRenderer implements ClientTooltipComponent {
		private final ItemStack stack;
		private final FormattedCharSequence title;

		public HeaderRenderer(CustomTooltipNodes.HeaderNode node) {
			this.stack = node.stack();
			this.title = Language.getInstance().getVisualOrder(node.title());
		}

		@Override
		public int getHeight(Font font) {
			return 24;
		}

		@Override
		public int getWidth(Font font) {
			return 26 + font.width(title);
		}

		@Override
		public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
			if (!TooltipDecor.hasSpecialBorder) return;

			int margin = 2;
			int renderWidth = 22;
			int renderHeight = 22;

			int borderStart = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBorderStart >> 24) & 0xFF) * 0.35f), (TooltipDecor.currentBorderStart >> 16) & 0xFF, (TooltipDecor.currentBorderStart >> 8) & 0xFF, TooltipDecor.currentBorderStart & 0xFF);
			int bgStart = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBackgroundStart >> 24) & 0xFF) * 0.15f), (TooltipDecor.currentBackgroundStart >> 16) & 0xFF, (TooltipDecor.currentBackgroundStart >> 8) & 0xFF, TooltipDecor.currentBackgroundStart & 0xFF);
			int bgEnd = TooltipDecor.combineARGB((int) (((TooltipDecor.currentBackgroundEnd >> 24) & 0xFF) * 0.60f), (TooltipDecor.currentBackgroundEnd >> 16) & 0xFF, (TooltipDecor.currentBackgroundEnd >> 8) & 0xFF, TooltipDecor.currentBackgroundEnd & 0xFF);


			TooltipDecor.drawGradientRect(graphics, x + margin + 1, y + margin + 1, x + renderWidth - margin - 1, y + renderHeight - margin - 1, bgStart, bgEnd);
			TooltipDecor.drawGradientRectHorizontal(graphics, x + margin + 1, y + margin + 1, x + renderWidth - margin - 1, y + renderHeight - margin - 1, bgStart, bgEnd);

			TooltipDecor.drawGradientRect(graphics, x + margin + 1, y + margin, x + renderWidth - margin - 1, y + margin + 1, borderStart, borderStart);
			TooltipDecor.drawGradientRect(graphics, x + margin + 1, y + renderHeight - margin - 1, x + renderWidth - margin - 1, y + renderHeight - margin, borderStart, borderStart);
			TooltipDecor.drawGradientRect(graphics, x + margin, y + margin + 1, x + margin + 1, y + renderHeight - margin - 1, borderStart, borderStart);
			TooltipDecor.drawGradientRect(graphics, x + renderWidth - margin - 1, y + margin + 1, x + renderWidth - margin, y + renderHeight - margin - 1, borderStart, borderStart);

			// 26.2 GUI is 2D: GuiGraphicsExtractor#pose() is a Matrix3x2fStack, so the old
			// PoseStack 3D spin (Axis.YP rotation about the tooltip's Z depth) has no equivalent.
			// The item is drawn flat at the same anchor instead.
			float offsetX = -0.75f;
			float offsetY = -0.75f;

			float centerX = x + margin - 1 + 11 + offsetX;
			float centerY = y + margin - 1 + 11 + offsetY;

			graphics.pose().pushMatrix();
			graphics.pose().translate(centerX, centerY);
			float scale = 0.9f;
			graphics.pose().scale(scale, scale);
			graphics.item(stack, -8, -8);
			graphics.pose().popMatrix();
		}

		@Override
		public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
			int textY = y + (22 - font.lineHeight) / 2;
			graphics.text(font, title, x + 26, textY, -1, true);
		}
	}

	public static class SeparatorRenderer implements ClientTooltipComponent {
		public SeparatorRenderer(CustomTooltipNodes.SeparatorNode node) {}

		@Override
		public int getHeight(Font font) { return 0; }

		@Override
		public int getWidth(Font font) { return 0; }

		@Override
		public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
			if (!TooltipDecor.hasSpecialBorder) return;
			TooltipDecor.drawSeparator(graphics, TooltipDecor.lastTooltipX, y - 8, TooltipDecor.lastTooltipW, TooltipDecor.currentBorderStart);
		}
	}

	public static class PaddingRenderer implements ClientTooltipComponent {
		private final int height;
		public PaddingRenderer(CustomTooltipNodes.PaddingNode node) { this.height = node.height(); }
		@Override public int getHeight(Font font) { return height; }
		@Override public int getWidth(Font font) { return 0; }
		@Override public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) { }
	}
}
