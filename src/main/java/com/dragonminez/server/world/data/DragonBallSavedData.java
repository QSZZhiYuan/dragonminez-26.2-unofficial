package com.dragonminez.server.world.data;

import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DragonBallSavedData extends SavedData {

	private static final Codec<DragonBallSavedData> CODEC = CompoundTag.CODEC.xmap(
			tag -> load(tag, null),
			data -> data.writeTo(new CompoundTag(), null));

	public static final SavedDataType<DragonBallSavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("dragonminez", "dragon_balls_data"),
			DragonBallSavedData::new,
			CODEC);
	private final Map<String, Map<Integer, List<BlockPos>>> activeBallsBySet = new HashMap<>();
	private final Map<String, Map<Integer, List<BlockPos>>> pendingBallsBySet = new HashMap<>();
	private final Set<String> firstSpawnedSetIds = new HashSet<>();

	public DragonBallSavedData() {
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			activeBallsBySet.put(definition.getId(), createStarMap(definition));
			pendingBallsBySet.put(definition.getId(), createStarMap(definition));
		}
	}

	private static Map<Integer, List<BlockPos>> createStarMap(DragonBallSetDefinition definition) {
		Map<Integer, List<BlockPos>> map = new HashMap<>();
		for (int star : definition.getStars()) {
			map.put(star, new ArrayList<>());
		}
		return map;
	}

	private static Map<Integer, List<BlockPos>> createStarMapForSetId(String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		return definition == null ? new HashMap<>() : createStarMap(definition);
	}

	public static DragonBallSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public Map<Integer, List<BlockPos>> getActiveBalls(String setId) {
		return activeBallsBySet.computeIfAbsent(setId, DragonBallSavedData::createStarMapForSetId);
	}

	public Map<Integer, List<BlockPos>> getPendingBalls(String setId) {
		return pendingBallsBySet.computeIfAbsent(setId, DragonBallSavedData::createStarMapForSetId);
	}

	public List<BlockPos> getAllKnownPositionsForRadar(String setId) {
		List<BlockPos> allPos = new ArrayList<>();
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null) return allPos;
		Map<Integer, List<BlockPos>> active = getActiveBalls(setId);
		Map<Integer, List<BlockPos>> pending = getPendingBalls(setId);
		for (int star : definition.getStars()) {
			allPos.addAll(active.getOrDefault(star, List.of()));
			allPos.addAll(pending.getOrDefault(star, List.of()));
		}
		return allPos;
	}

	public boolean isFirstSpawnComplete(String setId) {
		return firstSpawnedSetIds.contains(setId);
	}

	public void setFirstSpawnComplete(String setId, boolean value) {
		if (value) firstSpawnedSetIds.add(setId);
		else firstSpawnedSetIds.remove(setId);
		setDirty();
	}

	public static DragonBallSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		DragonBallSavedData data = new DragonBallSavedData();
		if (tag.contains("SetData")) {
			CompoundTag setData = tag.getCompoundOrEmpty("SetData");
			for (String setId : setData.keySet()) {
				CompoundTag entry = setData.getCompoundOrEmpty(setId);
				loadMap(entry.getListOrEmpty("Active"), data.getActiveBalls(setId));
				loadMap(entry.getListOrEmpty("Pending"), data.getPendingBalls(setId));
				if (entry.getBooleanOr("FirstSpawned", false)) data.firstSpawnedSetIds.add(setId);
			}
		} else {
			loadLegacySet(tag, data, "earth", "ActiveEarth", "PendingEarth", "FirstSpawnEarth");
			loadLegacySet(tag, data, "namek", "ActiveNamek", "PendingNamek", "FirstSpawnNamek");
		}
		return data;
	}

	private static void loadLegacySet(CompoundTag tag, DragonBallSavedData data, String setId, String activeKey, String pendingKey, String firstKey) {
		if (tag.contains(activeKey)) loadMap(tag.getListOrEmpty(activeKey), data.getActiveBalls(setId));
		if (tag.contains(pendingKey)) loadMap(tag.getListOrEmpty(pendingKey), data.getPendingBalls(setId));
		if (tag.getBooleanOr(firstKey, false)) data.firstSpawnedSetIds.add(setId);
	}

	public @NotNull CompoundTag writeTo(CompoundTag tag, HolderLookup.Provider registries) {
		CompoundTag setData = new CompoundTag();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			String setId = definition.getId();
			CompoundTag entry = new CompoundTag();
			entry.put("Active", saveMap(getActiveBalls(setId)));
			entry.put("Pending", saveMap(getPendingBalls(setId)));
			entry.putBoolean("FirstSpawned", firstSpawnedSetIds.contains(setId));
			setData.put(setId, entry);
		}
		tag.put("SetData", setData);
		return tag;
	}

	private static void loadMap(ListTag list, Map<Integer, List<BlockPos>> map) {
		for (int i = 0; i < list.size(); i++) {
			CompoundTag item = list.getCompoundOrEmpty(i);
			int star = item.getIntOr("Star", 0);
			// BlockPos.CODEC writes an IntArrayTag. Reading it as a compound replaced
			// every saved position with ZERO after a world reload.
			var encoded = item.get("Pos");
			if (encoded == null) continue;
			BlockPos.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).result().ifPresent(pos -> {
				List<BlockPos> positions = map.computeIfAbsent(star, ignored -> new ArrayList<>());
				if (!positions.contains(pos)) positions.add(pos);
			});
		}
	}

	private static ListTag saveMap(Map<Integer, List<BlockPos>> map) {
		ListTag list = new ListTag();
		for (Map.Entry<Integer, List<BlockPos>> entry : map.entrySet()) {
			for (BlockPos pos : entry.getValue()) {
				CompoundTag item = new CompoundTag();
				item.putInt("Star", entry.getKey());
				net.minecraft.core.BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, pos)
						.result().ifPresent(encoded -> item.put("Pos", encoded));
				list.add(item);
			}
		}
		return list;
	}
}
