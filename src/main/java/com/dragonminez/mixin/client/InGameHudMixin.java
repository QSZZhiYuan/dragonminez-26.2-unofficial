package com.dragonminez.mixin.client;

import com.dragonminez.Reference;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class InGameHudMixin {
	private static final Identifier DMZ$BLOCK_SHIELD = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/block_shield.png");
	private static final int DMZ$SHIELD_SIZE = 18;

	// 26.2: Gui#renderCrosshair moved to Hud#extractCrosshair and is now a render-state extractor.
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void dragonminez$pre_renderCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (dragonminez$isBlocking() && dragonminez$crosshairVisible()) {
			int x = (guiGraphics.guiWidth() - DMZ$SHIELD_SIZE) / 2;
			int y = (guiGraphics.guiHeight() - DMZ$SHIELD_SIZE) / 2;
			guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DMZ$BLOCK_SHIELD, x, y, 0.0F, 0.0F, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE, DMZ$SHIELD_SIZE);
			ci.cancel();
			return;
		}
	}

	@Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
	private boolean dragonminez$shoulderCrosshair(CameraType cameraType) {
		return cameraType.isFirstPerson()
				|| (cameraType == CameraType.THIRD_PERSON_BACK && OverShoulderCamera.isRunning());
	}

    @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0))
    private void dragonminez$tintTargetCrosshair(GuiGraphicsExtractor graphics,
            com.mojang.blaze3d.pipeline.RenderPipeline pipeline, Identifier sprite,
            int x, int y, int width, int height) {
        if (((Minecraft_DMZ) Minecraft.getInstance()).hasTargetsInReach()) {
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    sprite, x, y, width, height, 0xFFFF3333);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height);
        }
    }

	@Inject(method = "extractCrosshair", at = @At("TAIL"))
	private void dragonminez$post_renderCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
	}

	private static boolean dragonminez$isBlocking() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> data.getStatus().isBlocking())
				.orElse(false);
	}

	private static boolean dragonminez$crosshairVisible() {
		Minecraft mc = Minecraft.getInstance();
		CameraType cameraType = mc.options.getCameraType();
		if (!cameraType.isFirstPerson()
				&& (cameraType != CameraType.THIRD_PERSON_BACK || !OverShoulderCamera.isRunning())) return false;
		return mc.gameMode == null || mc.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.SPECTATOR;
	}
}

