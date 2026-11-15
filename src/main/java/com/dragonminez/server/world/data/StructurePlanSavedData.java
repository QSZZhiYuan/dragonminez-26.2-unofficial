package com.dragonminez.server.world.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StructurePlanSavedData extends SavedData {
	private static final String NAME = "dragonminez_structure_plan";

	public static final SavedDataType<StructurePlanSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("dragonminez", "structure_plan"),
			StructurePlanSavedData::new,
			CompoundTag.CODEC.xmap(
					tag -> load(tag, null),
					data -> data.writeTo(new CompoundTag(), null)));

	private boolean resolved = false;
	private final Map<Integer, ChunkPos> positions = new HashMap<>();

	public static StructurePlanSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isResolved() {
		return resolved;
	}

	public Map<Integer, ChunkPos> getPositions() {
		return Collections.unmodifiableMap(positions);
	}

	/**
	 * Stores the planned positions. {@code complete} marks the plan as fully
	 * resolved; when false, missing structures are searched again on next load.
	 */
	public void setPositions(Map<Integer, ChunkPos> newPositions, boolean complete) {
		this.positions.clear();
		if (newPositions != null) this.positions.putAll(newPositions);
		this.resolved = complete;
		setDirty();
	}

	/** Drops one structure's planned position so it can be relocated. */
	public void removePosition(int salt) {
		this.positions.remove(salt);
		this.resolved = false;
		setDirty();
	}

	public static StructurePlanSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		StructurePlanSavedData data = new StructurePlanSavedData();
		data.resolved = tag.getBooleanOr("resolved", false);
		ListTag list = tag.getListOrEmpty("positions");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompoundOrEmpty(i);
			data.positions.put(entry.getIntOr("salt", 0), new ChunkPos(entry.getIntOr("x", 0), entry.getIntOr("z", 0)));
		}
		return data;
	}

	public @NotNull CompoundTag writeTo(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("resolved", resolved);
		ListTag list = new ListTag();
		for (Map.Entry<Integer, ChunkPos> e : positions.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("salt", e.getKey());
			entry.putInt("x", e.getValue().x());
			entry.putInt("z", e.getValue().z());
			list.add(entry);
		}
		tag.put("positions", list);
		return tag;
	}
}
