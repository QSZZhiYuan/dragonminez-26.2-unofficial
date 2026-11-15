package com.dragonminez.client.init.entities.model;

import net.minecraft.resources.Identifier;

/** GeckoLib uses short cache keys; Minecraft's ResourceManager requires the full file path. */
public final class GeoAssetPaths {
    private GeoAssetPaths() {}

    public static Identifier modelFile(Identifier key) {
        return key.withPath("geckolib/models/" + key.getPath() + ".geo.json");
    }

    public static Identifier animationFile(Identifier key) {
        return key.withPath("geckolib/animations/" + key.getPath() + ".animation.json");
    }
}
