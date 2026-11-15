package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.util.KiMeshFactory;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class KiProjectileRenderer extends EntityRenderer<AbstractKiProjectile, KiProjectileRenderer.State> {
    private static final Identifier TEXTURE_KI = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast.png");
    private static final Identifier TEXTURE_KI_SPARKS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle1.png");
    private static final Identifier TEXTURE_KI_SPARKS2 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/kiblast_sparkle2.png");
    private static final Identifier TEXTURE_CORE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/ki_laser.png");
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);

    public KiProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    /** Per-frame snapshot of everything the draw needs. */
    public static class State extends EntityRenderState {
        public int kiRenderType;
        public float scale;
        public float ageInTicks;
        public float lerpYaw;
        public float lerpPitch;
        public float coreR, coreG, coreB;
        public int borderColor = 0xFFFFFFFF, outlineColor = 0xFFFFFFFF;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AbstractKiProjectile entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.ageInTicks = entity.tickCount + partialTick;
        state.kiRenderType = entity.getKiRenderType();
        state.lerpYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        state.lerpPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        float scale = entity.getSize();
        if (entity instanceof KiBlastEntity blastEntity) {
            boolean isFiring = blastEntity.isFiring();
            float castTime = (float) blastEntity.getCastTime();
            if (!isFiring && castTime > 0.1F && state.ageInTicks <= castTime) {
                scale = scale * (state.ageInTicks / castTime);
            }
        }
        state.scale = scale;

        float[] core = entity.getRgbColorMain();
        state.coreR = core[0];
        state.coreG = core[1];
        state.coreB = core[2];
        state.borderColor = 0xFF000000 | entity.getColorBorder();
        state.outlineColor = 0xFF000000 | entity.getColorOutline();
    }

    /**
     * Draws the projectile.
     *
     * <p>26.2 note: the 1.21 version wrapped everything in
     * {@code PlayerEffectQueue.addKiAttack((stack, proj) -> ...)} because it needed a bound
     * {@code VertexBuffer} and a projection matrix. Neither exists now --geometry goes straight into
     * the frame's {@link SubmitNodeCollector} --so the deferral is gone and this draws inline.
     */
    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        float ageInTicks = state.ageInTicks;
        float scale = state.scale;
        float[] coreColor = { state.coreR, state.coreG, state.coreB };
        float[] borderColor = ColorUtils.rgbIntToFloat(state.borderColor);
        float[] outlineColor = ColorUtils.rgbIntToFloat(state.outlineColor);

        poseStack.pushPose();
        poseStack.translate(0.0D, state.boundingBoxHeight / 2.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.lerpYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.lerpPitch));
        poseStack.scale(scale, scale, scale);

            switch (state.kiRenderType) {
                case 4:
                    float[] soulWhite = ColorUtils.rgbIntToFloat(0xFFFFFF);
                    poseStack.pushPose();
                    poseStack.scale(0.15F, 0.15F, 0.15F);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, soulWhite, soulWhite, soulWhite, 1.0F, ageInTicks, true, null);
                    poseStack.popPose();
                    poseStack.pushPose();
                    poseStack.scale(0.4F, 0.4F, 0.4F);
                    renderSoulPunisherSpots(poseStack, collector, ageInTicks);
                    renderCastigadorSpikes(poseStack, collector, ageInTicks, soulWhite);
                    poseStack.popPose();
                    break;
                case 11:
                    float[] moonCore = coreColor;
                    float[] moonOutline = outlineColor;
                    poseStack.pushPose();
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, moonCore, moonCore, moonOutline, 1.0F, ageInTicks, true, null);
                    float moonPulse = 1.12F + (float) Math.sin(ageInTicks * 0.1F) * 0.08F;
                    poseStack.scale(moonPulse, moonPulse, moonPulse);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, moonCore, moonOutline, moonOutline, 0.25F, ageInTicks, false, null);
                    poseStack.popPose();
                    break;
                case 6:
                    applyJitter(poseStack, ageInTicks, 0.01F);
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    break;
                case 7:
                    applyJitter(poseStack, ageInTicks, 0.03F);
                    poseStack.scale(2.0F, 2.0F, 2.0F);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, ColorUtils.darkenColor(coreColor, 0.7F), borderColor, outlineColor, 1.0F, ageInTicks, false, null);
                    float deathPulse = 1.0F + (float)Math.sin(ageInTicks * 0.3F) * 0.15F;
                    poseStack.scale(deathPulse, deathPulse, deathPulse);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, borderColor, outlineColor, outlineColor, 0.3F, ageInTicks, false, null);
                    poseStack.scale(1.1F, 1.1F, 1.1F);
                    int sparkCycle = (int) ageInTicks % 20;
                    if (sparkCycle < 10) {
                        Identifier sparkTex = sparkCycle < 5 ? TEXTURE_KI_SPARKS : TEXTURE_KI_SPARKS2;
                        drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, coreColor, borderColor, outlineColor, 0.8F, ageInTicks, false, sparkTex);
                    }
                    break;
                case 9:
                    float separation = 1.5F;

                    poseStack.pushPose();
                    poseStack.translate(-separation, 0.0D, 0.0D);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    poseStack.popPose();

                    poseStack.pushPose();
                    poseStack.translate(separation, 0.0D, 0.0D);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    poseStack.popPose();
                    break;
                default:
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    drawMesh(KiMeshFactory.sphereMesh(), poseStack, collector, coreColor, borderColor, outlineColor, 1.0F, ageInTicks, true, null);
                    break;
            }

        poseStack.popPose();
    }

    private void applyJitter(PoseStack stack, float ageInTicks, float intensity) {
        float jitterSpeed = ageInTicks * 20.0F;
        stack.translate(Math.sin(jitterSpeed) * intensity, Math.cos(jitterSpeed * 1.2) * intensity, Math.sin(jitterSpeed * 0.8) * intensity);
    }

    /**
     * Replays a pre-baked ki mesh into the frame's submit collector.
     *
     * <p>26.2 removed {@code VertexBuffer}, {@code ShaderInstance} and
     * {@code RenderSystem#setShaderTexture}. The colour/alpha that used to be loose uniforms ride on
     * the vertex data as a tint instead. See {@link KiBarrierRenderer} for the full rationale.
     *
     * @param texture when non-null the textured ki pipeline is used, otherwise the plain one
     */
    private void drawMesh(java.util.List<KiMeshFactory.Vertex> mesh, PoseStack poseStack, SubmitNodeCollector collector,
                          float[] c1, float[] c2, float[] c3, float alpha, float age,
                          boolean addBloom, Identifier texture) {
        int tint = ARGB.colorFromFloat(alpha, c1[0], c1[1], c1[2]);
        if (texture != null) {
            collector.submitCustomGeometry(poseStack, ModRenderTypes.ki_rendertype(texture),
                    (pose, vc) -> KiMeshFactory.emit(mesh, pose, vc, tint));
        } else {
            int border = ARGB.colorFromFloat(1, c2[0], c2[1], c2[2]);
            int outline = ARGB.colorFromFloat(1, c3[0], c3[1], c3[2]);
            collector.submitCustomGeometry(poseStack, ModRenderTypes.kiWave(),
                    (pose, vc) -> KiMeshFactory.emitKiWave(mesh, pose, vc, tint, border, outline));
        }
    }

    /**
     * Bursts of small quads around the projectile.
     *
     * <p>26.2 note: {@code SubmitNodeCollector} has no {@code getBuffer(RenderType)} — the only way
     * to get a {@link VertexConsumer} is the {@code submitCustomGeometry} callback. Each quad's
     * accumulated matrix is therefore baked at submit time and replayed inside that callback.
     */
    private void renderCastigadorSpikes(PoseStack poseStack, SubmitNodeCollector collector, float ageInTicks, float[] colorRGB) {
        float rotationTime = ageInTicks * 3.55F;
        float rawSin = Mth.sin(ageInTicks * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.4F;

        int r = (int)(colorRGB[0] * 255.0F);
        int g = (int)(colorRGB[1] * 255.0F);
        int b = (int)(colorRGB[2] * 255.0F);
        int alpha = (int)(155.0F * fade);

        RandomSource randomsource = RandomSource.create(432L);
        java.util.List<Spike> spikes = new java.util.ArrayList<>();

        for(int i = 0; (float)i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

            float width = randomsource.nextFloat() * 1.1F;
            float length = randomsource.nextFloat() * 0.3F;
            spikes.add(new Spike(new org.joml.Matrix4f(poseStack.last().pose()), width, length, r, g, b, alpha));

            poseStack.popPose();
        }

        collector.submitCustomGeometry(new PoseStack(), ModRenderTypes.kiRays(), (pose, vc) -> {
            for (Spike s : spikes) {
                org.joml.Matrix4f matrix = new org.joml.Matrix4f(pose.pose()).mul(s.matrix);
                vertex01(vc, matrix, s.alpha, s.r, s.g, s.b);
                vertex2(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
                vertex3(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
                vertex01(vc, matrix, s.alpha, s.r, s.g, s.b);
                vertex3(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
                vertex4(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
                vertex01(vc, matrix, s.alpha, s.r, s.g, s.b);
                vertex4(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
                vertex2(vc, matrix, s.width, s.length, s.r, s.g, s.b, s.alpha);
            }
        });
    }

    /** One pre-baked decorative quad. */
    private record Spike(org.joml.Matrix4f matrix, float width, float length, int r, int g, int b, int alpha) {}

    /** Randomly coloured glow blobs on the projectile surface. See {@link #renderCastigadorSpikes}. */
    private void renderSoulPunisherSpots(PoseStack poseStack, SubmitNodeCollector collector, float ageInTicks) {
        RandomSource posRandom = RandomSource.create(91L);
        int timeSeed = (int) (ageInTicks * 2.0F);
        int spotCount = 30;

        java.util.List<Spike> spots = new java.util.ArrayList<>();

        for (int i = 0; i < spotCount; i++) {
            float px = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float py = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float pz = (posRandom.nextFloat() * 2.0F - 1.0F) * 0.42F;
            float blobSize = 0.10F + posRandom.nextFloat() * 0.08F;

            RandomSource colorRandom = RandomSource.create((long) i * 9781L + (long) timeSeed * 131L);
            int r = colorRandom.nextInt(256);
            int g = colorRandom.nextInt(256);
            int b = colorRandom.nextInt(256);

            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(Axis.XP.rotationDegrees(colorRandom.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(colorRandom.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(colorRandom.nextFloat() * 360.0F));

            spots.add(new Spike(new org.joml.Matrix4f(poseStack.last().pose()), blobSize, blobSize, r, g, b, 255));

            poseStack.popPose();
        }

        collector.submitCustomGeometry(new PoseStack(), ModRenderTypes.kiRays(), (pose, vc) -> {
            for (Spike s : spots) {
                org.joml.Matrix4f matrix = new org.joml.Matrix4f(pose.pose()).mul(s.matrix);
                vertex01(vc, matrix, 255, s.r, s.g, s.b);
                vertex2(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
                vertex3(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
                vertex01(vc, matrix, 255, s.r, s.g, s.b);
                vertex3(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
                vertex4(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
                vertex01(vc, matrix, 255, s.r, s.g, s.b);
                vertex4(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
                vertex2(vc, matrix, s.width, s.length, s.r, s.g, s.b, 255);
            }
        });
    }

    private static void vertex01(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
        pConsumer.addVertex(pMatrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, pAlpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
    }
    private static void vertex2(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
    }
    private static void vertex3(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
    }
    private static void vertex4(VertexConsumer pConsumer, org.joml.Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, 0.0F, pWidth, 1.0F * pLength).setColor(r, g, b, alpha).setUv(0.5F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
    }

}

