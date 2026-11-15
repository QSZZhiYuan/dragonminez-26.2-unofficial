package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.network.C2S.GrantWishC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WishesScreen extends ScaledScreen {

	private static final Identifier MENU_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 20;
	private static final int MAX_VISIBLE_ITEMS = 8;

	private final String dragonType;
	private final int maxWishesToSelect;
	private List<Wish> availableWishes;
	private final List<Integer> selectedIndices = new ArrayList<>();

	private int guiLeft, guiTop;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton confirmButton;

	public WishesScreen(String dragonType, int wishCount) {
		super(Component.translatable("gui.dragonminez.wishes_title", 0, wishCount));
		this.dragonType = dragonType;
		this.maxWishesToSelect = wishCount;
		this.availableWishes = WishManager.getClientWishes(dragonType);
	}

	@Override
	protected void init() {
		super.init();
		refreshWishes();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

		this.confirmButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, getUiHeight() - 30)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.customization.select"))
				.onPress(btn -> confirmWishes())
				.build();

		this.confirmButton.visible = false;
		this.addRenderableWidget(confirmButton);
	}

	private void confirmWishes() {
		refreshWishes();
		if (selectedIndices.size() != maxWishesToSelect) return;
		NetworkHandler.sendToServer(new GrantWishC2S(dragonType, selectedIndices));
		this.onClose();
	}


	// 26.2: vanilla already calls extractBackground once per frame from the final
	// extractRenderStateWithTooltipAndSubtitles; calling it again here caused
	//   IllegalStateException: Can only blur once per frame
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		refreshWishes();

		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				tr("gui.dragonminez.wishes_title", selectedIndices.size(), maxWishesToSelect),
				getUiWidth() / 2, guiTop + 18, 0xFFFFD700);

		renderWishesList(graphics, uiMouseX, uiMouseY);

		super.extractRenderState(graphics, uiMouseX, uiMouseY, partialTick);

		renderTooltip(graphics, uiMouseX, uiMouseY);

		endUiScale(graphics);
	}

	private void renderWishesList(GuiGraphicsExtractor graphics, int uiMouseX, int uiMouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		int totalHeight = availableWishes.size() * ITEM_HEIGHT;

		maxScroll = Math.max(0, totalHeight - viewHeight);
		targetScroll = Mth.clamp(targetScroll, 0, maxScroll);
		float tickDelta = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
		currentScroll = Mth.lerp(tickDelta * 0.4f, currentScroll, targetScroll);

		int scLeft = toScissorCoord(listLeft);
		int scTop = toScissorCoord(listTop);
		int scRight = toScissorCoord(listLeft + listWidth);
		int scBottom = toScissorCoord(listTop + viewHeight);

		graphics.enableScissor(scLeft, scTop, scRight, scBottom);
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, -currentScroll);

		for (int i = 0; i < availableWishes.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT >= listTop + currentScroll && itemY <= listTop + viewHeight + currentScroll) {
				Wish wish = availableWishes.get(i);
				boolean isSelected = selectedIndices.contains(i);
				boolean isHovered = uiMouseX >= listLeft && uiMouseX < listLeft + listWidth &&
						uiMouseY >= itemY - currentScroll && uiMouseY < itemY + ITEM_HEIGHT - currentScroll;

				int color;
				if (isSelected) color = 0x80D4AF37;
				else if (isHovered) color = 0x80555555;
				else color = 0x00000000;

				graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);

				TextUtil.drawStringWithBorder(graphics, this.font, tr(wish.getName()), listLeft + 5, itemY + 6, 0xFFFFFFFF);

				if (isSelected) {
					graphics.outline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
				}
			}
		}

		graphics.pose().popMatrix();
		graphics.disableScissor();

		if (maxScroll > 0) {
			int scrollBarX = guiLeft + PANEL_WIDTH - 12;
			int scrollBarY = listTop;

			graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + viewHeight, 0xFF333333);

			float scrollPercent = currentScroll / maxScroll;
			float visiblePercent = (float) viewHeight / totalHeight;

			int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
			int indicatorY = scrollBarY + (int) ((viewHeight - indicatorHeight) * scrollPercent);

			graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderTooltip(GuiGraphicsExtractor graphics, int uiMouseX, int uiMouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (uiMouseX >= listLeft && uiMouseX < listLeft + listWidth && uiMouseY >= listTop && uiMouseY <= listTop + viewHeight) {
			int relativeY = (int) (uiMouseY - listTop + currentScroll);
			int index = relativeY / ITEM_HEIGHT;

			if (index >= 0 && index < availableWishes.size()) {
				Wish wish = availableWishes.get(index);
				graphics.setTooltipForNextFrame(this.font, tr(wish.getDescription()), uiMouseX, uiMouseY);
			}
		}
	}

	private float calculateScrollPercent(double uiY, int startY, int viewHeight) {
		float percent = (float)(uiY - startY) / viewHeight;
		return Mth.clamp(percent, 0.0f, 1.0f);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (super.mouseClicked(event, doubleClick)) return true;

		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);

		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;

		if (maxScroll > 0 && uiX >= listLeft + listWidth && uiX <= guiLeft + PANEL_WIDTH &&
				uiY >= listTop && uiY <= listTop + viewHeight) {
			this.isScrolling = true;
			targetScroll = calculateScrollPercent(uiY, listTop, viewHeight) * maxScroll;
			return true;
		}

		if (uiX >= listLeft && uiX < listLeft + listWidth && uiY >= listTop && uiY <= listTop + viewHeight) {
			int relativeY = (int) (uiY - listTop + currentScroll);
			int index = relativeY / ITEM_HEIGHT;

			if (index >= 0 && index < availableWishes.size()) {
				toggleSelection(index);
				Minecraft.getInstance().getSoundManager().play(
						net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
				);
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll > 0) {
			targetScroll = (float) Mth.clamp(targetScroll - (Math.signum(scrollY) * ITEM_HEIGHT * 2), 0, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		this.isScrolling = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (isScrolling && maxScroll > 0) {
			double uiY = toUiY(mouseY);
			int listTop = guiTop + 35;
			int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
			targetScroll = calculateScrollPercent(uiY, listTop, viewHeight) * maxScroll;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	private void toggleSelection(int index) {
		if (selectedIndices.contains(index)) {
			selectedIndices.remove(Integer.valueOf(index));
		} else {
			if (maxWishesToSelect == 1) {
				selectedIndices.clear();
				selectedIndices.add(index);
			} else {
				if (selectedIndices.size() < maxWishesToSelect) {
					selectedIndices.add(index);
				}
			}
		}
		confirmButton.visible = selectedIndices.size() == maxWishesToSelect;
	}

	private void refreshWishes() {
		List<Wish> current = WishManager.getClientWishes(dragonType);
		if (current.equals(availableWishes)) return;
		availableWishes = List.copyOf(current);
		selectedIndices.clear();
		targetScroll = currentScroll = 0;
		if (confirmButton != null) confirmButton.visible = false;
	}

	@Override
	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
