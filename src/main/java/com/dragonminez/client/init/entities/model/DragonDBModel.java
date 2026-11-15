
package com.dragonminez.client.init.entities.model;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonAssetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * Asset selection for the dragon models.
 *
 * <h2>GeckoLib 5 migration</h2>
 * {@code getModelResource} / {@code getTextureResource} now receive a {@link GeoRenderState}, not the
 * entity, so the dragon-specific asset definition has to be resolved while the entity is still
 * reachable. {@link #addAdditionalStateData(Object, Object, GeoRenderState)} is that hook: it runs
 * during render-state extraction with the live entity, so the resolved assets are stashed there and
 * read back at texture time. The entity type comes straight off {@link EntityRenderState#entityType}.
 */
public class DragonDBModel<T extends DragonWishEntity> extends GeoModel<T> {

    private static final DataTicket<DragonAssetDefinition> DMZ_DRAGON_ASSETS =
            DataTicket.create("dmz_dragon_assets", DragonAssetDefinition.class);

    @Override
    public void addAdditionalStateData(T animatable, Object unused, GeoRenderState renderState) {
        renderState.addGeckolibData(DMZ_DRAGON_ASSETS, resolveAssets(animatable));
    }

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        DragonAssetDefinition assets = renderState.getGeckolibData(DMZ_DRAGON_ASSETS);
        if (assets != null && assets.getModelPath().isPresent()) return assetIdentifier(assets.getModelPath().get());
        String name = entityName(renderState);
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/dragon/" + name );
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        DragonAssetDefinition assets = renderState.getGeckolibData(DMZ_DRAGON_ASSETS);
        if (assets != null && assets.getTexturePath().isPresent()) return assetIdentifier(assets.getTexturePath().get());
        String name = entityName(renderState);
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/dragon/" + name + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        DragonAssetDefinition assets = resolveAssets(animatable);
        if (assets != null && assets.getAnimationPath().isPresent()) return assetIdentifier(assets.getAnimationPath().get());
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/dragon/" + name );
    }

    private static String entityName(GeoRenderState renderState) {
        EntityRenderState ers = (EntityRenderState) renderState;
        return BuiltInRegistries.ENTITY_TYPE.getKey(ers.entityType).getPath();
    }

    private DragonAssetDefinition resolveAssets(T animatable) {
        DragonDefinition definition = animatable.getDragonDefinition();
        return definition == null ? null : definition.resolveAssetDefinition();
    }

    static Identifier assetIdentifier(String path) {
        return path.indexOf(':') >= 0 ? Identifier.parse(path)
                : Identifier.fromNamespaceAndPath(Reference.MOD_ID, path);
    }
}
