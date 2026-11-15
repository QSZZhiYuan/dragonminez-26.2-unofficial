package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.GeoEntityRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Vanilla excludes the camera entity in first person, so its body needs its own submission. */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonListener {
    private static final ContextKey<BodyFrame> BODY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(Reference.MOD_ID, "first_person_body"));

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void extractBody(ExtractLevelRenderStateEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        FirstPersonManager.publishFrameState(player, false);
        if (!FirstPersonManager.shouldRenderFirstPerson(player) || player.isSleeping()
                || player.isSpectator() || mc.getCameraEntity() != player) return;
        DMZPlayerRenderer<?> renderer = DMZRendererCache.getRenderer(player);
        if (renderer == null) return;
        float partialTick = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        GeoEntityRenderState state = (GeoEntityRenderState) ((EntityRenderer) renderer)
                .createRenderState(player, partialTick);
        state.shadowPieces.clear();
        state.nameTag = null;
        var camera = event.getCamera().position();
        event.getRenderState().setRenderData(BODY, new BodyFrame(renderer, state,
                state.x - camera.x, state.y - camera.y, state.z - camera.z));
    }

    // Queue the body's layers before the effect listener drains aura/lightning submissions.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void submitBody(SubmitCustomGeometryEvent event) {
        BodyFrame body = event.getLevelRenderState().getRenderData(BODY);
        if (body == null) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(body.x, body.y, body.z);
            body.renderer.submit(body.state, pose, event.getSubmitNodeCollector(),
                    event.getLevelRenderState().cameraRenderState);
            FirstPersonManager.publishFrameState(Minecraft.getInstance().player, true);
        } finally {
            pose.popPose();
        }
    }

    private record BodyFrame(DMZPlayerRenderer<?> renderer, GeoEntityRenderState state, double x, double y, double z) {}
}
