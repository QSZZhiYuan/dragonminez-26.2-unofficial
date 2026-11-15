package com.dragonminez.client.util;

import com.dragonminez.client.gui.tooltip.TooltipDecor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * Centers a single tooltip line on the tooltip border drawn by {@link TooltipDecor}.
 *
 * <h2>26.2 migration</h2>
 * {@code ClientTooltipComponent} changed shape:
 * <ul>
 *   <li>{@code getHeight()} → {@code getHeight(Font)};</li>
 *   <li>{@code renderText(Font, int, int, Matrix4f, MultiBufferSource)} →
 *       {@code extractText(GuiGraphicsExtractor, Font, int, int)} — the tooltip is now extracted into
 *       the GUI render state rather than drawn immediately, so there is no matrix or buffer source.</li>
 * </ul>
 */
public class CenteredTextComponent implements ClientTooltipComponent {
	private final ClientTooltipComponent original;

	public CenteredTextComponent(FormattedCharSequence text) {
		this.original = ClientTooltipComponent.create(text);
	}

	@Override
	public int getWidth(Font font) {
		return this.original.getWidth(font);
	}

	@Override
	public int getHeight(Font font) {
		return this.original.getHeight(font);
	}

	@Override
	public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
		int finalX = x;

		if (TooltipDecor.hasSpecialBorder && !TooltipDecor.hasItemBox && y <= TooltipDecor.lastTooltipY + 12) {
			int textW = this.getWidth(font);
			finalX = TooltipDecor.lastTooltipX + (TooltipDecor.lastTooltipW / 2) - (textW / 2);
		}

		this.original.extractText(graphics, font, finalX, y);
	}
}
