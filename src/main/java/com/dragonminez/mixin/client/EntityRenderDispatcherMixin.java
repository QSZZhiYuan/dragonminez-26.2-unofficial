package com.dragonminez.mixin.client;

import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.GeoEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Hooks the entity renderer dispatcher and exposes the collaborators DMZ needs to rebuild an
 * {@link EntityRendererProvider.Context}.
 *
 * <h2>26.2 accessor note</h2>
 * {@code EntityRendererProvider.Context} takes ten arguments. {@code Minecraft} exposes six
 * publicly (dispatcher, resource manager, map renderer, atlas manager, entity models, font); the
 * other four have no client accessor:
 * {@link BlockModelResolver}, {@link ItemModelResolver}, {@link EquipmentAssetManager},
 * {@link PlayerSkinRenderCache}. They are the exact instances vanilla gave to the dispatcher, so
 * reading them back here is correct and needs no new construction.
 *
 * <p>Those accessors are what let {@code HeldItemRendererMixin} build a complete {@code Context} for
 * the first-person hand renderer instead of leaving the feature disabled.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

	@Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void dmz$selectPlayerRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (entity instanceof Player player) {
			DMZPlayerRenderer<?> renderer = EntityPreviewRenderContext.isRendering()
					? DMZRendererCache.getTPRenderer(player) : DMZRendererCache.getRenderer(player);
			if (renderer != null) cir.setReturnValue(renderer);
		}
	}

	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void dmz$selectExtractedPlayerRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (state instanceof GeoEntityRenderState geoState && geoState.dmzPlayerRenderer != null) {
			cir.setReturnValue(geoState.dmzPlayerRenderer);
		}
	}


    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void dmz$extractAssimilation(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        com.dragonminez.client.render.effects.NamekAssimilationEffect.extract(entity, cir.getReturnValue(), partialTick);
        com.dragonminez.client.render.effects.GodRitualShellEffect.extract(entity, cir.getReturnValue(), partialTick);
        com.dragonminez.client.render.effects.CandyEffect.extract(entity, cir.getReturnValue(), partialTick);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void dmz$hideDimensionalWarp(EntityRenderState state,
            net.minecraft.client.renderer.state.level.CameraRenderState camera, double x, double y, double z,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, CallbackInfo ci) {
        Integer entityId = state.getRenderData(DMZ_ENTITY_ID);
        if (entityId != null && com.dragonminez.client.render.effects.DimensionalShatterEffect.isHidden(entityId)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.util.context.ContextKey<Integer> DMZ_ENTITY_ID =
            new net.minecraft.util.context.ContextKey<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("dragonminez", "effect_entity_id"));

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void dmz$captureEffectEntityId(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        cir.getReturnValue().setRenderData(DMZ_ENTITY_ID, entity.getId());
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dmz$submitCandy(net.minecraft.client.renderer.entity.EntityRenderer renderer, EntityRenderState state,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        var effects = com.dragonminez.client.render.effects.NamekAssimilationEffect.wrap(state,
                com.dragonminez.client.render.effects.GodRitualShellEffect.wrap(state, collector));
        if (!com.dragonminez.client.render.effects.CandyEffect.submitInstead(state, pose, effects, camera))
            renderer.submit(state, pose, effects, camera);
    }

    // ---- Resource reload hook ----

	@Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	public void dmz$onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, EntityRendererProvider.Context context) {
		DMZRendererCache.onResourceReload(context);
	}
}
