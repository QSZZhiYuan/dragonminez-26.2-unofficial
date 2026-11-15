package com.dragonminez.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Suppresses vanilla's menu music so DMZ can drive its own track selection.
 *
 * <h2>26.2 migration</h2>
 * {@code SoundManager#play(SoundInstance)} used to return {@code void}; in 26.2 it returns
 * {@link SoundEngine.PlayResult}. A {@code CallbackInfo} injection therefore fails with
 * {@code CallbackInfoReturnable is required}, so this uses the returnable form and supplies the
 * "nothing was played" result on the cancelled path.
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {

	@Unique
	private static final Identifier MENU_MUSIC_RESOURCE = Identifier.fromNamespaceAndPath("minecraft", "music.menu");

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void dragonminez$onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (sound.getIdentifier().equals(MENU_MUSIC_RESOURCE)) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}
