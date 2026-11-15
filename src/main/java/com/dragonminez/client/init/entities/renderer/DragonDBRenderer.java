package com.dragonminez.client.init.entities.renderer;

import com.dragonminez.client.init.entities.model.DragonDBModel;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.dragonminez.client.render.GeoEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import com.geckolib.renderer.GeoEntityRenderer;

public class DragonDBRenderer extends GeoEntityRenderer<DragonWishEntity, com.dragonminez.client.render.GeoEntityRenderState> {

    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);


    public DragonDBRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new DragonDBModel<>());
	}

    /**
     * 26.2 splits entity rendering into extractRenderState + submit, so the old
     * render(T, ...) override no longer exists. GeckoLib's adjustRenderPose hook is the
     * place to apply the model-space scale/offset that used to live in render().
     */
    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<com.dragonminez.client.render.GeoEntityRenderState> info) {
        super.adjustRenderPose(info);
        info.poseStack().scale(1.5f, 1.5f, 1.5f);
    }

	@Override
	public RenderType getRenderType(com.dragonminez.client.render.GeoEntityRenderState renderState, Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}

    private void renderWishEffect(DragonWishEntity entity, float partialTicks, PoseStack poseStack, SubmitNodeCollector buffer, int r, int g, int b) {
        float rotationTime = (entity.tickCount + partialTicks) * 0.01F;
        float rawSin = Mth.sin((entity.tickCount + partialTicks) * 0.1F);
        float normalizedFade = (rawSin + 1.0F) / 2.0F;
        float fade = 0.4F + (normalizedFade * 0.6F);
        float intensity = 0.6F;

        RandomSource randomsource = RandomSource.create(432L);
        // 26.2: SubmitNodeCollector has no getBuffer; the vertex stream comes from a submit callback.
        final RenderType lightningType = RenderTypes.lightning();

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.scale(1.0F, 1.0F, 1.0F);

        for(int i = 0; (float)i < (intensity + intensity * intensity) / 2.0F * 60.0F; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + rotationTime * 90.0F));

            final float width = randomsource.nextFloat() * 5.0F + 4.0F;
            final float length = randomsource.nextFloat() * 1.0F + 0.5F;
            final int alpha = (int)(255.0F * fade);
            final int fr = r, fg = g, fb = b;

            buffer.submitCustomGeometry(poseStack, lightningType, (pose, vc) -> {
                Matrix4f matrix4f = pose.pose();
                vertex01(vc, matrix4f, alpha, fr, fg, fb);
                vertex2(vc, matrix4f, width, length, fr, fg, fb, alpha);
                vertex3(vc, matrix4f, width, length, fr, fg, fb, alpha);
                vertex01(vc, matrix4f, alpha, fr, fg, fb);
                vertex3(vc, matrix4f, width, length, fr, fg, fb, alpha);
                vertex4(vc, matrix4f, width, length, fr, fg, fb, alpha);
                vertex01(vc, matrix4f, alpha, fr, fg, fb);
                vertex4(vc, matrix4f, width, length, fr, fg, fb, alpha);
                vertex2(vc, matrix4f, width, length, fr, fg, fb, alpha);
            });
        }

        poseStack.popPose();
    }

    private static void vertex01(VertexConsumer pConsumer, Matrix4f pMatrix, int pAlpha, int r, int g, int b) {
        pConsumer.addVertex(pMatrix, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, pAlpha);
    }

    private static void vertex2(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, -HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha);
    }

    private static void vertex3(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, HALF_SQRT_3 * pLength, pWidth, -0.5F * pLength).setColor(r, g, b, alpha);
    }

    private static void vertex4(VertexConsumer pConsumer, Matrix4f pMatrix, float pWidth, float pLength, int r, int g, int b, int alpha) {
        pConsumer.addVertex(pMatrix, 0.0F, pWidth, 1.0F * pLength).setColor(r, g, b, alpha);
    }


	/**
	 * GeckoLib's {@code GeoEntityRenderer#createRenderState(T, Void)} default implementation builds a
	 * plain vanilla {@code LivingEntityRenderState}. This renderer declares {@code GeoEntityRenderState} as its render
	 * state, and {@code fillRenderState} casts to it, so the default produced
	 * <pre>ClassCastException: LivingEntityRenderState cannot be cast to GeoEntityRenderState</pre>
	 * the first time an entity using it was drawn. The single-argument overload is {@code final} and
	 * delegates here, so overriding this one is enough.
	 */
	@Override
	public GeoEntityRenderState createRenderState(DragonWishEntity entity, Void unused) {
		return new GeoEntityRenderState();
	}
}

