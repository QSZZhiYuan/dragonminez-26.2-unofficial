package com.dragonminez.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the "experimental features" warning badge on the world-selection entries.
 *
 * <h2>26.2 migration</h2>
 * Two problems made this mixin abort the whole single-player world list:
 *
 * <ul>
 *   <li>{@code remap = false} — this is a Minecraft method reached through the normal (remapped)
 *       namespace, so opting out of remapping left mixin looking for a name that does not exist at
 *       runtime. Only mod-owned methods may use {@code remap = false}.</li>
 *   <li>the parameter list was {@code (GuiGraphicsExtractor, int, int, int, int)}; 26.2 declares
 *       {@code renderExperimentalWarning(GuiGraphicsExtractor, int, int, boolean)}. A mismatched
 *       descriptor fails with
 *       {@code InvalidInjectionException: Invalid descriptor ...}.</li>
 * </ul>
 *
 * Because the failure happens while {@code WorldListEntry} is being loaded, the exception surfaces
 * as "the single-player screen has no entries at all" rather than as a crash.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldSelectionListMixin {

	@Inject(method = "renderExperimentalWarning", at = @At("HEAD"), cancellable = true)
	private void dragonminez$renderExperimentalWarning(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
													   boolean experimental, CallbackInfo ci) {
		ci.cancel();
	}
}
