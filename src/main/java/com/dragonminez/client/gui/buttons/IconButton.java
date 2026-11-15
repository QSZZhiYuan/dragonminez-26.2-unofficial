package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IconButton extends Button {
	private final Identifier icon;
	private final int iconSize;
	private final int textureSize;

	public IconButton(int x, int y, int width, int height, Identifier icon, int iconSize, int textureSize,
						Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		this.iconSize = iconSize;
		this.textureSize = textureSize;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 26.2: AbstractButton#extractWidgetRenderState is final; content goes in extractContents.
        // Icon-only button, so the default sprite/label is skipped on purpose.
		int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
		int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, textureSize, textureSize);
	}
}

