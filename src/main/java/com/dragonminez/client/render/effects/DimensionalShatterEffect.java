package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.IrisCompat;



import com.mojang.blaze3d.vertex.PoseStack;


import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.dragonminez.client.render.util.ModRenderTypes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class DimensionalShatterEffect {

	private static final int MAX_CUBES = 900;
	private static final int MAX_CUBES_PER_BURST = 170;
	private static final double MAX_RENDER_DISTANCE_SQR = 128.0 * 128.0;

	private static final int SHATTER_LIFE = 22;
	private static final int ASSEMBLE_LIFE = 12;
	private static final int HIDE_TICKS = 9;

	private static final float INSET = 0.37F;
	private static final float INSET_LIFT = 0.004F;
	private static final float EDGE_SHADE = 0.10F;

	private static final int[] PALETTE = {
			0xD7263D, 0x8E3FB8, 0xF5D33B, 0x2FC7B0, 0x2B4FD8, 0xF26CA7, 0xB9A3E3, 0xF28C28
	};

	private static final float[][] FACES = {
			{0, 1, 0, 1, 0, 0, 0, 0, 1, 1.00F},
			{0, -1, 0, 1, 0, 0, 0, 0, 1, 0.55F},
			{0, 0, 1, 1, 0, 0, 0, 1, 0, 0.82F},
			{0, 0, -1, 1, 0, 0, 0, 1, 0, 0.82F},
			{1, 0, 0, 0, 0, 1, 0, 1, 0, 0.68F},
			{-1, 0, 0, 0, 0, 1, 0, 1, 0, 0.68F}
	};

	private static final class Cube {
		final Vec3 home;
		final Vec3 drift;
		final Vector3f axis;
		final float spin;
		final float size;
		final float[] color;
		final boolean assemble;
		final int lifetime;
		final int delay;
		int age;

		private Cube(Vec3 home, Vec3 drift, Vector3f axis, float spin, float size, float[] color,
					 boolean assemble, int lifetime, int delay) {
			this.home = home;
			this.drift = drift;
			this.axis = axis;
			this.spin = spin;
			this.size = size;
			this.color = color;
			this.assemble = assemble;
			this.lifetime = lifetime;
			this.delay = delay;
		}
	}

	private static final List<Cube> CUBES = new ArrayList<>();
	private static final Map<Integer, Integer> HIDDEN = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();

	private DimensionalShatterEffect() {}

	public static void spawn(int entityId, double x, double y, double z, float width, float height, boolean assemble) {
		Minecraft mc = Minecraft.getInstance();
		if (assemble && mc.player != null && mc.player.getId() == entityId && mc.options.getCameraType().isFirstPerson()) return;

		float w = Math.max(0.4F, width);
		float h = Math.max(0.6F, height);

		float size = Mth.clamp((float) Math.cbrt(w * w * h / 110.0F), 0.16F, 1.6F);
		int nx = Math.max(2, Mth.ceil(w / size));
		int ny = Math.max(3, Mth.ceil(h / size));
		int total = nx * ny * nx;
		float keep = total > MAX_CUBES_PER_BURST ? (float) MAX_CUBES_PER_BURST / total : 1.0F;

		for (int ix = 0; ix < nx; ix++) {
			for (int iy = 0; iy < ny; iy++) {
				for (int iz = 0; iz < nx; iz++) {
					if (keep < 1.0F && RANDOM.nextFloat() > keep) continue;

					double px = x - w * 0.5 + (ix + 0.5) * (w / nx);
					double py = y + (iy + 0.5) * (h / ny);
					double pz = z - w * 0.5 + (iz + 0.5) * (w / nx);
					Vec3 home = new Vec3(px, py, pz);

					Vec3 out = new Vec3(px - x, (py - (y + h * 0.5)) * 0.6, pz - z);
					if (out.lengthSqr() < 1.0E-4) out = new Vec3(RANDOM.nextDouble() - 0.5, 0.2, RANDOM.nextDouble() - 0.5);
					double reach = (1.2 + RANDOM.nextDouble() * 1.6) * Math.max(1.0, w);
					Vec3 drift = out.normalize().scale(reach)
							.add((RANDOM.nextDouble() - 0.5) * 0.6, RANDOM.nextDouble() * 0.8, (RANDOM.nextDouble() - 0.5) * 0.6);

					Vector3f axis = new Vector3f(RANDOM.nextFloat() - 0.5F, RANDOM.nextFloat() - 0.5F, RANDOM.nextFloat() - 0.5F);
					if (axis.lengthSquared() < 1.0E-4F) axis.set(0, 1, 0);
					axis.normalize();

					float cubeSize = size * (0.75F + RANDOM.nextFloat() * 0.45F);
					float[] color = rgb(PALETTE[RANDOM.nextInt(PALETTE.length)]);
					int delay = assemble ? RANDOM.nextInt(3) : Mth.floor((float) iy / ny * 5.0F) + RANDOM.nextInt(2);
					int life = (assemble ? ASSEMBLE_LIFE : SHATTER_LIFE) + RANDOM.nextInt(5);

					while (CUBES.size() >= MAX_CUBES) CUBES.remove(0);
					CUBES.add(new Cube(home, drift, axis, 8.0F + RANDOM.nextFloat() * 22.0F, cubeSize, color, assemble, life, delay));
				}
			}
		}

		if (assemble && entityId >= 0) HIDDEN.put(entityId, HIDE_TICKS);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			CUBES.clear();
			HIDDEN.clear();
			return;
		}
		if (mc.isPaused()) return;

		CUBES.removeIf(cube -> ++cube.age >= cube.lifetime + cube.delay);
		HIDDEN.replaceAll((id, ticks) -> ticks - 1);
		HIDDEN.values().removeIf(ticks -> ticks <= 0);
	}

    public static boolean isHidden(int entityId) {
        return !AfterimageEffect.isRendering() && HIDDEN.containsKey(entityId);
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, float partialTick) {
        Vec3 camPos = camera.pos;
        for (Cube cube : CUBES) {
            float time = cube.age + partialTick - cube.delay;
            if (time < 0) {
                if (!cube.assemble) collector.submitCustomGeometry(poseStack, ModRenderTypes.effectCubes(), (pose, vertices) -> {
                    PoseStack stack = new PoseStack(); stack.last().set(pose);
                    emitCube(vertices, stack, camPos, cube, cube.home, 0, 1, 1);
                });
                continue;
            }
            float progress = Mth.clamp(time / cube.lifetime, 0, 1);
            float travel = cube.assemble ? 1 - easeOut(progress) : easeOut(progress);
            Vec3 moved = cube.home.add(cube.drift.scale(travel));
            final Vec3 position = cube.assemble ? moved : moved.add(0, -1.4 * progress * progress, 0);
            if (position.distanceToSqr(camPos) > MAX_RENDER_DISTANCE_SQR) continue;
            float scale = cube.assemble ? Mth.clamp(progress * 3, 0, 1) : 1 - Mth.clamp((progress - 0.6f) / 0.4f, 0, 1);
            float alpha = cube.assemble && progress > 0.85f ? 1 - (progress - 0.85f) / 0.15f : 1;
            if (scale <= 0.02f || alpha <= 0.02f) continue;
            float angle = cube.spin * time * travel;
            collector.submitCustomGeometry(poseStack, ModRenderTypes.effectCubes(), (pose, vertices) -> {
                PoseStack stack = new PoseStack(); stack.last().set(pose);
                emitCube(vertices, stack, camPos, cube, position, angle, scale, alpha);
            });
        }
    }

	private static void emitCube(VertexConsumer buffer, PoseStack poseStack, Vec3 camPos, Cube cube,
								 Vec3 pos, float angle, float scale, float alpha) {
		poseStack.pushPose();
		poseStack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
		if (angle != 0.0F) poseStack.mulPose(Axis.of(cube.axis).rotationDegrees(angle));
		float s = cube.size * scale;
		poseStack.scale(s, s, s);

		Matrix4f pose = poseStack.last().pose();
		for (float[] face : FACES) {
			float shade = face[9];
			quad(buffer, pose, face, 0.5F, 0.5F,
					cube.color[0] * EDGE_SHADE, cube.color[1] * EDGE_SHADE, cube.color[2] * EDGE_SHADE, alpha);
			quad(buffer, pose, face, 0.5F + INSET_LIFT, INSET,
					cube.color[0] * shade, cube.color[1] * shade, cube.color[2] * shade, alpha);
		}
		poseStack.popPose();
	}

	private static void quad(VertexConsumer buffer, Matrix4f pose, float[] f, float dist, float half,
							 float r, float g, float b, float a) {
		float nx = f[0] * dist, ny = f[1] * dist, nz = f[2] * dist;
		float ux = f[3] * half, uy = f[4] * half, uz = f[5] * half;
		float vx = f[6] * half, vy = f[7] * half, vz = f[8] * half;

		buffer.addVertex(pose, nx - ux - vx, ny - uy - vy, nz - uz - vz).setColor(r, g, b, a);
		buffer.addVertex(pose, nx + ux - vx, ny + uy - vy, nz + uz - vz).setColor(r, g, b, a);
		buffer.addVertex(pose, nx + ux + vx, ny + uy + vy, nz + uz + vz).setColor(r, g, b, a);
		buffer.addVertex(pose, nx - ux + vx, ny - uy + vy, nz - uz + vz).setColor(r, g, b, a);
	}

	private static float[] rgb(int color) {
		return new float[]{((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
	}

	private static float easeOut(float t) {
		float inv = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
		return 1.0F - inv * inv * inv;
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
		return stack;
	}
}
