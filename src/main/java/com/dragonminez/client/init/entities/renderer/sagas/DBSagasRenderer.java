package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.client.init.entities.renderer.sagas.layer.DMZSagaItemInHandLayer;
import com.dragonminez.client.render.effects.AuraRenderer;
import com.dragonminez.client.init.entities.renderer.sagas.layer.DMZSagaArmorLayer;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.AuraMeshFactory;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.sagas.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.util.ARGB;
import com.geckolib.renderer.GeoEntityRenderer;

import java.util.Random;

public class DBSagasRenderer<T extends DBSagasEntity> extends GeoEntityRenderer<T, DBSagasRenderer.DBSagasRenderState> {

    private static final Identifier NAPPA_NORMAL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa.png");
    private static final Identifier NAPPA_DAMAGED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_nappa2.png");

    public DBSagasRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DBSagaModel<>());
        this.shadowRadius = 0.4f;

        // GeckoLib 5's layer bound is GeoRenderState while this renderer is bound to
        // LivingEntityRenderState; the bridge below is the same unchecked hand-off DMZPlayerRenderer uses.
        this.withRenderLayer(new DMZSagaItemInHandLayer<T, DBSagasRenderState>(this));
        this.withRenderLayer(new DMZSagaArmorLayer<T, DBSagasRenderState>(this, renderManager));
    }

    @Override
    public DBSagasRenderState createRenderState(T entity, Void unused) {
        // GeckoLib's default factory creates LivingEntityRenderState, which cannot
        // reach this renderer's typed extraction/submission methods.
        return new DBSagasRenderState();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addBridgedLayer(Object layer) {
        this.renderLayers.addLayer((com.geckolib.renderer.layer.GeoRenderLayer) layer);
    }

    @Override
    public RenderType getRenderType(DBSagasRenderState renderState, Identifier texture) {
        if (renderState instanceof DBSagasRenderState state && state.dmzShadowDummy) {
            return RenderTypes.entityTranslucent(texture);
        }
        return RenderTypes.entityCutout(texture);
    }

    /**
     * 26.2 splits entity rendering in two: {@code extractRenderState} gathers data, then
     * {@code submit} emits geometry. The 1.21 single {@code render} override mixed both, so the
     * aura/lightning geometry now goes out from {@code submit} - the phase that owns a collector.
     */
    @Override
    public void submit(DBSagasRenderState renderState, PoseStack poseStack, SubmitNodeCollector bufferSource,
                       net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, bufferSource, cameraRenderState);
        // Aura and lightning are driven from PlayerEffectQueue, not from the render state, so the
        // per-entity flags are consulted there (see executeAuraShaderDraw / executeLightningShaderDraw).
    }

    private void drawEffects(T entity, Matrix4f baseMatrix, SubmitNodeCollector collector, float partialTick, boolean showAura, boolean showLightning) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(baseMatrix);
        drawEffectsInline(entity, poseStack, collector, partialTick, showAura, showLightning);
    }

    private void drawEffectsInline(T entity, PoseStack poseStack, SubmitNodeCollector collector, float partialTick, boolean showAura, boolean showLightning) {
        Minecraft mc = Minecraft.getInstance();

        poseStack.pushPose();

        if (showAura) {
            if (entity.onGround()) {
                poseStack.pushPose();
                poseStack.translate(0.0, 0.05, 0.0);
                renderPulseAura(entity, poseStack, mc, collector, partialTick);
                poseStack.popPose();
            }

            poseStack.pushPose();
            executeAuraShaderDraw(entity, poseStack, mc, collector, partialTick);
            poseStack.popPose();
        }

        if (showLightning) {
            poseStack.pushPose();
            executeLightningShaderDraw(entity, poseStack, collector, partialTick);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * 26.2: texture choice moved from getTextureLocation(T) to getTextureLocation(R renderState).
     * Only the render state reaches texture time, so the per-entity decision is captured in
     * addRenderData (which still receives the live entity) and read back here.
     */
    @Override
    public void addRenderData(T animatable, Void unused, DBSagasRenderState renderState, float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        if (renderState instanceof DBSagasRenderState st) {
            st.dmzShadowDummy = animatable instanceof ShadowDummyEntity;
        }
        if (renderState instanceof DBSagasRenderState state && animatable instanceof SagaNappaEntity nappa) {
            state.dmzTexture = nappa.isBattleDamaged() ? NAPPA_DAMAGED : NAPPA_NORMAL;
        }
    }

    @Override
    public Identifier getTextureLocation(DBSagasRenderState renderState) {
        if (renderState instanceof DBSagasRenderState state && state.dmzTexture != null) {
            return state.dmzTexture;
        }
        return super.getTextureLocation(renderState);
    }

    /** Carries the per-entity texture choice into the render state (see addRenderData). */
    /** Render state for saga enemies; also satisfies GeckoLib's GeoRenderState bound. */
    public static class DBSagasRenderState extends LivingEntityRenderState implements com.geckolib.renderer.base.GeoRenderState {
        @Nullable
        public Identifier dmzTexture;
        public boolean dmzShadowDummy;
    }


    @Override
    public int getRenderColor(T animatable, Void unused, float partialTick) {

        if (animatable instanceof ShadowDummyEntity) {
            return ARGB.colorFromFloat(0.7f, 1.0f, 1.0f, 1.0f);
        }

        return super.getRenderColor(animatable, unused, partialTick);
    }

    private void renderPulseAura(T animatable, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick) {
        float time = (animatable.tickCount + partialTick) * 0.02f;
        float progress1 = time % 1.0f;
        float progress2 = (progress1 + 0.5f) % 1.0f;

        drawSinglePulse(animatable, poseStack, mc, collector, partialTick, progress1);
        drawSinglePulse(animatable, poseStack, mc, collector, partialTick, progress2);
    }

    private void drawSinglePulse(T animatable, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick, float progress) {
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.billboardQuad(), 0xFFFFFFFF);
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> groundMesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.groundQuad(), 0xFFFFFFFF);
        float expansion = 1.0f + (6.0f * progress);
        float alphaCurve = (float) Math.sin(progress * Math.PI);

        String auraType = animatable.getAuraType() != null && !animatable.getAuraType().isEmpty() ? animatable.getAuraType().toLowerCase() : "kakarot";
        Identifier crossTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_cross.png");


        poseStack.pushPose();

        float cameraPitch = mc.player != null ? mc.player.getXRot() : 0.0f;
        if (cameraPitch < 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        }

        float scaleMultiplier = 0.7f;
        float sX = expansion * scaleMultiplier;
        float sZ = expansion * scaleMultiplier;

        poseStack.scale(sX, 1.0f, sZ);

        float animSpeed = (animatable.tickCount + partialTick) * 0.5f;

        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());


        RenderType pulseRender = AuraRenderer.auraType(crossTex);
        AuraRenderer.customSetup(pulseRender, crossTex, null);


        AuraRenderer.applyAndDraw(mesh, poseStack, collector, pulseRender, 0xFFFFFFFF);

        AuraRenderer.customClear(pulseRender);

        poseStack.popPose();
    }

    private void executeAuraShaderDraw(T animatable, PoseStack poseStack, Minecraft mc, SubmitNodeCollector collector, float partialTick) {
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.billboardQuad(), 0xFFFFFFFF);
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> groundMesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.groundQuad(), 0xFFFFFFFF);


        String auraType = animatable.getAuraType() != null && !animatable.getAuraType().isEmpty() ? animatable.getAuraType().toLowerCase() : "kakarot";
        Identifier mainTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_aura.png");
        Identifier crossTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/" + auraType + "_cross.png");
        Identifier sparkingTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/aura/sparking_effects.png");

        float animSpeed = (animatable.tickCount + partialTick) * 0.5f;

        float[] color = ColorUtils.rgbIntToFloat(animatable.getAuraColor());


        float cameraPitch = mc.player != null ? mc.player.getXRot() : 0.0f;
        float cameraYaw = mc.player != null ? mc.player.getYRot() : 0.0f;
        float absPitch = Math.abs(cameraPitch);
        float crossFactor = 0.0f;
        float pitchSquash = 1.0f;

        if (absPitch > 45.0f) {
            crossFactor = (float) Math.pow((absPitch - 45.0f) / 45.0f, 2.0);
            pitchSquash = 1.0f - (crossFactor * 0.5f);
        }

        float scaleMultiplier = 2.2f;

        if (crossFactor < 1.0f) {
            poseStack.pushPose();

            poseStack.translate(0.0, animatable.getBbHeight() / 2.0f + 0.8f, 0.0);

            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(cameraPitch));

            poseStack.scale(scaleMultiplier, scaleMultiplier * pitchSquash, scaleMultiplier);


            RenderType mainRender = AuraRenderer.auraType(mainTex);
            AuraRenderer.customSetup(mainRender, mainTex, null);
            AuraRenderer.applyAndDraw(mesh, poseStack, collector, mainRender, 0xFFFFFFFF);
            AuraRenderer.customClear(mainRender);

            poseStack.pushPose();
            float sparkingPulse = 1.0f + (float) Math.sin((animatable.tickCount + partialTick) * 0.2f) * 0.05f;
            poseStack.scale(0.8f * sparkingPulse, 0.65f * sparkingPulse, 0.8f * sparkingPulse);
            poseStack.translate(0.0, -0.25, 0.0);

            RenderType sparkingRender = AuraRenderer.auraType(sparkingTex);
            AuraRenderer.customSetup(sparkingRender, sparkingTex, null);
            AuraRenderer.applyAndDraw(mesh, poseStack, collector, sparkingRender, 0xFFFFFFFF);
            AuraRenderer.customClear(sparkingRender);
            poseStack.popPose();

            poseStack.popPose();
        }

        if (crossFactor > 0.0f) {
            poseStack.pushPose();

            poseStack.translate(0.0, 0.05, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));

            if (cameraPitch < 0.0f) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            poseStack.scale(scaleMultiplier, 1.0f, scaleMultiplier);


            RenderType crossRender = AuraRenderer.auraType(crossTex);
            AuraRenderer.customSetup(crossRender, crossTex, null);

            AuraRenderer.applyAndDraw(groundMesh, poseStack, collector, crossRender, 0xFFFFFFFF);
            AuraRenderer.customClear(crossRender);

            poseStack.popPose();
        }

    }

    private void executeLightningShaderDraw(T animatable, PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> mesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.billboardQuad(), 0xFFFFFFFF);
        java.util.List<com.dragonminez.client.render.util.KiMeshFactory.Vertex> groundMesh =
                AuraRenderer.toQuadVertices(com.dragonminez.client.render.util.AuraMeshFactory.groundQuad(), 0xFFFFFFFF);

        boolean isAuraActive = animatable.isCharge() || animatable.isTransforming();
        float speedMod = isAuraActive ? 1.0f : 0.20f;
        int maxBranches = isAuraActive ? 5 : 3;
        float maxScale = isAuraActive ? 0.5f : 0.25f;

        float[] colorRgb = ColorUtils.rgbIntToFloat(animatable.getLightningColor());
        float time = (animatable.tickCount + partialTick) / 20.0f;



        Identifier lightningTex = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
        RenderType renderType = AuraRenderer.lightningType(lightningTex);
        AuraRenderer.customSetup(renderType, lightningTex, null);


        long timeHash = animatable.level().getGameTime() / 2L;
        Random seededRand = new Random(animatable.getId() + timeHash);
        float bbHeight = animatable.getBbHeight() - 0.6f;

        for (int i = 0; i < maxBranches; i++) {
            poseStack.pushPose();

            float spread = isAuraActive ? 1.8f : 1.2f;
            float randomY = seededRand.nextFloat() * bbHeight;

            poseStack.translate((seededRand.nextFloat() - 0.5f) * spread, randomY, (seededRand.nextFloat() - 0.5f) * spread);
            poseStack.mulPose(Axis.YP.rotationDegrees(seededRand.nextFloat() * 360));

            float scale = 0.15f + seededRand.nextFloat() * maxScale;
            poseStack.scale(scale, scale, scale);


            AuraRenderer.applyAndDraw(mesh, poseStack, collector, renderType, 0xFFFFFFFF);
            poseStack.popPose();
        }

        AuraRenderer.customClear(renderType);
    }
}

