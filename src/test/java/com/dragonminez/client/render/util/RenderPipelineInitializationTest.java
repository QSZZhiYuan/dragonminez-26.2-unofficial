package com.dragonminez.client.render.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenderPipelineInitializationTest {
    @Test
    void translucentKiPartsCannotHideOneAnotherByWritingDepth() {
        for (var type : List.of(ModRenderTypes.kiWave(), ModRenderTypes.kiDisk(), ModRenderTypes.kiRays())) {
            assertFalse(type.pipeline().isCull());
            assertFalse(type.pipeline().getDepthStencilState().writeDepth());
        }
        assertEquals(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES, ModRenderTypes.kiRays().pipeline().getPrimitiveTopology());
        assertFalse(ModRenderTypes.kiRays().sortOnUpload(), "Triangle rays cannot use the engine's quad sorting");
    }

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("dragonminez", "textures/entity/races/null.png");

    @Test
    void skinOverlaysCanInitializeTheRenderTypesUsedByCharacterPreviews() {
        assertNotNull(ModRenderTypes.skinOverlayCutout(TEXTURE));
        assertNotNull(ModRenderTypes.skinOverlayTranslucent(TEXTURE));
        assertNotNull(ModRenderTypes.energy(TEXTURE));
        assertNotNull(ModRenderTypes.getKiLightning(TEXTURE));
    }

    @Test
    void effectPipelinesHaveUniqueBindingsAndOnlyEngineProvidedUniforms() {
        for (RenderPipeline pipeline : List.of(
                ModRenderTypes.getCustomAura(TEXTURE).pipeline(),
                ModRenderTypes.getCustomAuraCompat(TEXTURE).pipeline(),
                ModRenderTypes.getCustomLightning(TEXTURE).pipeline(),
                ModRenderTypes.getCustomLightningCompat(TEXTURE).pipeline(),
                ModRenderTypes.kiWave().pipeline(),
                ModRenderTypes.GUI_AURA)) {
            assertDoesNotThrow(() -> com.mojang.blaze3d.pipeline.BindGroupLayout.ensureCompatible(
                    pipeline.getBindGroupLayouts()), pipeline.toString());
            var engineUniforms = java.util.Set.of("Globals", "DynamicTransforms", "Projection", "Fog", "Lighting");
            for (var group : pipeline.getBindGroupLayouts()) {
                for (var uniform : group.getUniforms()) {
                    assertTrue(engineUniforms.contains(uniform.name()),
                            "Deferred passes do not bind custom material blocks: " + uniform.name());
                }
            }
        }
    }

    @Test
    void guiAuraUsesTheAnimatedMaskShaderWithGuiVertexLayout() {
        assertEquals(ModRenderTypes.getCustomAura(TEXTURE).pipeline().getFragmentShader(),
                ModRenderTypes.GUI_AURA.getFragmentShader());
        assertEquals(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED.getVertexFormatBinding(0),
                ModRenderTypes.GUI_AURA.getVertexFormatBinding(0));
        assertEquals(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED.getColorTargetState(),
                ModRenderTypes.GUI_AURA.getColorTargetState());
    }

    @Test
    void customPipelinesResolveShadersInTheModNamespace() throws java.io.IOException {
        List<RenderPipeline> pipelines = List.of(
                ModRenderTypes.getCustomAura(TEXTURE).pipeline(),
                ModRenderTypes.getCustomAuraCompat(TEXTURE).pipeline(),
                ModRenderTypes.getCustomLightning(TEXTURE).pipeline(),
                ModRenderTypes.getCustomLightningCompat(TEXTURE).pipeline(),
                ModRenderTypes.kiWave().pipeline(),
                ModRenderTypes.transformationMask().pipeline(),
                ModRenderTypes.transformationMaskViewOffset().pipeline());
        for (RenderPipeline pipeline : pipelines) {
            assertShaderResource(pipeline.getVertexShader(), ".vsh");
            assertShaderResource(pipeline.getFragmentShader(), ".fsh");
        }
    }

    private static void assertShaderResource(Identifier shader, String extension) throws java.io.IOException {
        assertEquals("dragonminez", shader.getNamespace());
        String path = "/assets/" + shader.getNamespace() + "/shaders/" + shader.getPath() + extension;
        try (var stream = RenderPipelineInitializationTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Shader resource must exist: " + path);
            assertTrue(stream.readAllBytes().length > 0);
        }
    }
}
