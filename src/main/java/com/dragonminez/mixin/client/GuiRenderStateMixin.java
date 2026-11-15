package com.dragonminez.mixin.client;

import com.dragonminez.client.render.util.DMZPanorama;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements DMZPanorama.StateAccess {
    @Unique private DMZPanorama.Frame dmz$panorama;

    @Override
    public DMZPanorama.Frame dmz$getPanorama() {
        return dmz$panorama;
    }

    @Override
    public void dmz$setPanorama(DMZPanorama.Frame frame) {
        dmz$panorama = frame;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void dmz$resetPanorama(CallbackInfo ci) {
        dmz$panorama = null;
    }
}
