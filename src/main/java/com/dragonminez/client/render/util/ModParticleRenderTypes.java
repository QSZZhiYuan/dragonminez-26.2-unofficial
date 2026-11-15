package com.dragonminez.client.render.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/** Particle layers retain their own blend state in the 26.2 extraction pipeline. */
public final class ModParticleRenderTypes {
    public static final SingleQuadParticle.Layer ADDITIVE_LIT = new SingleQuadParticle.Layer(true,
            TextureAtlas.LOCATION_PARTICLES, RenderPipelines.TRANSLUCENT_PARTICLE.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "additive_particle"))
                    .withColorTargetState(new ColorTargetState(new BlendFunction(
                            BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .build());

    private ModParticleRenderTypes() {}
}
