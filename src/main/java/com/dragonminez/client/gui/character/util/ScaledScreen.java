package com.dragonminez.client.gui.character.util;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.UnblurredScreen;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ScaledScreen extends UnblurredScreen {
	private static final float MIN_MENU_SCALE_MULTIPLIER = 0.25f;
	private static final float MAX_MENU_SCALE_MULTIPLIER = 3.0f;

	protected static final Identifier DMZ_FONT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private float uiScale = 1.0f;
	private int uiWidth;
	private int uiHeight;

	private int cachedGuiWidth = -1;
	private int cachedGuiHeight = -1;
	private double cachedPixelScale = Double.NaN;
	private float cachedMultiplier = Float.NaN;

	protected ScaledScreen(Component title) {
		super(title);
	}

	/**
	 * Preserve Screen.render's 1.20.1 behavior used by every DMZ menu. In 1.21
	 * vanilla added renderBackground here, which blurs custom panoramas, models,
	 * and panels that DMZ deliberately renders before calling super.render.
	 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		for (Renderable renderable : this.renderables) {
			renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}
	}

	protected void updateUiScale() {
		if (this.minecraft == null) {
			uiScale = 1.0f;
			uiWidth = this.width;
			uiHeight = this.height;
			return;
		}

		Window window = this.minecraft.getWindow();
		int currentWidth = window.getGuiScaledWidth();
		int currentHeight = window.getGuiScaledHeight();
		float multiplier = getMenuScaleMultiplier();
		double pixelScale = window.getGuiScale();

		if (currentWidth == cachedGuiWidth && currentHeight == cachedGuiHeight
				&& pixelScale == cachedPixelScale && multiplier == cachedMultiplier) return;

		float newScale = calculateUiScale(window, multiplier);
		if (newScale <= 0.0f || Float.isNaN(newScale) || Float.isInfinite(newScale)) newScale = 1.0f;
		// Align bitmap texels to physical pixels. A fractional GUI scale is needed
		// when vanilla's automatic scale leaves less than 240 logical pixels high
		// (for example 1280x720 at GUI scale 4), but must still stay crisp.
		newScale = (float) (Math.max(1.0, Math.floor(newScale * pixelScale + 0.0001)) / pixelScale);

		uiScale = newScale;
		uiWidth = Math.max(1, Math.round(currentWidth / uiScale));
		uiHeight = Math.max(1, Math.round(currentHeight / uiScale));

		cachedGuiWidth = currentWidth;
		cachedGuiHeight = currentHeight;
		cachedPixelScale = pixelScale;
		cachedMultiplier = multiplier;
	}

	private float calculateUiScale(Window window, float multiplier) {
		float availableScale = getAvailableScale(window);
		float dynamicScale = computeDynamicScale(availableScale);
		float desiredScale = dynamicScale * multiplier;
		float minScale = Math.min(getMinUiScale(), availableScale);
		return clamp(desiredScale, minScale, availableScale);
	}

	protected float computeDynamicScale(float availableScale) {
		return (float) Math.sqrt(availableScale);
	}

	protected float getMinUiScale() {
		return 1.0f;
	}

	private float getAvailableScale(Window window) {
		int guiWidth = Math.max(1, window.getGuiScaledWidth());
		int guiHeight = Math.max(1, window.getGuiScaledHeight());
		float widthScale = guiWidth / (float) getMinGuiWidth();
		float heightScale = guiHeight / (float) getMinGuiHeight();
		float availableScale = Math.min(widthScale, heightScale);

		if (!Float.isFinite(availableScale) || availableScale <= 0.0f) {
			return 1.0f;
		}

		return availableScale;
	}

	private float getMenuScaleMultiplier() {
		float multiplier = ConfigManager.getUserConfig().getMenuScaleMultiplier();
		if (!Float.isFinite(multiplier)) {
			return 1.0f;
		}
		return clamp(multiplier, MIN_MENU_SCALE_MULTIPLIER, MAX_MENU_SCALE_MULTIPLIER);
	}

	private float clamp(float value, float min, float max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	protected int getMinGuiWidth() {
		return 320;
	}

	protected int getMinGuiHeight() {
		return 240;
	}

	protected float getUiScale() {
		updateUiScale();
		return uiScale;
	}

	protected int getUiWidth() {
		updateUiScale();
		return uiWidth;
	}

	protected int getUiHeight() {
		updateUiScale();
		return uiHeight;
	}

	protected double toUiX(double mouseX) {
		updateUiScale();
		return mouseX / uiScale;
	}

	protected double toUiY(double mouseY) {
		updateUiScale();
		return mouseY / uiScale;
	}

	/** 26.2 transforms scissor bounds with the current GUI pose, including uiScale. */
	protected int toScissorCoord(double uiCoord) {
		return (int) Math.round(uiCoord);
	}

	protected void beginUiScale(GuiGraphicsExtractor graphics) {
		updateUiScale();
		graphics.pose().pushMatrix();
		graphics.pose().scale(uiScale, uiScale);
	}

	protected void endUiScale(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		return super.mouseClicked(toUiEvent(event), doubleClick);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
		return super.mouseReleased(toUiEvent(event));
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
		return super.mouseDragged(toUiEvent(event), dragX / getUiScale(), dragY / getUiScale());
	}

	private net.minecraft.client.input.MouseButtonEvent toUiEvent(net.minecraft.client.input.MouseButtonEvent event) {
		return new net.minecraft.client.input.MouseButtonEvent(toUiX(event.x()), toUiY(event.y()), event.buttonInfo());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return super.mouseScrolled(toUiX(mouseX), toUiY(mouseY), scrollX, scrollY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(toUiX(mouseX), toUiY(mouseY));
	}

	public MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}

	public MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(DMZ_FONT)));
	}
}

