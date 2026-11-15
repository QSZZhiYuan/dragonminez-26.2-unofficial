package com.dragonminez.client.render;

import com.dragonminez.client.render.layer.*;
import com.dragonminez.client.init.entities.model.DmzHeadLook;
import com.dragonminez.client.model.DMZPlayerModel;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Player renderer for all races / forms.
 *
 * <h2>26.2 + GeckoLib 5 migration</h2>
 * The render pipeline was rebuilt around render states and deferred submission:
 * <ul>
 *   <li>{@code render(...)} → {@code submit(renderState, PoseStack, SubmitNodeCollector, CameraRenderState)}</li>
 *   <li>{@code SubmitNodeCollector} is gone; drawing is queued on a {@code SubmitNodeCollector}</li>
 *   <li>{@code addRenderLayer(...)} is gone → {@code withRenderLayer(...)} (chainable)</li>
 *   <li>{@code addRenderData(...)} is the only hook that still receives the animatable — this is
 *       where {@link DMZPlayerRenderData} is captured for the layers to read back</li>
 * </ul>
 *
 * <h2>Generic bound note</h2>
 * Layers are declared against {@link GeoRenderState} (GeckoLib's bound for
 * {@code GeoRenderLayer}), while this renderer is bound to {@link LivingEntityRenderState}
 * (vanilla's bound for {@code GeoEntityRenderer}). The two are the same object at runtime —
 * {@code EntityRenderStateMixin} adds the {@code GeoRenderState} interface — so layer
 * registration goes through an unchecked bridge.
 */
public class DMZPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable>
		extends GeoEntityRenderer<T, GeoEntityRenderState> {

	public DMZPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
		super(renderManager, model);

		this.registerLayer(new DMZPlayerItemInHandLayer<T>(this, renderManager));
		this.registerLayer(new DMZPlayerArmorLayer<T>(this, renderManager));
		this.registerLayer(new DMZCapeLayer<T>(this, renderManager));
		this.registerLayer(new DMZWeightCapeLayer<T>(this, renderManager));
		this.registerLayer(new DMZCustomArmorLayer<T>(this));
		this.registerLayer(new DMZRacePartsLayer<T>(this));
		this.registerLayer(new DMZWeaponsLayer<T>(this));
		this.registerLayer(new DMZAuraLayer<T>(this));
		this.registerLayer(new DMZSkinLayer<T>(this));
		this.registerLayer(new DMZHairLayer<T>(this));
		this.registerLayer(new DMZThirdPartyLayerForwarder<T>(this));
	}

	/**
	 * Registers a layer declared against {@link GeoRenderState} on this renderer, whose own
	 * render-state parameter is {@link LivingEntityRenderState}.
	 *
	 * <p>Safe at runtime: every {@code EntityRenderState} implements {@code GeoRenderState}
	 * through GeckoLib's {@code EntityRenderStateMixin}, and the layers only ever read the state
	 * this renderer itself created.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void registerLayer(GeoRenderLayer<?, ?, ?> layer) {
		this.renderLayers.addLayer((GeoRenderLayer) layer);
	}

	/**
	 * Captures everything the layers need from the live animatable and stores it on the render
	 * state. Runs once per frame, before any layer is invoked.
	 */
	@Override
	public void addRenderData(T animatable, Void unused, GeoEntityRenderState renderState, float partialTick) {
		renderState.dmzPlayerRenderer = this;
		super.addRenderData(animatable, unused, renderState, partialTick);

		if (animatable == null) return;

		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, animatable)
				.orElse(new StatsData(animatable));

		GeoRenderState geoState = renderState;
		geoState.addGeckolibData(
				DMZPlayerRenderData.TICKET,
				new DMZPlayerRenderData(
						stats,
						animatable,
						animatable.isSpectator(),
						EntityPreviewRenderContext.isHudPortrait(),
						animatable.getId(),
						animatable.level() != null ? animatable.level().getGameTime() : 0L
				)
		);

		// GeckoLib 5 removed GeoModel#setCustomAnimations; the head-look angles (and the
		// body-only "boob" scale) are captured here because this is the last hook that still
		// sees the live animatable, then applied in adjustModelBonesForRender below.
		DmzHeadLook.captureFrom(geoState, animatable, partialTick);
		boolean searchFlightMode = stats.getStatus().getFlightMode() == Status.FLIGHT_SEARCH;
		boolean flightPoseAllowed = !EntityPreviewRenderContext.isRendering()
				&& !com.dragonminez.common.init.entities.ki.GumBindEntity.isClientBound(animatable)
				&& !stats.getStatus().isStunned()
				&& stats.getSkills().isSkillActive("fly")
				&& searchFlightMode;
		renderState.fastFlight = flightPoseAllowed
				&& com.dragonminez.client.events.FlySkillEvent.getInstance().isFlyingFast(animatable);
		renderState.flightPitch = animatable.getViewXRot(partialTick);
		renderState.flightRoll = animatable == Minecraft.getInstance().player
				? com.dragonminez.client.flight.FlightRollHandler.getRoll(partialTick) : 0;
		renderState.flightPivotY = animatable.getBbHeight() / 2;
		renderState.kiSharePose = EntityPreviewRenderContext.isRendering() ? null
				: com.dragonminez.client.systems.kishare.KiSharePose.capture(animatable, stats, partialTick);
	}

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoEntityRenderState> info) {
        super.adjustRenderPose(info);
        var state = info.renderState();
		if (state.fastFlight) {
            var pose = info.poseStack();
            pose.translate(0, state.flightPivotY, 0);
			// Minecraft pitch is positive down, so this sign keeps the body axis aligned to the look vector.
			pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(state.flightPitch));
			pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(state.flightRoll));
            pose.translate(0, -state.flightPivotY, 0);
        }
    }

	/**
	 * GeckoLib 5 replacement for {@code setCustomAnimations}: writes the captured head-look and
	 * scale factors onto this frame's bone snapshots.
	 */
	@Override
	public void adjustModelBonesForRender(RenderPassInfo<GeoEntityRenderState> info, BoneSnapshots bones) {
		super.adjustModelBonesForRender(info, bones);
		if (bones == null) return;
		com.dragonminez.client.systems.kishare.KiSharePose.apply(info.renderState().kiSharePose, bones);
        if (!Boolean.TRUE.equals(info.renderState().getGeckolibData(DmzHeadLook.HEAD_LOCKED))) DmzHeadLook.apply(
				bones,
				info.renderState().getGeckolibData(DmzHeadLook.HEAD_PITCH),
				info.renderState().getGeckolibData(DmzHeadLook.HEAD_YAW));
		Float boobScale = info.renderState().getGeckolibData(DmzHeadLook.BOOB_SCALE);
		if (boobScale != null) {
			bones.ifPresent("boobas", snapshot -> {
				float[] axis = DMZPlayerModel.computeBoobAxisScale(boobScale);
				snapshot.setScale(axis[0], axis[1], axis[2]);
			});
		}
	}

	@Override
	public boolean shouldShowName(T animatable, double distanceToCamera) {
		if (animatable == Minecraft.getInstance().getCameraEntity()) return false;
		return super.shouldShowName(animatable, distanceToCamera);
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
	public GeoEntityRenderState createRenderState(T entity, Void unused) {
		return new GeoEntityRenderState();
	}
}
