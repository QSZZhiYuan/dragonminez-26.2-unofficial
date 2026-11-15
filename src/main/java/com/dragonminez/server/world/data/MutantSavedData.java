package com.dragonminez.server.world.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MutantSavedData extends SavedData {
	private static final String FILE_NAME = "dragonminez_mutants";

	public static final SavedDataType<MutantSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("dragonminez", "mutants"),
			MutantSavedData::new,
			CompoundTag.CODEC.xmap(
					tag -> load(tag, null),
					data -> data.writeTo(new CompoundTag(), null)));

	private final Set<UUID> holders = new HashSet<>();
	private long nextRollTick = -1L;

	public MutantSavedData() {
	}

	public static MutantSavedData get(MinecraftServer server) {
		SavedDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(TYPE);
	}

	public static MutantSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		MutantSavedData data = new MutantSavedData();
		ListTag holdersList = tag.getListOrEmpty("Holders");
		for (int i = 0; i < holdersList.size(); i++) {
			CompoundTag holderTag = holdersList.getCompoundOrEmpty(i);
			holderTag.read("Id", UUIDUtil.CODEC).ifPresent(data.holders::add);
		}
		data.nextRollTick = tag.getLongOr("NextRollTick", 0L);
		return data;
	}

	public CompoundTag writeTo(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag holdersList = new ListTag();
		for (UUID holder : holders) {
			CompoundTag holderTag = new CompoundTag();
			holderTag.store("Id", UUIDUtil.CODEC, holder);
			holdersList.add(holderTag);
		}
		tag.put("Holders", holdersList);
		tag.putLong("NextRollTick", nextRollTick);
		return tag;
	}

	public boolean isHolder(UUID playerId) {
		return holders.contains(playerId);
	}

	public Set<UUID> getHolders() {
		return new HashSet<>(holders);
	}

	public int count() {
		return holders.size();
	}

	public void addHolder(UUID playerId) {
		if (holders.add(playerId)) setDirty();
	}

	public void removeHolder(UUID playerId) {
		if (holders.remove(playerId)) setDirty();
	}

	public void setNextRollTick(long tick) {
		this.nextRollTick = tick;
		setDirty();
	}

	@java.lang.SuppressWarnings("all")
	public long getNextRollTick() {
		return this.nextRollTick;
	}
}
