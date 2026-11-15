package com.dragonminez.mixin.client;

import com.dragonminez.client.render.shader.KiBloomRenderer;
import com.dragonminez.client.render.shader.TransformationOutlineRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public abstract class EffectGeometryCaptureMixin {
    @Inject(method = "submitCustomGeometry", at = @At("HEAD"))
    private void dmz$captureEffects(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer draw, CallbackInfo ci) {
        KiBloomRenderer.capture(pose, type, draw);
        var player = TransformationOutlineRenderer.capturePlayer();
        if (player != null) TransformationOutlineRenderer.capture(player, pose, type, draw);
    }
}
