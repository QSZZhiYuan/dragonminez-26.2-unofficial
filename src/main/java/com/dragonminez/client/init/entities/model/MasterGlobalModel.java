package com.dragonminez.client.init.entities.model;


import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashMap;
import java.util.Map;

public class MasterGlobalModel<T extends MastersEntity> extends GeoModel<T> {

    private static final Map<String, Boolean> MODEL_CACHE = new HashMap<>();
    private static final Map<String, Boolean> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, Boolean> ANIM_CACHE = new HashMap<>();

    @Override
    public Identifier getModelResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/master/" + name );

        boolean exists = MODEL_CACHE.computeIfAbsent(name, k -> resourceExists(GeoAssetPaths.modelFile(original)));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/enemies/robotxv");
    }

    @Override
    public Identifier getTextureResource(com.geckolib.renderer.base.GeoRenderState renderState) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(((net.minecraft.client.renderer.entity.state.EntityRenderState) renderState).entityType).getPath();
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/master/" + name + ".png");

        boolean exists = TEXTURE_CACHE.computeIfAbsent(name, k -> resourceExists(original));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/enemies/robotxv.png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(animatable.getType()).getPath();
        Identifier original = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/master/" + name );

        boolean exists = ANIM_CACHE.computeIfAbsent(name, k -> resourceExists(GeoAssetPaths.animationFile(original)));
        return exists ? original : Identifier.fromNamespaceAndPath(Reference.MOD_ID, "entity/master/master_goku");
    }

    private boolean resourceExists(Identifier location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    public static void clearCache() {
        MODEL_CACHE.clear();
        TEXTURE_CACHE.clear();
        ANIM_CACHE.clear();
    }
}
