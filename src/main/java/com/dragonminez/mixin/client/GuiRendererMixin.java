package com.dragonminez.mixin.client;

import com.dragonminez.client.render.util.DMZPanorama;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow @Final private GuiRenderState renderState;

    @Inject(method = "render", at = @At("HEAD"))
    private void dmz$renderPanorama(CallbackInfo ci) {
        DMZPanorama.Frame panorama = ((DMZPanorama.StateAccess) this.renderState).dmz$getPanorama();
        if (panorama != null) {
            this.renderState.panoramaRenderState = null;
            panorama.render();
        }
    }
}
