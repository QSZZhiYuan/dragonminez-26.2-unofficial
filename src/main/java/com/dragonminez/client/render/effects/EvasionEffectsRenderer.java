package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.IrisCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Captures evasion geometry before feature preparation, and replays it after shader packs. */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class EvasionEffectsRenderer {
    private static SubmitNodeStorage late = new SubmitNodeStorage();

    @SubscribeEvent
    public static void beginFrame(RenderFrameEvent.Pre event) {
        late = new SubmitNodeStorage();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void submit(SubmitCustomGeometryEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var collector = IrisCompat.isShaderPackInUse() ? late : event.getSubmitNodeCollector();
        var pose = event.getPoseStack();
        var camera = event.getLevelRenderState().cameraRenderState;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        AfterimageEffect.submit(pose, collector, camera, partialTick);
        CandyEffect.submitBolts(pose, collector, camera, partialTick);
        RageScreamEffect.submit(pose, collector, camera, partialTick);
        DimensionalShatterEffect.submit(pose, collector, camera, partialTick);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drawLate(RenderLevelStageEvent.AfterLevel event) {
        if (!IrisCompat.isShaderPackInUse()) return;
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        try {
            view.set(event.getModelViewMatrix());
            Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(late);
        } finally {
            view.popMatrix();
        }
    }
}
