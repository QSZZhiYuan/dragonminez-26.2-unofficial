package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Helpers for the "grab a buffer and emit vertices now" pattern that 1.21 code used heavily and
 * which 26.2 replaced with deferred submission.
 *
 * <h2>Why this exists</h2>
 * 1.21:
 * <pre>{@code
 * MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
 * VertexConsumer vc = buf.getBuffer(renderType);
 * vc.addVertex(...)...;
 * buf.endBatch();
 * }</pre>
 * 26.2: {@code Minecraft#renderBuffers()} no longer exists. Geometry must be handed to a
 * {@link SubmitNodeCollector}, either the one delivered to an {@code EntityRenderer#submit} /
 * {@code RenderLevelStage} callback, or via {@link SubmitNodeCollector#submitCustomGeometry}:
 * <pre>{@code
 * collector.submitCustomGeometry(poseStack, renderType, (pose, vc) -> { vc.addVertex(...); });
 * }</pre>
 *
 * <p>Call sites that previously created their own buffer source now receive the frame's collector
 * (from the event) and go through {@link #emit}.
 */
public final class ImmediateGeometry {

    /** Receives the raw vertex consumer for one render type. */
    @FunctionalInterface
    public interface Draw {
        void accept(PoseStack.Pose pose, VertexConsumer consumer);
    }

    private ImmediateGeometry() {
    }

    /**
     * Queues {@code draw} on {@code collector} using {@code renderType}.
     *
     * @param collector the frame's submit node collector
     * @param poseStack pose stack at the moment of the call (captured by value)
     * @param renderType pipeline to draw with
     * @param draw the vertex emission
     */
    public static void emit(SubmitNodeCollector collector, PoseStack poseStack,
                            RenderType renderType, Draw draw) {
        collector.submitCustomGeometry(poseStack, renderType, draw::accept);
    }

    /**
     * Convenience for code that still wants a plain {@link VertexConsumer} with an identity pose,
     * e.g. when it already applied the transform to the passed-in {@code poseStack}.
     */
    public static void emit(SubmitNodeCollector collector, PoseStack poseStack,
                            RenderType renderType, java.util.function.Consumer<VertexConsumer> draw) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, vc) -> draw.accept(vc));
    }
}
