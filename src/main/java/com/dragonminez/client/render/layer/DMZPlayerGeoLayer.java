package com.dragonminez.client.render.layer;

import com.dragonminez.client.render.DMZPlayerRenderData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Base class for DMZ player render layers.
 *
 * <p>Absorbs the GeckoLib 4 → 5 layer API change in one place. The old hook
 * <pre>{@code
 * render(PoseStack, T animatable, BakedGeoModel, RenderType,
 *        SubmitNodeCollector, VertexConsumer, float partialTick, int light, int overlay)
 * }</pre>
 * no longer exists; layers now run through {@code preRender(RenderPassInfo, SubmitNodeCollector)}
 * and <b>no longer receive the animatable</b>. Subclasses implement
 * {@link #render(DMZLayerContext)}, which carries the same information plus the
 * {@link DMZPlayerRenderData} captured by the renderer.
 *
 * <h2>Why R is {@link GeoRenderState}</h2>
 * {@code GeoRenderLayer<T, O, R extends GeoRenderState>} constrains R to {@code GeoRenderState}.
 * Vanilla's {@code EntityRenderState} hierarchy only satisfies that bound at runtime (GeckoLib
 * adds the interface via {@code EntityRenderStateMixin}), so declaring the layer with
 * {@code LivingEntityRenderState} does not compile. Render state *cast* happens wherever vanilla
 * fields are needed, via {@link DMZLayerContext#livingState()}.
 */
public abstract class DMZPlayerGeoLayer<T extends AbstractClientPlayer & GeoAnimatable>
        extends GeoRenderLayer<T, Void, GeoRenderState> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected DMZPlayerGeoLayer(GeoRenderer<T, ?, ?> renderer) {
        super((GeoRenderer) renderer);
    }

    /** Convenience bundle handed to subclasses. */
    public static final class DMZLayerContext {
        private final RenderPassInfo<GeoRenderState> info;
        private final SubmitNodeCollector collector;
        private final DMZPlayerRenderData data;

        DMZLayerContext(RenderPassInfo<GeoRenderState> info,
                        SubmitNodeCollector collector,
                        DMZPlayerRenderData data) {
            this.info = info;
            this.collector = collector;
            this.data = data;
        }

        public RenderPassInfo<GeoRenderState> info() { return info; }
        public SubmitNodeCollector collector() { return collector; }
        public DMZPlayerRenderData data() { return data; }

        public AbstractClientPlayer player() { return data.player(); }
        public com.dragonminez.common.stats.StatsData stats() { return data.stats(); }
        public com.mojang.blaze3d.vertex.PoseStack poseStack() { return info.poseStack(); }
        public com.geckolib.cache.model.BakedGeoModel model() { return info.model(); }
        public int packedLight() { return info.packedLight(); }
        public int packedOverlay() { return info.packedOverlay(); }
        public float partialTick() { return info.renderState().getPartialTick(); }
        public boolean isHudPortrait() { return data.hudPortrait(); }
        public int entityId() { return data.entityId(); }
        public long gameTime() { return data.gameTime(); }

        /** The vanilla render state, for layers that need {@code LivingEntityRenderState} fields. */
        @SuppressWarnings("unchecked")
        public net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState() {
            return (net.minecraft.client.renderer.entity.state.LivingEntityRenderState) info.renderState();
        }
    }

    @Override
    public void preRender(RenderPassInfo<GeoRenderState> info, SubmitNodeCollector collector) {
        DMZPlayerRenderData data = info.renderState().getGeckolibData(DMZPlayerRenderData.TICKET);
        if (data == null) return;
        if (com.dragonminez.client.render.effects.CandyEffect.isCandy(data.player())) return;

        DMZLayerContext ctx = new DMZLayerContext(info, collector, data);
        if (skipInHudPortrait() && ctx.isHudPortrait()) return;

        var previous = com.dragonminez.client.render.shader.TransformationOutlineRenderer.setCapturePlayer(ctx.player());
        try { render(ctx); }
        finally { com.dragonminez.client.render.shader.TransformationOutlineRenderer.setCapturePlayer(previous); }
    }

    /**
     * Called once per frame with the full layer context.
     *
     * @param ctx pose stack, baked model, light/overlay, submit collector and player data
     */
    protected abstract void render(DMZLayerContext ctx);

    /**
     * Whether this layer is skipped while rendering the HUD character portrait.
     * Mirrors the old {@code applyRenderLayers} whitelist in {@code DMZPlayerRenderer}.
     */
    protected boolean skipInHudPortrait() {
        return true;
    }
}
