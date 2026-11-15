package com.dragonminez.client.init.entities.renderer.rr;

import com.dragonminez.Reference;
import com.dragonminez.client.render.GeoEntityRenderState;
import com.dragonminez.client.render.layer.RedRibbonOutfitLayer;
import com.dragonminez.client.util.SkinCacheManager;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

/**
 * Red Ribbon soldier: same as {@link RedRibbonRenderer} plus the per-soldier skin texture.
 *
 * <h2>GeckoLib 5 migration</h2>
 * The texture is chosen from the render state only, so the resolved location is captured in
 * {@code addRenderData} (which still receives the live entity) on a {@code DataTicket} and read
 * back in {@code getTextureLocation}.
 */
public class RedRibbonSoldierRenderer<T extends RedRibbonSoldierEntity> extends RedRibbonRenderer<T> {

    /** Per-entity texture carried on the render state. */
    private static final com.geckolib.constant.dataticket.DataTicket<Identifier> DMZ_TEXTURE =
            com.geckolib.constant.dataticket.DataTicket.create("dmz_rr_soldier_texture", Identifier.class);
    private static final Identifier SOLDIER_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
            "textures/entity/enemies/red_ribbon_soldier.png");

    public RedRibbonSoldierRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager);
        this.shadowRadius = 0.4f;
        this.withRenderLayer(new RedRibbonOutfitLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(GeoEntityRenderState renderState) {
        Identifier captured = renderState.getGeckolibData(DMZ_TEXTURE);
        if (captured != null) return captured;
        return super.getTextureLocation(renderState);
    }

    @Override
    public void addRenderData(T animatable, Void unused, GeoEntityRenderState renderState, float partialTick) {
        super.addRenderData(animatable, unused, renderState, partialTick);
        Identifier texture = SkinCacheManager.resolveTexture(animatable.getSkinOwner());
        // Keep the soldier recognizable while its optional online skin is unavailable.
        if (texture.equals(DefaultPlayerSkin.getDefaultTexture())) texture = SOLDIER_TEXTURE;
        renderState.addGeckolibData(DMZ_TEXTURE, texture);
    }

    @Override
    public RenderType getRenderType(GeoEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
