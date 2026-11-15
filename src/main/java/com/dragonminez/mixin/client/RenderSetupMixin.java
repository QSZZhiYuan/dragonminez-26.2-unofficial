package com.dragonminez.mixin.client;

import com.dragonminez.client.render.util.PipelineRenderSetup;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.Map;

@Mixin(RenderSetup.class)
public abstract class RenderSetupMixin implements PipelineRenderSetup {
    @Shadow @Final private Map textures;
    @Shadow @Final private boolean useLightmap;
    @Shadow @Final private boolean useOverlay;
    @Shadow @Final private LayeringTransform layeringTransform;
    @Shadow @Final private OutputTarget outputTarget;
    @Shadow @Final private TextureTransform textureTransform;
    @Shadow @Final private RenderSetup.OutlineProperty outlineProperty;
    @Shadow @Final private boolean affectsCrumbling;
    @Shadow @Final private boolean sortOnUpload;

    @Invoker("<init>")
    private static RenderSetup dmz$create(RenderPipeline pipeline, Map textures, boolean lightmap,
            boolean overlay, LayeringTransform layering, OutputTarget output, TextureTransform texture,
            RenderSetup.OutlineProperty outline, boolean crumbling, boolean sort) {
        throw new AssertionError("Mixin constructor invoker");
    }

    @Override public RenderSetup dmz$withPipeline(RenderPipeline pipeline) {
        return dmz$create(pipeline, textures, useLightmap, useOverlay, layeringTransform,
                outputTarget, textureTransform, outlineProperty, affectsCrumbling, sortOnUpload);
    }
    @Override public RenderSetup dmz$withLightmapPipeline(RenderPipeline pipeline) {
        return dmz$create(pipeline, textures, true, false, layeringTransform,
                outputTarget, textureTransform, RenderSetup.OutlineProperty.NONE, false, true);
    }
}
