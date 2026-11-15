package com.dragonminez.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Restores the custom-screen rendering behaviour DMZ relied on in 1.20.1.
 *
 * <h2>26.2 migration</h2>
 * The whole GUI render contract was renamed: {@code Screen#render} became
 * {@link Screen#extractRenderState}, {@code renderBackground} became {@link Screen#extractBackground},
 * {@code renderTransparentBackground} became {@link Screen#extractTransparentBackground}, and
 * {@link Renderable} implementations now expose {@code extractRenderState} instead of {@code render}.
 * 26.2 collects GUI draw state first and rasterises it later, so "render" is now "extract".
 */
public abstract class UnblurredScreen extends Screen {
	protected UnblurredScreen(Component title) {
		super(title);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		for (Renderable renderable : this.renderables) {
			renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// 1.20.1 used the transparent in-world shade here. 1.21 added a post-process blur that also
		// blurred DMZ's already-rendered pixel panels, so the transparent variant is kept.
		this.extractTransparentBackground(graphics);
	}
}
