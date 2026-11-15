package com.dragonminez.client.render.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderSetup;

/** Copies a render setup without losing its texture, sampler, layering or output state. */
public interface PipelineRenderSetup {
    RenderSetup dmz$withPipeline(RenderPipeline pipeline);
    RenderSetup dmz$withLightmapPipeline(RenderPipeline pipeline);
}
