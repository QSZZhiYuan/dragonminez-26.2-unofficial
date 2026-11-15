package com.dragonminez.server.world.npc;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NPCPlacementSavedData extends SavedData {
	private static final String DATA_NAME = "dragonminez_npc_placements";
	private static final String PLACEMENTS_KEY = "placements";

	public static final SavedDataType<NPCPlacementSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("dragonminez", "npc_placements"),
			NPCPlacementSavedData::new,
			CompoundTag.CODEC.xmap(
					tag -> load(tag, null),
					data -> data.writeTo(new CompoundTag(), null)));

	private final Map<String, UUID> placements = new HashMap<>();

	public static NPCPlacementSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean hasPlacement(String placementId) {
		return placements.containsKey(placementId);
	}

	public Optional<UUID> getEntityUuid(String placementId) {
		return Optional.ofNullable(placements.get(placementId));
	}

	public void markSpawned(String placementId, UUID uuid) {
		if (placementId == null || placementId.isBlank() || uuid == null) {
			return;
		}
		placements.put(placementId, uuid);
		setDirty();
	}

	public void clear(String placementId) {
		if (placements.remove(placementId) != null) {
			setDirty();
		}
	}

	public static NPCPlacementSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		NPCPlacementSavedData data = new NPCPlacementSavedData();
		CompoundTag placementsTag = tag.getCompoundOrEmpty(PLACEMENTS_KEY);
		for (String placementId : placementsTag.keySet()) {
			try {
				data.placements.put(placementId, UUID.fromString(placementsTag.getStringOr(placementId, "")));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	public @NotNull CompoundTag writeTo(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag placementsTag = new CompoundTag();
		for (Map.Entry<String, UUID> entry : placements.entrySet()) {
			placementsTag.putString(entry.getKey(), entry.getValue().toString());
		}
		tag.put(PLACEMENTS_KEY, placementsTag);
		return tag;
	}
}
