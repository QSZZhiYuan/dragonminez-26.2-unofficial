package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;

import java.util.List;

/**
 * Shared render helpers for the conventions that changed across 1.21 -> 26.2: immediate-mode buffers
 * (Tesselator / BufferUploader are gone), the stage model-view pose, and the nameplate billboard.
 * Keeping them here means each convention is written down once.
 */
public final class RenderBufferUtil {
	/** Vanilla's nameplate scale, from {@code EntityRenderer#renderNameTag}. */
	public static final float NAMEPLATE_SCALE = 0.025F;

	private RenderBufferUtil() {}

	/**
	 * Collector for the frame currently being built.
	 *
	 * <p>26.2 removed {@code Minecraft#renderBuffers()} / {@code MultiBufferSource}, so there is no
	 * long-lived buffer source to reach for. Callers that only need to queue geometry should hook
	 * {@link net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent} and use its collector
	 * directly; this accessor exists for the few call sites that still take a collector parameter
	 * but are driven from a stage event, where no collector is offered.
	 *
	 * <p>Returns {@code null} when no collector was published this frame, so callers must null-check
	 * (the same guard they previously expressed as an {@code endBatch()} flush).
	 */
	public static SubmitNodeCollector frameCollector() {
		return CURRENT_FRAME_COLLECTOR;
	}

	/** Published by {@code SubmitCustomGeometryEvent} subscribers at the start of their work. */
	public static void publishFrameCollector(SubmitNodeCollector collector) {
		CURRENT_FRAME_COLLECTOR = collector;
	}

	private static SubmitNodeCollector CURRENT_FRAME_COLLECTOR;

	/** Pack RGBA floats (0-1) into ARGB int for GeckoLib render APIs. */
	public static int packColor(float r, float g, float b, float a) {
		return net.minecraft.util.ARGB.colorFromFloat(a, r, g, b);
	}

	/**
	 * Queues a textured quad into the frame's submit collector.
	 *
	 * <h2>26.2 migration</h2>
	 * 1.21 built this with {@code Tesselator.getInstance().begin(QUADS, POSITION_TEX)} and pushed it
	 * with {@code BufferUploader.drawWithShader(mesh)}. Both classes are deleted in 26.2 and there is
	 * no immediate-mode path at all, so the quad is now expressed as vertex data and handed to
	 * {@link SubmitNodeCollector#submitCustomGeometry}.
	 *
	 * <p>The old {@code Matrix4f} parameter became a {@link PoseStack}: the submit API transforms
	 * vertices by the pose captured at submit time, so callers push their own transform instead of
	 * baking it into a matrix argument.
	 */
	public static void drawTexturedQuad(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
			float x0, float y0, float x1, float y1, float z,
			float minU, float minV, float maxU, float maxV) {
		drawTexturedQuad(collector, poseStack, renderType, x0, y0, x1, y1, z, minU, minV, maxU, maxV, 0xFFFFFFFF);
	}

	/**
	 * Tinted variant — this is the 26.2 replacement for {@code RenderSystem.setShaderColor(r,g,b,a)}.
	 *
	 * <p>1.21 set the shader colour as global state before drawing. 26.2 deleted every such global
	 * state switch ({@code RenderSystem.setShaderColor/setShader/setShaderTexture/enableBlend/...})
	 * because render state now lives on the {@link RenderType}'s pipeline. There is no way to set a
	 * uniform at submit time either — {@code submitCustomGeometry}'s callback only receives a
	 * {@link com.mojang.blaze3d.vertex.VertexConsumer}. So the tint travels in the <b>vertex colour</b>.
	 *
	 * @param tint ARGB packed via {@link #packColor(float, float, float, float)}
	 */
	public static void drawTexturedQuad(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
			float x0, float y0, float x1, float y1, float z,
			float minU, float minV, float maxU, float maxV, int tint) {
		// POSITION_TEX layout: normal is unused by the pipeline but the record still carries one.
		List<KiMeshFactory.Vertex> quad = List.of(
				new KiMeshFactory.Vertex(x0, y1, z, minU, maxV, 0f, 0f, 1f, tint),
				new KiMeshFactory.Vertex(x1, y1, z, maxU, maxV, 0f, 0f, 1f, tint),
				new KiMeshFactory.Vertex(x1, y0, z, maxU, minV, 0f, 0f, 1f, tint),
				new KiMeshFactory.Vertex(x0, y0, z, minU, minV, 0f, 0f, 1f, tint));

		collector.submitCustomGeometry(poseStack, renderType,
				(pose, vc) -> KiMeshFactory.emit(quad, pose, vc));
	}

	public static void drawTexturedQuadCentered(SubmitNodeCollector collector, PoseStack poseStack, RenderType renderType,
			float halfSize, float minU, float minV, float maxU, float maxV) {
		drawTexturedQuad(collector, poseStack, renderType, -halfSize, -halfSize, halfSize, halfSize, 0.0F, minU, minV, maxU, maxV);
	}

	/**
	 * Model-view pose for anything drawn from a {@link RenderLevelStageEvent}.
	 *
	 * <p>1.21 handed the stage an identity {@link PoseStack} and kept the camera rotation on
	 * {@code RenderSystem.getModelViewStack()}. 26.2 does not offer a collector on this event at all
	 * (use {@code SubmitCustomGeometryEvent} for geometry, which supplies its own pose), so this
	 * helper now only serves code that still needs the raw model-view matrix.
	 */
	public static PoseStack stageModelViewPose(RenderLevelStageEvent event) {
		PoseStack poseStack = new PoseStack();
		poseStack.last().pose().set(event.getModelViewMatrix());
		poseStack.last().normal().set(new Matrix3f(event.getModelViewMatrix()));
		return poseStack;
	}

	/**
	 * Positions the pose above {@code entity} and turns it into vanilla's nameplate text space,
	 * for anything drawn above an entity's head. The caller owns the surrounding push/pop and
	 * picks how far above the hitbox to sit, since that offset is per-overlay.
	 *
	 * <p>The scale signs are the part worth centralising. 1.21 turned {@code Camera#rotation} an
	 * extra 180 degrees about Y (see {@code Camera#setRotation}), and vanilla absorbed that by
	 * flipping the X sign in {@code EntityRenderer#renderNameTag}: it now scales
	 * {@code (0.025F, -0.025F, 0.025F)} where 1.20.1 scaled {@code (-0.025F, -0.025F, 0.025F)}.
	 * Carrying the old signs over mirrors the overlay and inverts the winding of everything drawn
	 * in it, so its quads and text come out back-facing and are silently culled - which is exactly
	 * how the Ki Sense combat overlay went missing after the port.
	 *
	 * @param heightAboveEntity how far above the entity's hitbox the overlay sits, in blocks
	 */
	public static void nameplateBillboard(PoseStack poseStack, Entity entity, double heightAboveEntity) {
		poseStack.translate(0.0D, entity.getBbHeight() + heightAboveEntity, 0.0D);
		// 26.2: EntityRenderDispatcher#cameraOrientation() is gone - the orientation lives on the camera.
		poseStack.mulPose(Minecraft.getInstance().gameRenderer.mainCamera().rotation());
		poseStack.scale(NAMEPLATE_SCALE, -NAMEPLATE_SCALE, NAMEPLATE_SCALE);
	}
}
