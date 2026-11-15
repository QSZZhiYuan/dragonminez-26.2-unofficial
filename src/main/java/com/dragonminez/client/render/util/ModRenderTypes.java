package com.dragonminez.client.render.util;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Util;
import java.util.function.Function;

/**
 * DMZ's custom render types, rebuilt for Minecraft 26.2's pipeline model.
 *
 * <h2>What changed</h2>
 * The 1.21 API ({@code RenderType.create(name, format, mode, size, crumbling, sort,
 * CompositeState.builder()...createCompositeState())}) is gone. In 26.2 a render type is
 * {@code RenderType.create(name, RenderSetup)}, where the {@link RenderSetup} binds a
 * {@link com.mojang.blaze3d.pipeline.RenderPipeline} plus per-draw state (textures, lightmap,
 * overlay, layering, output target). The pipeline itself carries the shader, blend, cull and
 * depth-stencil state.
 *
 * <h2>Mapping used here</h2>
 * Every old CompositeState has been translated to the closest vanilla 26.2 pipeline:
 * <table>
 *   <tr><th>old shader state</th><th>26.2 pipeline</th></tr>
 *   <tr><td>rendertype_item_entity_translucent_cull</td><td>{@link RenderPipelines#ENTITY_TRANSLUCENT_CULL}</td></tr>
 *   <tr><td>rendertype_energy_swirl</td><td>{@link RenderPipelines#ENERGY_SWIRL}</td></tr>
 *   <tr><td>rendertype_eyes</td><td>{@link RenderPipelines#EYES}</td></tr>
 *   <tr><td>rendertype_beacon_beam</td><td>{@link RenderPipelines#BEACON_BEAM_TRANSLUCENT}</td></tr>
 *   <tr><td>rendertype_entity_translucent_emissive</td><td>{@link RenderPipelines#ENTITY_TRANSLUCENT_EMISSIVE}</td></tr>
 *   <tr><td>rendertype_entity_cutout_no_cull</td><td>{@link RenderPipelines#ENTITY_CUTOUT_CULL}</td></tr>
 *   <tr><td>rendertype_entity_translucent</td><td>{@link RenderPipelines#ENTITY_TRANSLUCENT}</td></tr>
 *   <tr><td>position_color</td><td>{@link RenderPipelines#DEBUG_FILLED_BOX} (opaque vertex-coloured)</td></tr>
 * </table>
 *
 * <p>The stencil-gated variants (aura / lightning drawn only where the player silhouette is not
 * already drawn) previously used a raw {@code glStencilFunc} layering shard. In 26.2 stencil
 * behaviour is declared on the pipeline via NeoForge's {@code StencilTest}; see
 * {@link #STENCIL_NOTEQUAL} and {@link #stencilPipeline}.
 */
public final class ModRenderTypes {

    // ------------------------------------------------------------------
    // Base pipelines for the aura / lightning effect render types
    // ------------------------------------------------------------------

    // The DMZ-authored core shaders now live in assets/dragonminez/shaders/core/ and are written
    // against 26.2's GLSL conventions (#version 330, `#moj_import <minecraft:dynamictransforms.glsl>`
    // for ModelViewMat and `projection.glsl` for ProjMat). ShaderManager discovers them by
    // scanning `assets/<ns>/shaders/**/*.vsh|.fsh`, so the pipeline only needs to name them.
    //
    // Deferred geometry supplies per-effect tint/alpha as vertex colours. The shaders use the
    // engine's Globals clock, so every declared uniform is actually bound by the render pass.

    private static final com.mojang.blaze3d.pipeline.RenderPipeline AURA_PIPELINE =
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.toBuilder()
                    .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.getColorTargetState())
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "custom_aura"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .build();

    private static final com.mojang.blaze3d.pipeline.RenderPipeline LIGHTNING_PIPELINE =
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.toBuilder()
                    .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.getColorTargetState())
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "custom_lightning"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/lightning"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/lightning"))
                    .build();

    private static final com.mojang.blaze3d.pipeline.RenderPipeline KI_WAVE_PIPELINE =
            com.mojang.blaze3d.pipeline.RenderPipeline.builder()
                    .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.getColorTargetState())
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.GLOBALS)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.MATRICES_PROJECTION)
                    .withVertexBinding(0, com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY)
                    .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "ki_wave"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/ki_wave"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/ki_wave"))
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(
                            com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false)
                    .build();

    private static final com.mojang.blaze3d.pipeline.RenderPipeline KI_DISK_PIPELINE =
            KI_WAVE_PIPELINE.toBuilder()
                    .withColorTargetState(KI_WAVE_PIPELINE.getColorTargetState())
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "ki_disk"))
                    .withShaderDefine("KI_DISK")
                    .build();

    /** Preview uses the same sprite animation and palette as the world aura, in GUI space. */
    public static final com.mojang.blaze3d.pipeline.RenderPipeline GUI_AURA =
            RenderPipelines.GUI_TEXTURED.toBuilder()
                    .withColorTargetState(RenderPipelines.GUI_TEXTURED.getColorTargetState())
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "gui_aura"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .build();

    // ------------------------------------------------------------------
    // Stencil variant (was: STENCIL_READ_NOTEQUAL LayeringStateShard)
    // ------------------------------------------------------------------

    /**
     * Returns a copy of {@code base} with NeoForge's stencil test set to "draw only where the
     * reference value was not written" — the 26.2 equivalent of the old
     * {@code glStencilFunc(GL_NOTEQUAL, 1, 0xFF)} + {@code glStencilOp(KEEP, KEEP, KEEP)} aura
     * layering shard.
     *
     * <p>Uses {@link com.mojang.blaze3d.pipeline.RenderPipeline#toBuilder()} so every other
     * property of the base pipeline (blend, cull, colour target, depth-stencil, vertex layout) is
     * preserved verbatim. Building a fresh pipeline from {@code builder()} would silently reset
     * all of those to defaults — which is how the aura would have lost its translucency.
     */
    private static com.mojang.blaze3d.pipeline.RenderPipeline stencilPipeline(
            com.mojang.blaze3d.pipeline.RenderPipeline base, Identifier location) {
        return base.toBuilder()
                .withColorTargetState(base.getColorTargetState())
                .withLocation(location)
                .withStencilTest(STENCIL_NOTEQUAL)
                .build();
    }

    // ------------------------------------------------------------------
        /** Draw-only-where-not-marked stencil state (old STENCIL_READ_NOTEQUAL shard). */
    private static final net.neoforged.neoforge.client.stencil.StencilTest STENCIL_NOTEQUAL = createStencilNotequal();

    private static net.neoforged.neoforge.client.stencil.StencilTest createStencilNotequal() {
        var keep = net.neoforged.neoforge.client.stencil.StencilOperation.KEEP;
        var perFace = new net.neoforged.neoforge.client.stencil.StencilPerFaceTest(
                keep, keep, keep, com.mojang.blaze3d.platform.CompareOp.NOT_EQUAL);
        return new net.neoforged.neoforge.client.stencil.StencilTest(
                perFace,
                net.neoforged.neoforge.client.stencil.StencilTest.DEFAULT_READ_MASK,
                net.neoforged.neoforge.client.stencil.StencilTest.DEFAULT_WRITE_MASK,
                1);
    }

    // Simple textured entity types
    // ------------------------------------------------------------------

    private static final Function<Identifier, RenderType> GLOW = Util.memoize(texture ->
            RenderType.create("dmz_glow", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> GLOW_KI = Util.memoize(texture ->
            RenderType.create("dmz_glow_ki", RenderSetup.builder(RenderPipelines.ENERGY_SWIRL)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> ENERGY = Util.memoize(texture ->
            RenderType.create("dmz_energy", RenderSetup.builder(RenderPipelines.EYES)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> ENERGY2 = Util.memoize(texture ->
            RenderType.create("dmz_energy2", RenderSetup.builder(RenderPipelines.EYES)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> LIGHTNING = Util.memoize(texture ->
            RenderType.create("dmz_lightning", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> KI_BLAST = Util.memoize(texture ->
            RenderType.create("dmz_ki_blast", RenderSetup.builder(RenderPipelines.BEACON_BEAM_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> KI_RENDERTYPE = Util.memoize(texture ->
            RenderType.create("dmz_ki_rendertype", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final RenderType KI_WAVE =
            RenderType.create("dmz_ki_wave", RenderSetup.builder(KI_WAVE_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());

    private static final RenderType KI_DISK =
            RenderType.create("dmz_ki_disk", RenderSetup.builder(KI_DISK_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());

    private static final RenderType KI_RAYS = RenderType.create("dmz_ki_rays", RenderSetup.builder(
            RenderPipelines.DEBUG_FILLED_BOX.toBuilder()
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(
                            com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "ki_rays"))
                    .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES)
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(
                            com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false)
                    .build()).createRenderSetup());

    private static final Function<Identifier, RenderType> AURA_BILLBOARD = Util.memoize(texture ->
            RenderType.create("dmz_aura_billboard", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE)
                    .withTexture("Sampler0", texture)
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> SKIN_OVERLAY_CUTOUT = Util.memoize(texture ->
            RenderType.create("dmz_skin_overlay_cutout", RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_CULL)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> SKIN_OVERLAY_TRANSLUCENT = Util.memoize(texture ->
            RenderType.create("dmz_skin_overlay_translucent", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .affectsCrumbling()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> SCOUTER_LENS = Util.memoize(texture ->
            RenderType.create("dmz_scouter_lens", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT_CULL)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()));

    // ------------------------------------------------------------------
    // Public API — signatures unchanged from 1.21
    // ------------------------------------------------------------------

    /**
     * Aura shader render type (stencil-gated: draws only outside the player silhouette).
     *
     * <p>The authored shader samples and blends the four-frame sprite sheet, converts its
     * black intensity mask to transparency, and applies the per-effect vertex tint.
     */
    public static RenderType getCustomAura(Identifier texture) {
        return auraType(texture, true);
    }

    /** Aura variant used under Iris shaderpacks: same pipeline, no depth test writes. */
    public static RenderType getCustomAuraCompat(Identifier texture) {
        return auraType(texture, false);
    }

    /** Lightning shader render type (stencil-gated). */
    public static RenderType getCustomLightning(Identifier texture) {
        return lightningType(texture, true);
    }

    /** Lightning variant used under Iris shaderpacks. */
    public static RenderType getCustomLightningCompat(Identifier texture) {
        return lightningType(texture, false);
    }

    private static final Map<Identifier, RenderType> AURA_CACHE = new HashMap<>();
    private static final Map<Identifier, RenderType> AURA_COMPAT_CACHE = new HashMap<>();
    private static final Map<Identifier, RenderType> LIGHTNING_CACHE = new HashMap<>();
    private static final Map<Identifier, RenderType> LIGHTNING_COMPAT_CACHE = new HashMap<>();

    private static RenderType auraType(Identifier texture, boolean stencilGated) {
        Map<Identifier, RenderType> cache = stencilGated ? AURA_CACHE : AURA_COMPAT_CACHE;
        return cache.computeIfAbsent(texture, t -> {
            var pipeline = stencilGated
                    ? stencilPipeline(AURA_PIPELINE, Identifier.fromNamespaceAndPath("dragonminez", "custom_aura_stencil"))
                    : AURA_PIPELINE;
            return RenderType.create(stencilGated ? "dmz_custom_aura" : "dmz_custom_aura_compat",
                    RenderSetup.builder(pipeline)
                            .withTexture("Sampler0", t)
                            .createRenderSetup());
        });
    }

    private static RenderType lightningType(Identifier texture, boolean stencilGated) {
        Map<Identifier, RenderType> cache = stencilGated ? LIGHTNING_CACHE : LIGHTNING_COMPAT_CACHE;
        return cache.computeIfAbsent(texture, t -> {
            var pipeline = stencilGated
                    ? stencilPipeline(LIGHTNING_PIPELINE, Identifier.fromNamespaceAndPath("dragonminez", "custom_lightning_stencil"))
                    : LIGHTNING_PIPELINE;
            return RenderType.create(stencilGated ? "dmz_custom_lightning" : "dmz_custom_lightning_compat",
                    RenderSetup.builder(pipeline)
                            .withTexture("Sampler0", t)
                            .createRenderSetup());
        });
    }

    public static RenderType glow(Identifier location) { return GLOW.apply(location); }
    public static RenderType glow_ki(Identifier location) { return GLOW_KI.apply(location); }
    public static RenderType energy(Identifier location) { return ENERGY.apply(location); }
    public static RenderType energy2(Identifier location) { return ENERGY2.apply(location); }
    public static RenderType auraBillboard(Identifier location) { return AURA_BILLBOARD.apply(location); }
    public static RenderType scouterLens(Identifier location) { return SCOUTER_LENS.apply(location); }
    public static RenderType skinOverlayCutout(Identifier location) { return SKIN_OVERLAY_CUTOUT.apply(location); }
    public static RenderType skinOverlayTranslucent(Identifier location) { return SKIN_OVERLAY_TRANSLUCENT.apply(location); }
    public static RenderType lightning(Identifier location) { return LIGHTNING.apply(location); }
    public static RenderType kiblast(Identifier location) { return KI_BLAST.apply(location); }
    public static RenderType ki_rendertype(Identifier location) { return KI_RENDERTYPE.apply(location); }
    public static RenderType kiWave() { return KI_WAVE; }
    public static RenderType kiDisk() { return KI_DISK; }
    public static RenderType kiRays() { return KI_RAYS; }

    /**
     * Opaque, vertex-coloured, double-sided geometry (Majin absorption gum blob).
     *
     * <p>1.21 built this from {@code position_color} + {@code NO_CULL} + {@code COLOR_DEPTH_WRITE}
     * on {@code DefaultVertexFormat.POSITION_COLOR} with {@code TRIANGLES}. The closest 26.2
     * pipeline with that vertex layout and blend profile is {@code DEBUG_TRIANGLE_FAN}; the blob
     * is triangle soup with no fog and no texture, so no texture binding is added.
     *
     * <p>Cached — callers draw this every frame while the absorption effect is active.
     */
    public static RenderType gooBlob() {
        return GOO_BLOB;
    }

    private static final RenderType GOO_BLOB =
            RenderType.create("dmz_goo_blob", RenderSetup.builder(RenderPipelines.DEBUG_TRIANGLE_FAN)
                    .createRenderSetup());

    public static RenderType getKiLightning(Identifier location) {
        return RenderType.create("ki_lightning", RenderSetup.builder(KI_LIGHTNING_PIPELINE)
                .withTexture("Sampler0", location)
                .sortOnUpload()
                .createRenderSetup());
    }

    /** Built once: stencil-gated emissive pipeline used by {@link #getKiLightning}. */
    private static final com.mojang.blaze3d.pipeline.RenderPipeline KI_LIGHTNING_PIPELINE =
            stencilPipeline(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, Identifier.fromNamespaceAndPath("dragonminez", "ki_lightning"));

    // ------------------------------------------------------------------
    // Transformation mask (post-shader outline)
    // ------------------------------------------------------------------

    private static final Map<Identifier, RenderType> TEXTURED_MASK_CACHE = new HashMap<>();
    private static final Map<Identifier, RenderType> TEXTURED_MASK_VIEW_OFFSET_CACHE = new HashMap<>();

    private static final String VIEW_OFFSET_LAYERING_TOKEN = "view_offset_z_layering";

    private static RenderType maskType(String name, @Nullable Identifier texture, boolean viewOffset) {
        // The outline pass uses DMZ's own transformation_mask shaders, migrated to 26.2 GLSL.
        var pipeline = RenderPipelines.ENTITY_TRANSLUCENT.toBuilder()
                .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT.getColorTargetState())
                .withLocation(Identifier.fromNamespaceAndPath("dragonminez", name))
                .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", texture != null
                        ? "core/transformation_mask_tex"
                        : "core/transformation_mask"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", texture != null
                        ? "core/transformation_mask_tex"
                        : "core/transformation_mask"))
                .build();

        RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline);
        if (texture != null) {
            b = b.withTexture("Sampler0", texture);
        }
        if (viewOffset) {
            b = b.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING);
        }
        return RenderType.create(name, b.createRenderSetup());
    }

    public static RenderType transformationMask() {
        return TRANSFORMATION_MASK;
    }

    public static RenderType transformationMaskViewOffset() {
        return TRANSFORMATION_MASK_VIEW_OFFSET;
    }

    /** Built once — these two are requested every frame by the outline pass. */
    private static final RenderType TRANSFORMATION_MASK =
            maskType("transformation_mask", null, false);
    private static final RenderType TRANSFORMATION_MASK_VIEW_OFFSET =
            maskType("transformation_mask_view_offset", null, true);

    public static RenderType transformationMask(RenderType sourceRenderType) {
        Identifier texture = resolveSourceTexture(sourceRenderType);
        boolean viewOffset = sourceRenderType != null
                && sourceRenderType.toString().contains(VIEW_OFFSET_LAYERING_TOKEN);
        if (texture != null) {
            Map<Identifier, RenderType> cache = viewOffset ? TEXTURED_MASK_VIEW_OFFSET_CACHE : TEXTURED_MASK_CACHE;
            return cache.computeIfAbsent(texture, t -> buildTexturedMask(t, viewOffset));
        }
        return viewOffset ? transformationMaskViewOffset() : transformationMask();
    }

    private static RenderType buildTexturedMask(Identifier texture, boolean viewOffset) {
        return maskType("transformation_mask_tex", texture, viewOffset);
    }

    /**
     * 26.2 note: the 1.21 implementation reached into the composite render type via mixins to
     * recover the texture identifier. {@code RenderSetup} in 26.2 stores textures as named
     * bindings without a public accessor, so texture recovery is not possible without an
     * accessor mixin onto {@code RenderSetup}; for now textured masks fall back to the
     * textureless mask.
     */
    @Nullable
    private static Identifier resolveSourceTexture(@Nullable RenderType sourceRenderType) {
        return null;
    }

    public static boolean hasTransformationMaskShader() {
        // The 26.2 mask collector is currently a pass-through: it neither captures geometry
        // nor supplies a mask target to the frame graph. Shader files alone do not make
        // this path usable. Advertising it caused remote transformations (e.g. Ikari) to
        // install an absent post chain and trigger Minecraft's resource-recovery disconnect.
        return false;
    }


    private static final Map<RenderType, RenderType> ASSIMILATION_SKETCH = new java.util.WeakHashMap<>();
    public static RenderType assimilationSketch(RenderType source) {
        if (source == null || source.format() != com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY
                || source.primitiveTopology() != com.mojang.blaze3d.PrimitiveTopology.QUADS) return null;
        String name = source.toString();
        if (name.contains("shadow") || name.contains("glint") || name.contains("dmz_assimilation")) return null;
        return ASSIMILATION_SKETCH.computeIfAbsent(source, type -> {
            var pipeline = type.pipeline().toBuilder()
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "assimilation_sketch/" + ASSIMILATION_SKETCH.size()))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/assimilation_sketch"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/assimilation_sketch"))
                    .withCull(false).build();
            var setup = ((com.dragonminez.mixin.client.RenderTypeAccessor) (Object) type).dmz$setup();
            return RenderType.create("dmz_assimilation", ((PipelineRenderSetup) (Object) setup).dmz$withPipeline(pipeline));
        });
    }

    private static final Map<RenderType, RenderType> RITUAL_SHELL = new java.util.WeakHashMap<>();
    public static RenderType ritualShell(RenderType source) {
        if (source == null || source.format() != com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY
                || source.primitiveTopology() != com.mojang.blaze3d.PrimitiveTopology.QUADS) return null;
        String name = source.toString();
        if (name.contains("shadow") || name.contains("glint") || name.contains("outline") || name.contains("ritual_shell")) return null;
        return RITUAL_SHELL.computeIfAbsent(source, type -> {
            var pipeline = type.pipeline().toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "ritual_shell/" + RITUAL_SHELL.size()))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/ritual_shell"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/ritual_shell"))
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false).build();
            var setup = ((com.dragonminez.mixin.client.RenderTypeAccessor) (Object) type).dmz$setup();
            return RenderType.create("dmz_ritual_shell", ((PipelineRenderSetup) (Object) setup).dmz$withPipeline(pipeline));
        });
    }

    private static final Map<RenderType, RenderType> AFTERIMAGE = new java.util.WeakHashMap<>();

    public static RenderType afterimage(RenderType source) {
        if (source == null || source.format() != com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY
                || source.primitiveTopology() != com.mojang.blaze3d.PrimitiveTopology.QUADS) return null;
        String name = source.toString();
        if (name.contains("shadow") || name.contains("glint") || name.contains("outline") || name.contains("aura")
                || name.contains("lightning") || name.contains("ritual_shell") || name.contains("afterimage")) return null;
        return AFTERIMAGE.computeIfAbsent(source, type -> {
            var pipeline = com.mojang.blaze3d.pipeline.RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0_SAMPLER2)
                    .withVertexBinding(0, com.mojang.blaze3d.vertex.DefaultVertexFormat.ENTITY)
                    .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "afterimage/" + AFTERIMAGE.size()))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/afterimage"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/afterimage"))
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false).build();
            var setup = ((com.dragonminez.mixin.client.RenderTypeAccessor) (Object) type).dmz$setup();
            return RenderType.create("dmz_afterimage", ((PipelineRenderSetup) (Object) setup).dmz$withLightmapPipeline(pipeline));
        });
    }

    private static final RenderType EFFECT_CUBES = RenderType.create("dmz_dimensional_cubes", RenderSetup.builder(
            RenderPipelines.DEBUG_FILLED_BOX.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "dimensional_cubes"))
                    .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
                    .withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT))
                    .withCull(false).build()).createRenderSetup());

    private static final RenderType RAGE_SCREAM = RenderType.create("dmz_rage_scream", RenderSetup.builder(
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "rage_scream"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/rage_scream"))
                    .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.getColorTargetState())
                    .withCull(false).build())
            .withTexture("Sampler0", Identifier.fromNamespaceAndPath("dragonminez", "textures/skills/rage_scream.png"))
            .sortOnUpload().createRenderSetup());

    private static final com.mojang.blaze3d.pipeline.RenderPipeline LIGHTNING_BOLT_PIPELINE =
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "lightning_bolt"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("dragonminez", "core/aura"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("dragonminez", "core/lightning_bolt"))
                    .withColorTargetState(RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE.getColorTargetState())
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(
                            com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false).build();
    private static final RenderType LIGHTNING_BOLT = RenderType.create("dmz_lightning_bolt",
            RenderSetup.builder(LIGHTNING_BOLT_PIPELINE).sortOnUpload().createRenderSetup());
    private static final RenderType LIGHTNING_BOLT_COMPAT = RenderType.create("dmz_lightning_bolt_compat",
            RenderSetup.builder(LIGHTNING_BOLT_PIPELINE.toBuilder()
                    .withLocation(Identifier.fromNamespaceAndPath("dragonminez", "lightning_bolt_compat"))
                    .withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(
                            com.mojang.blaze3d.platform.CompareOp.ALWAYS_PASS, false))
                    .build()).sortOnUpload().createRenderSetup());

    public static RenderType effectCubes() { return EFFECT_CUBES; }
    public static RenderType rageScream() { return RAGE_SCREAM; }
    public static RenderType lightningBolt() { return LIGHTNING_BOLT; }
    public static RenderType lightningBoltCompat() { return LIGHTNING_BOLT_COMPAT; }

    private ModRenderTypes() {
    }
}
