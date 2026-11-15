package com.dragonminez.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads every {@code .json} file under a directory into a {@code Map<id, JsonElement>}.
 *
 * <h2>Why this exists</h2>
 * 26.2 turned {@link SimpleJsonResourceReloadListener} into a codec-driven loader: it derives the
 * prepared map type from a {@code Codec<T>} and no longer performs raw JSON collection. DMZ has a few
 * listeners whose payloads are decoded by Gson type adapters (hand-written {@code Wish} subtypes,
 * space-pod definitions), so they cannot express a codec.
 *
 * <p>They still extend {@link SimplePreparableReloadListener} — which remains generic over the
 * prepared type — and use this helper to obtain exactly the map
 * {@code SimpleJsonResourceReloadListener} used to hand them.
 */
public final class SimpleJsonResourceReloadListenerSupport {

    private SimpleJsonResourceReloadListenerSupport() {}

    /** Scans {@code data/<directory>/**.json} (and the mod data folders) into a map keyed by file id. */
    public static Map<Identifier, JsonElement> scanJson(ResourceManager manager, String directory) {
        return scanJson(manager, directory, ".json");
    }

    public static Map<Identifier, JsonElement> scanJson(ResourceManager manager, String directory, String extension) {
        FileToIdConverter lister = FileToIdConverter.json(directory);
        Map<Identifier, JsonElement> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier id = lister.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(id, JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                // Match the vanilla behaviour: log and keep the remaining files loadable.
                org.slf4j.LoggerFactory.getLogger(SimpleJsonResourceReloadListenerSupport.class)
                        .error("Couldn't parse data file '{}' from '{}'", id, entry.getKey(), e);
            }
        }
        return result;
    }
}
