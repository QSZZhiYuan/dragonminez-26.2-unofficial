package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.ITTargetEntry;
import com.dragonminez.common.network.C2S.DeleteMasterC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelC2S;
import com.dragonminez.common.network.C2S.InstantTransmissionTravelToPlayerC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class InstantTransmissionScreen extends ScaledScreen {

	private static final Identifier MENU_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;
	private static final int ITEM_HEIGHT = 24;
	private static final int MAX_VISIBLE_ITEMS = 5;

	private final List<MasterEntry> destinations = new ArrayList<>();
	private final List<Integer> filteredIndices = new ArrayList<>();
	private int selectedIndex = -1;
	private final int skillLevel;
	private final String currentDimension;

	private int guiLeft, guiTop;
	private float targetScroll = 0;
	private float currentScroll = 0;
	private float maxScroll = 0;
	private boolean isScrolling = false;

	private TexturedTextButton travelButton;
	private CustomTextureButton deleteButton;
	private EditBox searchBox;

	public InstantTransmissionScreen(List<ITTargetEntry> entries, int skillLevel) {
		super(Component.literal("Instant Transmission").withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "smooth")))));
		this.skillLevel = skillLevel;
		this.currentDimension = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.level().dimension().identifier().toString() : "";
		loadDestinations(entries);
	}

	private void loadDestinations(List<ITTargetEntry> entries) {
		destinations.clear();
		for (ITTargetEntry entry : entries) destinations.add(new MasterEntry(entry.getType(), entry.getId(), entry.getName(), entry.getDimension(), entry.isReachable()));
		destinations.sort(Comparator.comparingInt((MasterEntry e) -> e.priority()).thenComparing(e -> e.name, String.CASE_INSENSITIVE_ORDER));
		applyFilter("");
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

		this.travelButton = new TexturedTextButton.Builder()
				.position(guiLeft + (PANEL_WIDTH - 80) / 2, getUiHeight() - 30)
				.size(74, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.textColors(0xFFFFFFFF, 0xFFFFD700)
				.message(tr("gui.dragonminez.travel"))
				.onPress(btn -> initiateTravel())
				.build();

		this.travelButton.visible = false;
		this.addRenderableWidget(travelButton);

		this.deleteButton = new CustomTextureButton.Builder()
				.position(guiLeft + PANEL_WIDTH - 28, getUiHeight() - 25)
				.size(14, 11)
				.texture(BUTTON_TEXTURE)
				.textureCoords(10, 0, 10, 10)
				.textureSize(10, 10)
				.onPress(btn -> deleteSelectedMaster())
				.build();

		this.deleteButton.visible = false;
		this.addRenderableWidget(deleteButton);

		this.searchBox = new EditBox(this.font, guiLeft + 13, guiTop + 170, PANEL_WIDTH - 26, 14,
				tr("gui.dragonminez.transmission.search"));
		this.searchBox.setMaxLength(64);
		this.searchBox.setHint(tr("gui.dragonminez.transmission.search"));
		this.searchBox.setResponder(this::applyFilter);
		this.addRenderableWidget(searchBox);
	}

	private void applyFilter(String query) {
		filteredIndices.clear();
		String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		for (int i = 0; i < destinations.size(); i++) {
			if (normalized.isEmpty() || destinations.get(i).name.toLowerCase(Locale.ROOT).contains(normalized)) {
				filteredIndices.add(i);
			}
		}
		selectedIndex = -1;
		targetScroll = 0;
		currentScroll = 0;
		if (travelButton != null) travelButton.visible = false;
		if (deleteButton != null) deleteButton.visible = false;
	}

	private void deleteSelectedMaster() {
		if (selectedIndex < 0 || selectedIndex >= destinations.size()) return;
		MasterEntry dest = destinations.get(selectedIndex);
		if (dest.type != ITTargetEntry.Type.MASTER) return;

		NetworkHandler.sendToServer(new DeleteMasterC2S(dest.id));
		destinations.remove(selectedIndex);
		applyFilter(searchBox == null ? "" : searchBox.getValue());
	}

	private void initiateTravel() {
		if (selectedIndex >= 0 && selectedIndex < destinations.size()) {
			MasterEntry dest = destinations.get(selectedIndex);
			if (dest.reachable) {
				if (dest.type == ITTargetEntry.Type.MASTER) NetworkHandler.sendToServer(new InstantTransmissionTravelC2S(dest.id));
				else NetworkHandler.sendToServer(new InstantTransmissionTravelToPlayerC2S(UUID.fromString(dest.id)));
				this.onClose();
			}
		}
	}


	// 26.2: vanilla already calls extractBackground once per frame from the final
	// extractRenderStateWithTooltipAndSubtitles; calling it again here caused
	//   IllegalStateException: Can only blur once per frame
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {


		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		Component title = tr("gui.dragonminez.transmission.title")
				.withStyle(style -> style.withColor(0xFFFFD700).withBold(true));
		TextUtil.drawCenteredStringWithBorder(graphics, this.font,
				title, getUiWidth() / 2, guiTop + 18, 0xFFFFD700);

		renderMasterList(graphics, uiMouseX, uiMouseY);
		super.extractRenderState(graphics, uiMouseX, uiMouseY, partialTick);

		endUiScale(graphics);
	}

	private void renderMasterList(GuiGraphicsExtractor graphics, int uiMouseX, int uiMouseY) {
		int listLeft = guiLeft + 10;
		int listTop = guiTop + 35;
		int listWidth = PANEL_WIDTH - 25;
		int viewHeight = MAX_VISIBLE_ITEMS * ITEM_HEIGHT;
		int totalHeight = filteredIndices.size() * ITEM_HEIGHT;

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

		for (int i = 0; i < filteredIndices.size(); i++) {
			int itemY = listTop + (i * ITEM_HEIGHT);

			if (itemY + ITEM_HEIGHT >= listTop + currentScroll && itemY <= listTop + viewHeight + currentScroll) {
				int destinationIndex = filteredIndices.get(i);
				MasterEntry dest = destinations.get(destinationIndex);
				boolean isSelected = (destinationIndex == selectedIndex);

				if (dest.reachable) {
					boolean isHovered = uiMouseX >= listLeft && uiMouseX < listLeft + listWidth &&
							uiMouseY >= itemY - currentScroll && uiMouseY < itemY + ITEM_HEIGHT - currentScroll;

					int color = isSelected ? 0x80D4AF37 : (isHovered ? 0x80555555 : 0x00000000);
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, color);
					if (isSelected) graphics.outline(listLeft, itemY, listWidth, ITEM_HEIGHT, 0xFFFFD700);
				} else {
					graphics.fill(listLeft, itemY, listLeft + listWidth, itemY + ITEM_HEIGHT, 0x30000000);
				}

				int textColor = dest.reachable ? colorForType(dest.type) : 0xFF9CA3AF;
				Component textToDraw = txt(dest.name).copy()
						.withStyle(ChatFormatting.BOLD)
						.withStyle(style -> style.withColor(textColor & 0xFFFFFF));
				TextUtil.drawStringWithBorder(graphics, this.font, textToDraw, listLeft + 10, itemY + 8, textColor);
			}
		}

		graphics.pose().popMatrix();
		graphics.disableScissor();

		if (maxScroll > 0) renderScrollbar(graphics, listTop, viewHeight, totalHeight);
	}

	private void renderScrollbar(GuiGraphicsExtractor graphics, int listTop, int viewHeight, int totalHeight) {
		int scrollBarX = guiLeft + PANEL_WIDTH - 12;
		graphics.fill(scrollBarX, listTop, scrollBarX + 3, listTop + viewHeight, 0xFF333333);

		float scrollPercent = currentScroll / maxScroll;
		float visiblePercent = (float) viewHeight / totalHeight;
		int indicatorHeight = Math.max(20, (int) (viewHeight * visiblePercent));
		int indicatorY = listTop + (int) ((viewHeight - indicatorHeight) * scrollPercent);

		graphics.fill(scrollBarX, indicatorY, scrollBarX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
	}

	private float calculateScrollPercent(double uiY, int startY, int viewHeight) {
		return Mth.clamp((float)(uiY - startY) / viewHeight, 0.0f, 1.0f);
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

		if (uiX >= listLeft && uiX < listLeft + listWidth && uiY >= listTop && uiY < listTop + viewHeight) {
			int index = (int) (uiY - listTop + currentScroll) / ITEM_HEIGHT;
			if (index >= 0 && index < filteredIndices.size()) {
				int destinationIndex = filteredIndices.get(index);
				MasterEntry dest = destinations.get(destinationIndex);
				if (dest.reachable) {
					selectDestination(destinationIndex);
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MainSounds.UI_MENU_SWITCH.get(), 1.0F));
				}
				return true;
			}
		}
		return false;
	}

	private void selectDestination(int index) {
		if (this.selectedIndex == index) return;
		this.selectedIndex = index;
		this.travelButton.visible = true;
		this.deleteButton.visible = destinations.get(index).type == ITTargetEntry.Type.MASTER;
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
			targetScroll = calculateScrollPercent(uiY, guiTop + 35, MAX_VISIBLE_ITEMS * ITEM_HEIGHT) * maxScroll;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	private static int colorForType(ITTargetEntry.Type type) {
		return switch (type) {
			case MASTER -> 0xFF20E0FF;
			case PARTY -> 0xFFFFD700;
			case EXTERNAL -> 0xFFFFFFFF;
		};
	}

	private static class MasterEntry {
		ITTargetEntry.Type type;
		String id;
		String name;
		String dimension;
		boolean reachable;

		public MasterEntry(ITTargetEntry.Type type, String id, String name, String dimension, boolean reachable) {
			this.type = type;
			this.id = id;
			this.name = name;
			this.dimension = dimension;
			this.reachable = reachable;
		}

		int priority() {
			return switch (type) {
				case MASTER -> 1;
				case PARTY -> 2;
				case EXTERNAL -> 3;
			};
		}
	}
}
