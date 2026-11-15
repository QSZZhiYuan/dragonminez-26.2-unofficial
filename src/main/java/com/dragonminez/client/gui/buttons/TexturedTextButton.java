package com.dragonminez.client.gui.buttons;

import com.dragonminez.common.init.MainSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TexturedTextButton extends Button {

    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int normalU;
    private final int normalV;
    private final int hoverU;
    private final int hoverV;
    private final int normalTextColor;
    private final int hoverTextColor;
    private int backgroundColor;
    private boolean hasBackgroundColor;
    private final SoundEvent sound;

    public TexturedTextButton(int x, int y, int width, int height, Identifier texture,
                              int normalU, int normalV, int hoverU, int hoverV,
                              int textureWidth, int textureHeight,
                              int normalTextColor, int hoverTextColor,
                              int backgroundColor, boolean hasBackgroundColor,
                              Component message, OnPress onPress, SoundEvent sound) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.normalU = normalU;
        this.normalV = normalV;
        this.hoverU = hoverU;
        this.hoverV = hoverV;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.normalTextColor = opaqueLegacyColor(normalTextColor);
        this.hoverTextColor = opaqueLegacyColor(hoverTextColor);
        this.backgroundColor = backgroundColor;
        this.hasBackgroundColor = hasBackgroundColor;
        this.sound = sound;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (this.sound != null) {
            handler.play(SimpleSoundInstance.forUI(this.sound, 1.0F));
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();


        int u = this.isHoveredOrFocused() ? hoverU : normalU;
        int v = this.isHoveredOrFocused() ? hoverV : normalV;

        com.dragonminez.client.gui.hud.HudRender.sprite(graphics,
                new com.dragonminez.client.gui.hud.HudSprites.Sprite(texture, 256, 256, u, v, textureWidth, textureHeight),
                this.getX(), this.getY(), this.width, this.height, 0xFFFFFF, this.alpha);

        if (hasBackgroundColor) {
            graphics.fill(this.getX() + 2, this.getY() + 2,
                         this.getX() + this.width - 2, this.getY() + this.height - 2,
                         (Math.round(this.alpha * 255) << 24) | (backgroundColor & 0xFFFFFF));
        }

        int textColor = this.active ? (this.isHoveredOrFocused() ? hoverTextColor : normalTextColor) : 0xFFA0A0A0;
        textColor = (Math.round(this.alpha * ((textColor >>> 24) & 255)) << 24) | (textColor & 0xFFFFFF);
        graphics.centeredText(minecraft.font, this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2,
                textColor);
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        this.hasBackgroundColor = true;
    }

    private static int opaqueLegacyColor(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static class Builder {
        private int x, y, width, height;
        private Identifier texture;
        private int normalU, normalV;
        private int hoverU, hoverV;
        private int textureWidth, textureHeight;
        private int normalTextColor = 0xFFFFFF;
        private int hoverTextColor = 0x7CFDD6;
        private int backgroundColor = 0;
        private boolean hasBackgroundColor = false;
        private Component message = Component.empty();
        private OnPress onPress;
		private SoundEvent sound = MainSounds.PIP_MENU.get();

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder texture(Identifier texture) {
            this.texture = texture;
            return this;
        }

        public Builder textureCoords(int normalU, int normalV, int hoverU, int hoverV) {
            this.normalU = normalU;
            this.normalV = normalV;
            this.hoverU = hoverU;
            this.hoverV = hoverV;
            return this;
        }

        public Builder textureSize(int width, int height) {
            this.textureWidth = width;
            this.textureHeight = height;
            return this;
        }

        public Builder textColors(int normalColor, int hoverColor) {
            this.normalTextColor = normalColor;
            this.hoverTextColor = hoverColor;
            return this;
        }

        public Builder backgroundColor(int color) {
            this.backgroundColor = color;
            this.hasBackgroundColor = true;
            return this;
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder onPress(OnPress onPress) {
            this.onPress = onPress;
            return this;
        }

        public Builder sound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }

        public TexturedTextButton build() {
            return new TexturedTextButton(x, y, width, height, texture,
                    normalU, normalV, hoverU, hoverV,
                    textureWidth, textureHeight,
                    normalTextColor, hoverTextColor, backgroundColor, hasBackgroundColor, message, onPress, sound);
        }
    }
}
