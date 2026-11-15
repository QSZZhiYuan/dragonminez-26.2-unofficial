package com.dragonminez.client.systems.taiyoken;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.FullscreenPostEffect;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Upstream solar-flare palette and continuous fade, using a live 26.2 uniform buffer. */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class TaiyokenFlashHandler {
    private static final FullscreenPostEffect EFFECT = new FullscreenPostEffect("taiyoken_flash");

    private TaiyokenFlashHandler() {}

    @SubscribeEvent
    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || !TaiyokenBlindState.isActive()) return;
        float progress = TaiyokenBlindState.getProgress();
        float intensity = progress < 0.25f ? progress * 4.0f : 1.0f;
        boolean invert = Boolean.TRUE.equals(ConfigManager.getUserConfig().getTaiyokenInvertPalette());
        EFFECT.process(intensity, invert ? 1.0f : 0.0f, 0.0f, 0.0f);
    }
}
