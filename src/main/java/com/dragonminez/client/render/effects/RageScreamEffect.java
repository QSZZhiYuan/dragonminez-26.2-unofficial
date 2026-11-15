package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;

import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.ModRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;


import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.dragonminez.client.render.util.RenderBufferUtil;
import net.minecraft.util.ARGB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class RageScreamEffect {

	private static final int RAY_COPIES = 10;
	private static final int CYCLE_TICKS = 15;
	private static final float DEFAULT_HITBOX = 1.8f;
	private static final float BASE_MIN_SPREAD = 0.4f;
	private static final float BASE_MAX_SPREAD = 3.6f;
	private static final float BASE_RAY_SCALE = 1.4f;
	private static final float LENGTH_VARIANCE_MIN = 0.7f;
	private static final float LENGTH_VARIANCE_MAX = 1.3f;

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/skills/rage_scream.png");

	private static final class Entry {
		int ticksRemaining;
		final int totalTicks;

		private Entry(int ticksRemaining, int totalTicks) {
			this.ticksRemaining = ticksRemaining;
			this.totalTicks = totalTicks;
		}

		float getProgress() {
			if (totalTicks <= 0) return 1.0f;
			return 1.0f - (float) ticksRemaining / (float) totalTicks;
		}

		float getCycleProgress() {
			int elapsed = Math.max(0, totalTicks - ticksRemaining);
			return (elapsed % CYCLE_TICKS) / (float) CYCLE_TICKS;
		}
	}

	private static final Map<Integer, Entry> ACTIVE = new ConcurrentHashMap<>();

	private RageScreamEffect() {}

	public static void start(int entityId, int durationTicks) {
		if (durationTicks <= 0) return;
		ACTIVE.put(entityId, new Entry(durationTicks, durationTicks));
	}

	public static java.util.Map<Integer, Float> getActiveProgress() {
		java.util.Map<Integer, Float> out = new java.util.HashMap<>();
		for (var entry : ACTIVE.entrySet()) out.put(entry.getKey(), entry.getValue().getProgress());
		return out;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		if (Minecraft.getInstance().level == null) { ACTIVE.clear(); return; }
		if (Minecraft.getInstance().isPaused()) return;
		if (ACTIVE.isEmpty()) return;

		Iterator<Map.Entry<Integer, Entry>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = it.next().getValue();
			entry.ticksRemaining--;
			if (entry.ticksRemaining <= 0) it.remove();
		}
	}

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float partialTick) {
        var mc = Minecraft.getInstance();
        for (var mapEntry : ACTIVE.entrySet()) {
            int id = mapEntry.getKey();
            if (!(mc.level.getEntity(id) instanceof LivingEntity living) || !living.isAlive()) continue;
            Entry entry = mapEntry.getValue();
            float cycle = ((entry.totalTicks - entry.ticksRemaining + partialTick) % CYCLE_TICKS) / CYCLE_TICKS;
            float eased = 1 - (1 - cycle) * (1 - cycle) * (1 - cycle);
            float alpha = 1 - cycle;
            float factor = Math.max(living.getBbWidth(), living.getBbHeight()) / DEFAULT_HITBOX;
            float spread = Mth.lerp(eased, BASE_MIN_SPREAD * factor, BASE_MAX_SPREAD * factor);
            Vec3 centre = living.getPosition(partialTick).add(0, living.getBbHeight() * 0.5, 0).subtract(camera.pos);
            for (int i = 0; i < RAY_COPIES; i++) {
                float angle = 360f * i / RAY_COPIES + (pseudoRandomUnit(id, i, 1) - 0.5f) * (360f / RAY_COPIES) * 0.6f;
                float variance = Mth.lerp(pseudoRandomUnit(id, i, 2), LENGTH_VARIANCE_MIN, LENGTH_VARIANCE_MAX);
                float rayScale = BASE_RAY_SCALE * factor * variance;
                poseStack.pushPose();
                poseStack.translate(centre.x, centre.y, centre.z);
                poseStack.mulPose(camera.orientation);
                poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
                poseStack.translate(0, spread * variance, 0);
                poseStack.scale(rayScale, rayScale, rayScale);
                RenderBufferUtil.drawTexturedQuad(collector, poseStack, ModRenderTypes.rageScream(), -1, -1, 1, 1, 0, 0, 0, 0.25f, 1,
                        ARGB.colorFromFloat(alpha, 1, 1, 1));
                poseStack.popPose();
            }
        }
    }

	private static float pseudoRandomUnit(int entityId, int copyIndex, int salt) {
		int seed = entityId * 73856093 ^ copyIndex * 19349663 ^ salt * 83492791;
		return (Math.abs(seed) % 10000) / 10000.0f;
	}

}
