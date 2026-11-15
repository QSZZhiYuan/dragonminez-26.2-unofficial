package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.AuraPose;
import com.dragonminez.client.render.effects.KiChargeEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.client.player.AbstractClientPlayer;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * Aura / spark effect layer.
 *
 * <p>Migrated to the GeckoLib 5 layer contract (see {@link DMZPlayerGeoLayer}).
 */
public class DMZAuraLayer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerGeoLayer<T> {

	public DMZAuraLayer(GeoRenderer<T, ?, ?> entityRendererIn) {
		super(entityRendererIn);
	}

	@Override
	protected void render(DMZLayerContext ctx) {
        if (com.dragonminez.client.render.effects.AfterimageEffect.isRendering()) return;
		if (com.dragonminez.client.render.EntityPreviewRenderContext.isRendering()) return;
		AbstractClientPlayer animatable = (AbstractClientPlayer) ctx.player();
		if (animatable == null || animatable.isSpectator()) return;

		StatsData stats = ctx.stats();
		if (stats == null) return;

		boolean isAuraActive = stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura()
				|| KiChargeEffects.hasChargeVisual(animatable, ctx.partialTick());
		boolean isAndroidChargingForm = stats.getStatus().isAndroidUpgraded()
				&& stats.getStatus().isActionCharging()
				&& stats.getStatus().getSelectedAction() == ActionMode.FORM;

		var character = stats.getCharacter();
		boolean hasLightning = false;

		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null) {
			hasLightning = character.getActiveStackFormData().getHasLightnings();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null) {
			hasLightning = character.getActiveFormData().getHasLightnings();
		}
		hasLightning |= com.dragonminez.client.render.effects.AuraFxState.hasLightning(stats) || KiChargeEffects.burst(animatable, ctx.partialTick()) > 0.0f;

		if (stats.getStatus().isAndroidUpgraded() && !isAndroidChargingForm && !hasLightning) return;
		if (!isAuraActive && !hasLightning) return;
		AuraPose bodyPose = AuraPose.capture(ctx.info());
		// The renderer's adjustRenderPose has already applied fast-flight pitch/roll to the
		// RenderPassInfo pose before layers run. AuraPose.capture reads that model-local pose;
		// applying GeoEntityRenderState's flight transform here would rotate the aura twice.

		boolean suppressSurgeShell = com.dragonminez.client.render.effects.AuraFxState.isBurstAuraSuppressed(
				animatable, stats, ctx.partialTick());
		if (isAuraActive && !isAndroidChargingForm && !suppressSurgeShell) {
			PlayerEffectQueue.addAura(animatable, ctx.model(), bodyPose, ctx.partialTick(), ctx.packedLight());
		}

		if (isAuraActive || hasLightning) {
			PlayerEffectQueue.addSpark(animatable, ctx.model(), bodyPose, ctx.partialTick(), ctx.packedLight());
		}
	}
}
