package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DiscordTitleButton extends Button {

	private static final Identifier DISCORD_LOGO = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/title/discord_logo.png");

	public DiscordTitleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 26.2: AbstractButton#renderString is gone; extractContents takes no font/color, so both
        // are resolved here from the render context.
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        int color = 0xFFFFFFFF;
		int textWidth = font.width(this.getMessage());
		int contentWidth = 12 + 4 + textWidth;
		int startX = this.getX() + Math.max(4, (this.getWidth() - contentWidth) / 2);
		int iconY = this.getY() + (this.getHeight() - 12) / 2;

		this.renderDiscordLogo(graphics, startX, iconY);
		graphics.text(font, this.getMessage(), startX + 12 + 4, this.getY() + (this.getHeight() - 8) / 2 + 1, color);
	}

	private void renderDiscordLogo(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DISCORD_LOGO, x, y, 0.0F, 0.0F, 12, 12, 32, 32);
	}
}

