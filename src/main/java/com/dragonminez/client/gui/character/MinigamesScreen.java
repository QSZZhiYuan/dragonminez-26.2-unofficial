package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.minigames.ControlGameScreen;
import com.dragonminez.client.gui.character.minigames.GravityGameScreen;
import com.dragonminez.client.gui.character.minigames.MemoryGameScreen;
import com.dragonminez.client.gui.character.minigames.PrecisionGameScreen;
import com.dragonminez.client.gui.character.minigames.RythmGameScreen;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.SummonPlayerShadowDummyC2S;
import com.dragonminez.client.gui.character.util.BaseMenuScreen;
import com.dragonminez.client.util.ScrollbarState;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MinigamesScreen extends BaseMenuScreen {

	private static final Identifier MENU_BIG = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");
	private static final Identifier CHAR_BUTTONS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final boolean DEBUG_UNLOCK_ALL = false;
	private static final String[] MINIGAMES = {"rhythm", "control", "memory", "precision", "gravity", "shadowdummy"};
	private static final int LIST_ITEM_HEIGHT = 20;

	private int selectedIndex = 0;
	private float descScrollY = 0;
	private float targetDescScrollY = 0;
	private int descContentHeight = 0;
	private int descViewportHeight = 0;
	private final ScrollbarState descBar = new ScrollbarState();
	private int shadowDummyPercent = 50;

	private TexturedTextButton playButton;
	private CustomTextureButton shadowDecBtn;
	private CustomTextureButton shadowIncBtn;

	public MinigamesScreen() {
		super(Component.literal("hub"));
	}

	@Override
	protected void init() {
		super.init();
		initPlayButton();
	}

	private void initPlayButton() {
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int playY = (centerY - 105) + 213 - 28;

		playButton = new TexturedTextButton.Builder()
				.position(rightPanelX + 18, playY)
				.size(105, 20)
				.texture(BUTTON_TEXTURE)
				.textureCoords(0, 50, 0, 50)
				.textureSize(105, 20)
				.message(tr("gui.dragonminez.minigames.play"))
				.onPress(b -> playSelected())
				.build();
		this.addRenderableWidget(playButton);

		shadowDecBtn = new CustomTextureButton.Builder()
				.position(rightPanelX + 14, centerY - 8)
				.size(14, 11)
				.texture(CHAR_BUTTONS)
				.textureCoords(32, 0, 32, 14)
				.textureSize(8, 14)
				.onPress(b -> adjustShadowPercent(-5))
				.build();
		this.addRenderableWidget(shadowDecBtn);

		shadowIncBtn = new CustomTextureButton.Builder()
				.position(rightPanelX + 113, centerY - 8)
				.size(14, 11)
				.texture(CHAR_BUTTONS)
				.textureCoords(20, 0, 20, 14)
				.textureSize(8, 14)
				.onPress(b -> adjustShadowPercent(5))
				.build();
		this.addRenderableWidget(shadowIncBtn);

		refreshPlayButton();
	}

	private void adjustShadowPercent(int delta) {
		shadowDummyPercent = Mth.clamp(shadowDummyPercent + delta, 25, 75);
	}

	private boolean isShadowDummyEntry(int index) {
		return "shadowdummy".equals(MINIGAMES[index]);
	}

	private boolean hasAccess(int index) {
		if (isShadowDummyEntry(index)) return hasShadowDummyAccess();
		if (DEBUG_UNLOCK_ALL) return true;
		String id = MINIGAMES[index];
		TrainingConfig.MinigameSettings settings = ConfigManager.getTrainingConfig().getSettings(id);
		if (settings.isUnlockedByDefault()) return true;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player)
				.map(d -> d.getCharacter().isMinigameKnown(id)).orElse(false);
	}

	private boolean hasShadowDummyAccess() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d ->
				d.getStatus().getShadowDummyKillCount() > 0
				&& d.getSkills().hasSkill("kicontrol")
				&& d.getSkills().getSkillLevel("kimanipulation") >= 5
		).orElse(false);
	}

	private String master(int index) {
		return ConfigManager.getTrainingConfig().getSettings(MINIGAMES[index]).getMasterName();
	}

	private void refreshPlayButton() {
		boolean isShadow = isShadowDummyEntry(selectedIndex);
		boolean access = hasAccess(selectedIndex);

		if (playButton != null) {
			playButton.active = access;
			playButton.visible = access;
			if (isShadow) playButton.setMessage(tr("gui.dragonminez.shadow_dummy.summon"));
			else playButton.setMessage(tr("gui.dragonminez.minigames.play"));
		}
		if (shadowDecBtn != null) shadowDecBtn.visible = isShadow && access;
		if (shadowIncBtn != null) shadowIncBtn.visible = isShadow && access;
	}

	private void playSelected() {
		if (!hasAccess(selectedIndex) || this.minecraft == null) return;
		if (isShadowDummyEntry(selectedIndex)) {
			NetworkHandler.sendToServer(new SummonPlayerShadowDummyC2S(shadowDummyPercent));
			return;
		}
		switch (MINIGAMES[selectedIndex]) {
			case "rhythm" -> this.minecraft.gui.setScreen(new RythmGameScreen());
			case "control" -> this.minecraft.gui.setScreen(new ControlGameScreen());
			case "memory" -> this.minecraft.gui.setScreen(new MemoryGameScreen());
			case "precision" -> this.minecraft.gui.setScreen(new PrecisionGameScreen());
			case "gravity" -> this.minecraft.gui.setScreen(new GravityGameScreen());
			default -> {}
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.descScrollY = Mth.lerp(0.5f, this.descScrollY, this.targetDescScrollY);
	}


	/**
	 * 26.2: Screen#extractRenderStateWithTooltipAndSubtitles is final and already calls
	 * extractBackground once per frame, so calling it again from extractRenderState made
	 * the blur request fire twice and threw
	 *   IllegalStateException: Can only blur once per frame
	 * The guard DMZ wanted (skip the backdrop during the open/close transition) belongs
	 * here instead, where vanilla still calls it exactly once.
	 */
	@Override
	public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
			int mouseX, int mouseY, float partialTick) {
		if (isNotAnimating()) {
			super.extractBackground(graphics, mouseX, mouseY, partialTick);
		}
	}
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

		int uiMouseX = (int) Math.round(toUiX(mouseX));
		int uiMouseY = (int) Math.round(toUiY(mouseY));

		beginUiScale(graphics);
		applyZoom(graphics, partialTick);
		renderPlayerModel(graphics, getUiWidth() / 2 + 5, getUiHeight() / 2 + 70, 75, uiMouseX, uiMouseY);

		int leftOffset = getLeftPanelSwitchOffset(partialTick);
		graphics.pose().pushMatrix();
		graphics.pose().translate(leftOffset, 0);
		renderLeftPanel(graphics, uiMouseX - leftOffset, uiMouseY);
		graphics.pose().popMatrix();

		int rightOffset = getRightPanelSwitchOffset(partialTick);
		graphics.pose().pushMatrix();
		graphics.pose().translate(rightOffset, 0);
		renderRightPanel(graphics, uiMouseX - rightOffset, uiMouseY);
		graphics.pose().popMatrix();

		int rightBase = getUiWidth() - 158;
		if (playButton != null) playButton.setX(rightBase + 18 + rightOffset);
		if (shadowDecBtn != null) shadowDecBtn.setX(rightBase + 14 + rightOffset);
		if (shadowIncBtn != null) shadowIncBtn.setX(rightBase + 113 + rightOffset);

		super.extractRenderState(graphics, uiMouseX, uiMouseY, partialTick);
		endUiScale(graphics);
	}

	private void renderLeftPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int leftPanelY = centerY - 105;

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_BIG, 12, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_BIG, 29, centerY - 95, 142, 22, 107, 21, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigames.list").withStyle(ChatFormatting.BOLD),
				leftPanelX + 70, leftPanelY + 17, 0xFFFFD700);

		int startY = leftPanelY + 38;
		for (int i = 0; i < MINIGAMES.length; i++) {
			int itemY = startY + (i * LIST_ITEM_HEIGHT);
			boolean selected = i == selectedIndex;
			boolean hovered = isOverListItem(mouseX, mouseY, i);

			int color;
			if (selected) color = 0xFFFFD700;
			else if (hovered) color = 0xFF7CFDD6;
			else if (hasAccess(i)) color = 0xFFFFFFFF;
			else color = 0xFF888888;

			Component name = tr("gui.dragonminez.minigame." + MINIGAMES[i]);
			TextUtil.drawStringWithBorder(graphics, this.font, name, leftPanelX + 20, itemY, color);
		}
	}

	private boolean isOverListItem(double uiMouseX, double uiMouseY, int index) {
		int leftPanelX = 12;
		int centerY = getUiHeight() / 2;
		int startY = (centerY - 105) + 38;
		int itemY = startY + (index * LIST_ITEM_HEIGHT);
		return uiMouseX >= leftPanelX + 18 && uiMouseX <= leftPanelX + 123
				&& uiMouseY >= itemY - 2 && uiMouseY <= itemY + 10;
	}

	private void renderRightPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int rightPanelY = centerY - 105;

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_BIG, getUiWidth() - 158, centerY - 105, 0, 0, 141, 213, 256, 256);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, MENU_BIG, getUiWidth() - 141, centerY - 95, 142, 22, 107, 21, 256, 256);

		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.minigame." + MINIGAMES[selectedIndex]).withStyle(ChatFormatting.BOLD),
				rightPanelX + 70, rightPanelY + 17, 0xFFFFD700);

		if (isShadowDummyEntry(selectedIndex)) {
			renderShadowDummyPanel(graphics, rightPanelX, rightPanelY, centerY);
		} else {
			renderDescription(graphics, rightPanelX, rightPanelY);
			if (!hasAccess(selectedIndex)) {
				int hintY = rightPanelY + 213 - 28 + 6;
				TextUtil.drawCenteredStringWithBorder(graphics, this.font,
						tr("gui.dragonminez.minigames.learn").append(" ").append(tr("entity.dragonminez.questnpc." + master(selectedIndex))),
						rightPanelX + 70, hintY, 0xFFFF7777);
			}
		}
	}

	private void renderShadowDummyPanel(GuiGraphicsExtractor graphics, int panelX, int panelY, int centerY) {
		boolean access = hasShadowDummyAccess();
		int hitColor = 0xFF55FF55, missColor = 0xFFFF5555;

		if (!access) {
			int y = panelY + 42;
			y = drawWrappedCentered(graphics, tr("gui.dragonminez.shadow_dummy.req_title"), panelX, y, 0xFFFF7777) + 4;
			y = drawWrappedCentered(graphics, tr("gui.dragonminez.shadow_dummy.req_kill"), panelX, y, clientShadowDummyKillCount() > 0 ? hitColor : missColor) + 3;
			y = drawWrappedCentered(graphics, tr("gui.dragonminez.shadow_dummy.req_kicontrol"), panelX, y, clientHasSkill("kicontrol") ? hitColor : missColor) + 3;
			drawWrappedCentered(graphics, tr("gui.dragonminez.shadow_dummy.req_kimanip"), panelX, y, clientSkillLevel("kimanipulation") >= 5 ? hitColor : missColor);
			return;
		}

		drawWrappedCentered(graphics, tr("gui.dragonminez.shadow_dummy.desc_short"), panelX, panelY + 40, 0xFFE0E0E0);
		TextUtil.drawCenteredStringWithBorder(graphics, this.font, tr("gui.dragonminez.shadow_dummy.percent", shadowDummyPercent), panelX + 70, centerY - 4, 0xFFFFD700);
	}

	private int drawWrappedCentered(GuiGraphicsExtractor graphics, FormattedText text, int panelX, int startY, int color) {
		int wrapWidth = (int) (118 / 0.75f);
		List<FormattedCharSequence> lines = this.font.split(text, wrapWidth);
		int cxScaled = (int) ((panelX + 70) / 0.75f);

		graphics.pose().pushMatrix();
		graphics.pose().scale(0.75f, 0.75f);
		int y = startY;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, cxScaled, (int) (y / 0.75f), color);
			y += 9;
		}
		graphics.pose().popMatrix();
		return y;
	}

	private int clientShadowDummyKillCount() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return 0;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> d.getStatus().getShadowDummyKillCount()).orElse(0);
	}

	private boolean clientHasSkill(String skill) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> d.getSkills().hasSkill(skill)).orElse(false);
	}

	private int clientSkillLevel(String skill) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return 0;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).map(d -> d.getSkills().getSkillLevel(skill)).orElse(0);
	}

	private void renderDescription(GuiGraphicsExtractor graphics, int panelX, int panelY) {
		int textX = panelX + 12;
		int top = panelY + 40;
		descViewportHeight = 142;
		int wrapWidth = (int) (118 / 0.75f);

		FormattedText desc = tr("gui.dragonminez.minigame." + MINIGAMES[selectedIndex] + ".desc");
		List<FormattedCharSequence> lines = this.font.split(desc, wrapWidth);
		int lineHeight = 9;
		descContentHeight = lines.size() * lineHeight;

		graphics.enableScissor(
				toScissorCoord(textX - 2),
				toScissorCoord(top),
				toScissorCoord(panelX + 130),
				toScissorCoord(top + descViewportHeight)
		);

		graphics.pose().pushMatrix();
		graphics.pose().scale(0.75f, 0.75f);
		int drawY = top - (int) descScrollY;
		for (FormattedCharSequence line : lines) {
			TextUtil.drawStringWithBorder(graphics, this.font, line,
					(int) (textX / 0.75f), (int) (drawY / 0.75f), 0xFFE0E0E0);
			drawY += lineHeight;
		}
		graphics.pose().popMatrix();
		graphics.disableScissor();

		int maxScroll = Math.max(0, descContentHeight - descViewportHeight);
		descBar.update(panelX + 130, 3, top, descViewportHeight, maxScroll);
		if (maxScroll > 0) {
			int barX = panelX + 130;
			graphics.fill(barX, top, barX + 3, top + descViewportHeight, 0xFF333333);
			float percent = Mth.clamp(descScrollY / maxScroll, 0f, 1f);
			int indicatorH = Math.max(20, (int) (descViewportHeight * ((float) descViewportHeight / descContentHeight)));
			int indicatorY = top + (int) ((descViewportHeight - indicatorH) * percent);
			graphics.fill(barX, indicatorY, barX + 3, indicatorY + indicatorH, 0xFFAAAAAA);
		}
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);

		if (descBar.tryStartDrag(uiMouseX, uiMouseY)) {
			targetDescScrollY = descBar.scrollFor(uiMouseY);
			return true;
		}

		for (int i = 0; i < MINIGAMES.length; i++) {
			if (isOverListItem(uiMouseX, uiMouseY, i)) {
				if (selectedIndex != i) {
					selectedIndex = i;
					targetDescScrollY = 0;
					descScrollY = 0;
					refreshPlayButton();
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (descBar.isDragging()) {
			targetDescScrollY = descBar.scrollFor(toUiY(mouseY));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
		if (descBar.isDragging()) {
			descBar.stopDrag();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double uiMouseX = toUiX(mouseX);
		double uiMouseY = toUiY(mouseY);
		int rightPanelX = getUiWidth() - 158;
		int centerY = getUiHeight() / 2;
		int top = (centerY - 105) + 35;

		if (uiMouseX >= rightPanelX && uiMouseX <= rightPanelX + 141 && uiMouseY >= top && uiMouseY <= top + descViewportHeight) {
			int maxScroll = Math.max(0, descContentHeight - descViewportHeight);
			targetDescScrollY = Mth.clamp(targetDescScrollY - (float) (scrollY * 12.0), 0f, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void renderPlayerModel(GuiGraphicsExtractor graphics, int x, int y, int scale, float mouseX, float mouseY) {
		LivingEntity player = this.minecraft.player;
		if (player == null) return;

		int adjustedScale = getAdjustedModelScale(scale);

		float xRotation = (float) Math.atan((y - mouseY) / 40.0F);
		float yRotation = (float) Math.atan((x - mouseX) / 40.0F);

		Quaternionf pose = (new Quaternionf()).rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = (new Quaternionf()).rotateX(xRotation * 20.0F * ((float) Math.PI / 180F));
		pose.mul(cameraOrientation);

		float yBodyRotO = player.yBodyRot;
		float yRotO = player.getYRot();
		float xRotO = player.getXRot();
		float yHeadRotO = player.yHeadRotO;
		float yHeadRot = player.yHeadRot;

		player.yBodyRot = 180.0F + yRotation * 20.0F;
		player.setYRot(180.0F + yRotation * 40.0F);
		player.setXRot(-xRotation * 20.0F);
		player.yHeadRot = player.getYRot();
		player.yHeadRotO = player.getYRot();

		graphics.pose().pushMatrix();
		EntityPreviewRenderContext.renderEntityInInventory(graphics, x, y, adjustedScale, new org.joml.Vector3f(0.0F, 0.0F, 0.0F), pose, cameraOrientation, player);
		graphics.pose().popMatrix();

		player.yBodyRot = yBodyRotO;
		player.setYRot(yRotO);
		player.setXRot(xRotO);
		player.yHeadRotO = yHeadRotO;
		player.yHeadRot = yHeadRot;
	}
}

