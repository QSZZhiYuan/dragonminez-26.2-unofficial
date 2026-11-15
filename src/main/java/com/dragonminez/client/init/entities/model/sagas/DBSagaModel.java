package com.dragonminez.client.init.entities.model.sagas;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashMap;
import java.util.Map;

/**
 * Model/texture selection for saga enemies, with on-disk existence fallbacks.
 *
 * <h2>GeckoLib 5 migration</h2>
 * {@code getModelResource} / {@code getTextureResource} no longer receive the entity, so the two
 * per-entity values this class keys off ({@code getGeckolibModelName()}, {@code getTextureVariant()})
 * are captured in {@link #addAdditionalStateData(Object, Object, GeoRenderState)} and read back from
 * the render state. The entity type comes from {@link EntityRenderState#entityType}.
 */
public class DBSagaModel<T extends DBSagasEntity> extends GeoModel<T> {

    private static final Map<Identifier, Boolean> RESOURCE_CACHE = new HashMap<>();

    private static final DataTicket<String> DMZ_MODEL_NAME = DataTicket.create("dmz_saga_model_name", String.class);
    private static final DataTicket<Integer> DMZ_TEXTURE_VARIANT = DataTicket.create("dmz_saga_texture_variant", Integer.class);

    @Override
    public void addAdditionalStateData(T animatable, Object unused, GeoRenderState renderState) {
        renderState.addGeckolibData(DMZ_MODEL_NAME, animatable.getGeckolibModelName());
        renderState.addGeckolibData(DMZ_TEXTURE_VARIANT, animatable.getTextureVariant());
    }

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String modelName = renderState.getGeckolibData(DMZ_MODEL_NAME);
        if (modelName == null) modelName = "saga_base";
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/sagas/" + modelName );

        boolean exists = RESOURCE_CACHE.computeIfAbsent(original, key -> resourceExists(
                com.dragonminez.client.init.entities.model.GeoAssetPaths.modelFile(key)));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/robotxv");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((EntityRenderState) renderState).entityType).getPath();
        Integer variantBoxed = renderState.getGeckolibData(DMZ_TEXTURE_VARIANT);
        int variant = variantBoxed == null ? 0 : variantBoxed;

        String variantSuffix = (variant == 0) ? "" : "_" + variant;
        Identifier variantTexture = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + variantSuffix + ".png");

        boolean variantExists = RESOURCE_CACHE.computeIfAbsent(variantTexture, this::resourceExists);
        if (variantExists) {
            return variantTexture;
        }

        if (variant > 0) {
            Identifier baseTexture = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/" + name + ".png");
            boolean baseExists = RESOURCE_CACHE.computeIfAbsent(baseTexture, this::resourceExists);
            if (baseExists) {
                return baseTexture;
            }
        }

        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/robotxv.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/sagas/saga_base");
    }

    @Override
    public Identifier[] getAnimationResourceFallbacks(T animatable) {
        return new Identifier[]{Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/movement"),
                Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/combat"),
                Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/skp"),
                Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/races/evs")};
    }

    private boolean resourceExists(Identifier location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    public static void clearCache() {
        RESOURCE_CACHE.clear();
    }
}
