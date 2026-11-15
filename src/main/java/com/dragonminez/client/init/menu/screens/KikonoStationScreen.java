package com.dragonminez.client.init.menu.screens;

import com.dragonminez.Reference;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class KikonoStationScreen extends AbstractContainerScreen<KikonoStationMenu> {
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/kikono_station_gui.png");

	public KikonoStationScreen(KikonoStationMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

		if (menu.isCrafting()) {
			guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x + 111, y + 35, 177, 2, menu.getScaledProgress(), 17, 256, 256);
		}

		int energyHeight = menu.getScaledEnergy();
		guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x + 9, y + 21 + (52 - energyHeight), 177, 21 + (60 - energyHeight), 12, energyHeight, 256, 256);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float delta) {
		// 26.2: vanilla calls extractBackground exactly once per frame from the final
		// extractRenderStateWithTooltipAndSubtitles; the explicit call here duplicated it.
		super.extractRenderState(guiGraphics, pMouseX, pMouseY, delta);
		extractTooltip(guiGraphics, pMouseX, pMouseY);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY) {

		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		if(pMouseX >= x + 10 && pMouseX <= x + 18 && pMouseY >= y + 16 && pMouseY <= y + 68) {
			guiGraphics.setTooltipForNextFrame(this.font,
					Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " Star Energy"),
					pMouseX, pMouseY);
		}
	}
}
