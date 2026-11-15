package com.dragonminez.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Invokes the tooltip renderer directly, for code that has already computed its own tooltip layout.
 *
 * <h2>26.2 migration</h2>
 * 1.21's private {@code renderTooltipInternal(Font, List, int, int, ClientTooltipPositioner)} became
 * the public {@code tooltip(Font, List, int, int, ClientTooltipPositioner, Identifier, ItemStack)}:
 * two arguments were appended (the style id and the stack the tooltip belongs to). Keeping the old
 * name here made the accessor fail at apply time with
 * {@code No candidates were found matching renderTooltipInternal(...)} and aborted mod construction.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsInvoker {

	@Invoker("tooltip")
	void invokeTooltip(Font font, List<ClientTooltipComponent> components, int x, int y,
					   ClientTooltipPositioner positioner, Identifier style, ItemStack tooltipStack);
}
