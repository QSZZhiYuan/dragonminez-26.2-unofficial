package com.dragonminez.client.gui.buttons;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SwitchButton extends Button {

    private boolean on;
    private static final Identifier BUTTONS_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/gui/buttons/characterbuttons.png");

    public SwitchButton(int x, int y, boolean active, Component message, OnPress onPress) {
        super(x, y, 20, 10, message, onPress, DEFAULT_NARRATION);
        this.on = active;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public void toggle() {
        this.on = !this.on;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int buttonX = 122;
        int buttonY = on ? 0 : 10;

        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BUTTONS_TEXTURE, this.getX(), this.getY(), buttonX, buttonY, 20, 10, 256, 256);

        // The original sprite has English ON/OFF baked into it. Keep the slider and
        // frame, but replace just the lettering with the selected language.
        int textX = this.getX() + (on ? 2 : 8);
        graphics.fill(textX, this.getY() + 2, textX + 10, this.getY() + 8,
                on ? 0xFF004400 : 0xFF440000);
        var font = Minecraft.getInstance().font;
        var label = Component.translatable(on ? "gui.dragonminez.config.on" : "gui.dragonminez.config.off");
        float scale = Math.min(0.5f, 10.0f / Math.max(1, font.width(label)));
        graphics.pose().pushMatrix();
        graphics.pose().translate(textX + 5.0f, this.getY() + 5.0f);
        graphics.pose().scale(scale, scale);
        graphics.text(font, label, -font.width(label) / 2, -font.lineHeight / 2, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();
    }
}


