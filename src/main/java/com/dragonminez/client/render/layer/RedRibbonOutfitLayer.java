package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.GeoEntityRenderState;
import com.dragonminez.common.init.entities.redribbon.RedRibbonSoldierEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Submits the uniform over the soldier's skin using the same captured animated pose.
 * A texture hook alone does not submit an additional pass in GeckoLib 5.
 */
public class RedRibbonOutfitLayer<T extends RedRibbonSoldierEntity>
        extends GeoRenderLayer<T, Void, GeoEntityRenderState> {

    private static final Identifier OUTFIT_TEXTURE =
            Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/redribbon_outfit.png");

    public RedRibbonOutfitLayer(GeoRenderer<T, Void, GeoEntityRenderState> renderer) {
        super(renderer);
    }

    /** The texture this layer paints with. */
    @Override
    protected Identifier getTextureResource(GeoEntityRenderState renderState) {
        return OUTFIT_TEXTURE;
    }

    @Override
    public void submitRenderTask(RenderPassInfo<GeoEntityRenderState> info, SubmitNodeCollector collector) {
        if (!info.willRender()) return;
        getRenderer().submitRenderTasks(info, collector.order(1),
                RenderTypes.entityCutout(getTextureResource(info.renderState())));
    }
}
