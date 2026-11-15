package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.tooltip.BedrockCenteringPositionModule;
import com.dragonminez.client.gui.tooltip.PrioritizeTooltipTopPositionModule;
import com.dragonminez.client.gui.tooltip.ScrollTracker;
import com.dragonminez.client.gui.tooltip.TooltipDecor;
import com.dragonminez.client.gui.tooltip.TooltipPositionModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Hooks tooltip placement so DMZ can reposition and decorate tooltips.
 *
 * <h2>26.2 migration</h2>
 * The method this mixin targets was renamed: 1.21's {@code renderTooltipInternal(Font, List,
 * int, int, ClientTooltipPositioner)} is now {@code tooltip(Font, List, int, int,
 * ClientTooltipPositioner, Identifier, ItemStack)}. The old private name no longer exists in
 * {@code GuiGraphicsExtractor}, so every injection here failed with "Scanned 0 target(s)".
 *
 * <p>Two further shape changes:
 * <ul>
 *   <li>the positioner call is still {@code positionTooltip(int,int,int,int,int,int)} but it now
 *       receives the tooltip's occupied rect rather than a cursor point, so the argument values
 *       are forwarded untouched;</li>
 *   <li>the matrix stack is a {@code Matrix3x2fStack} directly on the extractor (no intervening
 *       {@code PoseStack#pose()}).</li>
 * </ul>
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    public abstract org.joml.Matrix3x2fStack pose();

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    /** Repositions the tooltip after vanilla has computed its default origin. */
    @Redirect(
            method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic dragonminez$moveTooltip(ClientTooltipPositioner positioner, int screenWidth, int screenHeight,
                                              int x, int y, int width, int height) {
        Vector2ic currentPosition = positioner.positionTooltip(screenWidth, screenHeight, x, y, width, height);

        for (TooltipPositionModule module : List.of(
                new PrioritizeTooltipTopPositionModule(),
                new BedrockCenteringPositionModule()
        )) {
            Optional<Vector2ic> position = module.repositionTooltip(
                    currentPosition.x(), currentPosition.y(), width, height, x, y,
                    this.guiWidth(), this.guiHeight());
            if (position.isPresent()) currentPosition = position.get();
        }

        ScrollTracker.applyScroll((GuiGraphicsExtractor) (Object) this,
                currentPosition.x(), currentPosition.y(), width, height, screenWidth, screenHeight);

        TooltipDecor.lastTooltipX = currentPosition.x();
        TooltipDecor.lastTooltipY = currentPosition.y();
        TooltipDecor.lastTooltipW = width;
        TooltipDecor.lastTooltipH = height;

        return currentPosition;
    }

    // 26.2 note: do NOT pop matrices here. vanilla's tooltip() performs exactly one
    // pushMatrix/popMatrix pair (offsets 193 / 390 in the bytecode) and the DMZ redirect above never
    // pushes, so an extra pop underflows the stack with
    //   IllegalStateException: already at the bottom of the stack.

    /** Draws the DMZ border and background after vanilla finished the tooltip body. */
    @Inject(
            method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("TAIL"))
    private void dragonminez$drawCustomDecor(Font font, List<ClientTooltipComponent> components,
                                             int mouseX, int mouseY, ClientTooltipPositioner positioner,
                                             Identifier style, ItemStack tooltipStack, CallbackInfo ci) {
        if (!TooltipDecor.hasSpecialBorder) return;
        TooltipDecor.drawShadow((GuiGraphicsExtractor) (Object) this,
                TooltipDecor.lastTooltipX, TooltipDecor.lastTooltipY,
                TooltipDecor.lastTooltipW, TooltipDecor.lastTooltipH);
    }
}
