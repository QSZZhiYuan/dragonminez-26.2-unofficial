package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZRenderHand;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {

    @Unique
    private static DMZRenderHand dmz$handRenderer;

	// 26.2: renderHandsWithItems was renamed submitHandsWithItems.
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void dmz$cancelGlobalHandRendering(float pPartialTicks, PoseStack pPoseStack, SubmitNodeCollector pBufferSource, LocalPlayer pPlayerEntity, int pCombinedLight, CallbackInfo ci) {
		if (FirstPersonManager.shouldRenderFirstPerson(pPlayerEntity)) ci.cancel();
	}

    /**
     * Lazily builds the DMZ hand renderer.
     *
     * <p>26.2 changed {@code EntityRendererProvider.Context} to a ten-argument record-like holder.
     * {@code Minecraft} exposes six of those publicly (dispatcher, resource manager, map renderer,
     * atlas manager, entity models and font); the other four - {@code BlockModelResolver},
     * {@code ItemModelResolver}, {@code EquipmentAssetManager} and {@code PlayerSkinRenderCache} -
     * have no client accessor, but they are the very instances vanilla handed to the dispatcher it
     * already built. {@link EntityRenderDispatcherAccessor} reads them back, which is what makes a
     * complete {@code Context} constructible here.
     */
    @Unique
    private void dmz$ensureRenderer() {
		if (FirstPersonManager.shouldRenderFirstPerson(Minecraft.getInstance().player)) return;
        if (dmz$handRenderer != null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getEntityRenderDispatcher() == null) return;
        if (mc.getResourceManager() == null || mc.getAtlasManager() == null || mc.getEntityModels() == null) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        EntityRenderDispatcherAccessor accessor = (EntityRenderDispatcherAccessor) (Object) dispatcher;

        EntityRendererProvider.Context context;
        try {
            context = new EntityRendererProvider.Context(
                    dispatcher,
                    accessor.dragonminez$getBlockModelResolver(),
                    accessor.dragonminez$getItemModelResolver(),
                    mc.getMapRenderer(),
                    mc.getResourceManager(),
                    mc.getEntityModels(),
                    accessor.dragonminez$getEquipmentAssets(),
                    mc.getAtlasManager(),
                    mc.font,
                    accessor.dragonminez$getPlayerSkinRenderCache());
        } catch (RuntimeException broken) {
            // The renderer is optional; a failure here must not take the whole first-person view down.
            dmz$handRenderer = null;
            return;
        }

        dmz$handRenderer = new DMZRenderHand(context);
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void dmz$hookRenderArm(PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pCombinedLight, float pEquippedProgress, float pSwingProgress, HumanoidArm pSide, CallbackInfo ci) {
        this.dmz$ensureRenderer();
        AbstractClientPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            pPoseStack.pushPose();
            boolean isRight = pSide == net.minecraft.world.entity.HumanoidArm.RIGHT;
            float f = isRight ? 1.0F : -1.0F;

            float swingProgress = ((Minecraft_DMZ) Minecraft.getInstance()).getSwingProgress();
            float equippedProgress = Math.min(pEquippedProgress, swingProgress);
            float f1 = Mth.sqrt(pSwingProgress);
            float f2 = -0.3F * Mth.sin(f1 * (float)Math.PI);
            float f3 = 0.4F * Mth.sin(f1 * ((float)Math.PI * 2F));
            float f4 = -0.4F * Mth.sin(pSwingProgress * (float)Math.PI);
            pPoseStack.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equippedProgress * -0.6F, f4 + -0.71999997F);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
            float f5 = Mth.sin(pSwingProgress * pSwingProgress * (float)Math.PI);
            float f6 = Mth.sin(f1 * (float)Math.PI);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
            pPoseStack.translate(f * -1.0F, 3.6F, 3.5F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
            pPoseStack.translate(f * 5.6F, 0.0F, 0.0F);

            if(dmz$handRenderer != null){
                if (isRight) {
                    dmz$handRenderer.renderRightHand(pPoseStack, pBuffer, pCombinedLight, player);
                } else {
                    dmz$handRenderer.renderLeftHand(pPoseStack, pBuffer, pCombinedLight, player);
                }
            }


            pPoseStack.popPose();
            ci.cancel();
        }
    }

    @Inject(method = "renderMapHand", at = @At("HEAD"), cancellable = true)
    private void dmz$hookRenderMapHand(PoseStack pPoseStack, SubmitNodeCollector pBuffer, int pCombinedLight, HumanoidArm pSide, CallbackInfo ci) {
        this.dmz$ensureRenderer();
        AbstractClientPlayer player = Minecraft.getInstance().player;

        if (player != null) {
            pPoseStack.pushPose();
            float f = pSide == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;

            pPoseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
            pPoseStack.translate(f * 0.3F, -1.1F, 0.45F);

            if (pSide == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                dmz$handRenderer.renderRightHand(pPoseStack, pBuffer, pCombinedLight, player);
            } else {
                dmz$handRenderer.renderLeftHand(pPoseStack, pBuffer, pCombinedLight, player);
            }

            pPoseStack.popPose();
            ci.cancel();
        }
    }

//    @Redirect(
//            method = { "renderPlayerArm", "renderMapHand" },
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;")
//    )
//    private <T extends Entity> EntityRenderer<? super T> dmz$useVanillaHands(EntityRenderDispatcher instance, T entity) {
//        EntityRenderer<? super T> renderer = instance.getRenderer(entity);
//
//        if (renderer instanceof PlayerDMZRenderer && entity instanceof AbstractClientPlayer player) {
//            String modelName = player.getModelName();
//
//            return (EntityRenderer<? super T>) instance.getSkinMap().get(modelName);
//        }
//
//        return renderer;
//    }

}
