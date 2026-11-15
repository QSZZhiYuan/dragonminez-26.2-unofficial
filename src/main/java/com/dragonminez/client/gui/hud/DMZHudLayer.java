package com.dragonminez.client.gui.hud;

import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * DMZ-owned HUD layer type.
 *
 * <p>Minecraft 26.2 removed {@code net.minecraft.client.gui.GuiLayer} and its nested
 * {@code Layer} interface; the HUD is now driven by {@code net.minecraft.client.gui.Hud}
 * through {@code GuiGraphicsExtractor}, and NeoForge exposes
 * {@code net.neoforged.neoforge.client.gui.GuiLayer} as the declarative replacement, with
 * {@code RegisterGuiLayersEvent#registerAbove(Identifier, Identifier, GuiLayer)} for ordering.
 *
 * <p>{@code GuiLayer} already carries the exact old {@code GuiLayer} signature
 * ({@code void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker)}), so this
 * alias keeps the DMZ code self-documenting without changing a single call site: every
 * {@code DMZHudLayer} constant is directly assignable to the {@code GuiLayer} parameter that
 * {@code ModClientEvents#registerGuiOverlays} passes to {@code registerAbove}.
 *
 * <p>This interface intentionally stays functional (it declares no abstract methods of its own),
 * so the existing lambda form keeps working:
 * {@code public static final DMZHudLayer HUD_X = (guiGraphics, deltaTracker) -> { ... };}
 */
@FunctionalInterface
public interface DMZHudLayer extends GuiLayer {
}
