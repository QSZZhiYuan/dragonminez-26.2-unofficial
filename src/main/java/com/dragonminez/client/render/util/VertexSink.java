package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Bridge between DMZ's legacy "grab a VertexConsumer and draw" code and Minecraft 26.2's
 * deferred {@code SubmitNodeCollector} pipeline.
 *
 * <h2>Why this exists</h2>
 * 26.2 removed {@code SubmitNodeCollector}. Code that used to do
 * <pre>{@code
 * VertexConsumer vc = bufferSource.getBuffer(renderType);
 * vc.addVertex(...)...;
 * }</pre>
 * must now hand the draw off to {@code SubmitNodeCollector#submitCustomGeometry(poseStack,
 * renderType, (pose, consumer) -> { ... })}. That API only exposes a <i>single</i> render type per
 * call, while DMZ's hair/effect renderers pick the render type per strand (translucent vs cutout
 * depending on alpha).
 *
 * <p>{@code VertexSink} models that: call {@link #draw(RenderType, Consumer)} once per render type
 * and it packages each group into its own {@code submitCustomGeometry} call.
 */
public interface VertexSink {

    /** Receives the raw vertex consumer for one render type, plus the pose to draw in. */
    @FunctionalInterface
    interface Consumer {
        void accept(PoseStack.Pose pose, VertexConsumer consumer);
    }

    /**
     * Queues a draw batch for {@code renderType}.
     *
     * @param poseStack the pose stack at the moment of the call (captured by value)
     * @param renderType which pipeline to draw with
     * @param draw the actual vertex emission
     */
    void draw(PoseStack poseStack, RenderType renderType, Consumer draw);
}
