package com.dragonminez.client.init.menu.screens;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import com.dragonminez.common.network.C2S.GravityDeviceUpdateC2S;
import com.dragonminez.common.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GravityDeviceScreen extends AbstractContainerScreen<GravityDeviceMenu> {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/screen/gravity_device_gui.png");

	private EditBox gravityInput;
	private Button toggleButton;

	public GravityDeviceScreen(GravityDeviceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	protected void init() {
		super.init();
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		gravityInput = new EditBox(this.font, x + 40, y + 20, 50, 16, Component.translatable("gui.dragonminez.gravity_device.gravity"));
		gravityInput.setMaxLength(4);
		gravityInput.setValue(String.valueOf(Math.max(1, menu.getTargetGravity())));
		// 26.2: EditBox#setFilter is gone; digit filtering lives in the responder.
		gravityInput.setResponder(v -> { if (!v.matches("\\d{0,4}")) gravityInput.setValue(v.replaceAll("[^\\d]", "")); });
		addRenderableWidget(gravityInput);

		addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.gravity_device.set"), b -> sendUpdate(menu.isActive()))
				.bounds(x + 95, y + 19, 40, 18).build());

		toggleButton = Button.builder(toggleLabel(menu.isActive()), b -> sendUpdate(!menu.isActive()))
				.bounds(x + 40, y + 42, 95, 18).build();
		addRenderableWidget(toggleButton);
	}

	private Component toggleLabel(boolean active) {
		return active
				? Component.translatable("gui.dragonminez.gravity_device.turn_off")
				: Component.translatable("gui.dragonminez.gravity_device.turn_on");
	}

	private void sendUpdate(boolean active) {
		int gravity = parseGravity();
		NetworkHandler.sendToServer(new GravityDeviceUpdateC2S(menu.getBlockPos(), active, gravity));
	}

	private int parseGravity() {
		int max = ConfigManager.getServerConfig().getGravity().getDeviceMaxGravity();
		int value;
		try {
			value = Integer.parseInt(gravityInput.getValue());
		} catch (NumberFormatException e) {
			value = 1;
		}
		return Math.max(1, Math.min(value, max));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

		int energyHeight = menu.getScaledEnergy();
		guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, x + 154, y + 21 + (52 - energyHeight), 177, 21 + (60 - energyHeight), 12, energyHeight, 256, 256);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float delta) {
		// 26.2: vanilla calls extractBackground exactly once per frame from the final
		// extractRenderStateWithTooltipAndSubtitles; the explicit call here duplicated it.
		if (toggleButton != null) toggleButton.setMessage(toggleLabel(menu.isActive()));
		super.extractRenderState(guiGraphics, pMouseX, pMouseY, delta);

		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;

		Component status;
		if (!menu.isRoomValid()) {
			status = Component.translatable("gui.dragonminez.gravity_device.invalid_room").withStyle(ChatFormatting.RED);
		} else if (menu.isRunning()) {
			status = Component.translatable("gui.dragonminez.gravity_device.running", menu.getTargetGravity()).withStyle(ChatFormatting.GREEN);
		} else if (menu.isActive()) {
			status = Component.translatable("gui.dragonminez.gravity_device.no_energy").withStyle(ChatFormatting.GOLD);
		} else {
			status = Component.translatable("gui.dragonminez.gravity_device.idle").withStyle(ChatFormatting.GRAY);
		}
		guiGraphics.text(this.font, status, x + (imageWidth - this.font.width(status)) / 2, y + 64, 0xFFFFFFFF, false);

		extractTooltip(guiGraphics, pMouseX, pMouseY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY) {
		guiGraphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, 0xFF404040, false);
		guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY) {
		super.extractTooltip(guiGraphics, pMouseX, pMouseY);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		if (pMouseX >= x + 154 && pMouseX < x + 166 && pMouseY >= y + 21 && pMouseY < y + 73) {
			guiGraphics.setTooltipForNextFrame(this.font,
					Component.translatable("gui.dragonminez.fuel_generator.energy", menu.getEnergy(), menu.getMaxEnergy()),
					pMouseX, pMouseY);
		} else if (!menu.isRoomValid() && pMouseX >= x + 8 && pMouseX < x + 148
				&& pMouseY >= y + 64 && pMouseY < y + 73) {
			guiGraphics.setTooltipForNextFrame(this.font,
					Component.translatable("gui.dragonminez.gravity_device.no_room"), pMouseX, pMouseY);
		}
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (gravityInput.isFocused() && (event.key() == 257 || event.key() == 335)) {
			sendUpdate(menu.isActive());
			return true;
		}
		if (gravityInput.isFocused() && event.key() != 256) {
			return gravityInput.keyPressed(event) || super.keyPressed(event);
		}
		return super.keyPressed(event);
	}
}

