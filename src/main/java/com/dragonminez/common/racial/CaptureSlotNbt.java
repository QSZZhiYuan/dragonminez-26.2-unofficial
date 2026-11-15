package com.dragonminez.common.racial;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CaptureSlotNbt {

	private CaptureSlotNbt() {
	}

	static CompoundTag toNbt(String bonusName, UUID sourceId, String sourceName,
							  Map<String, Integer> grantedStats, long acquiredAtGameTime) {
		CompoundTag tag = new CompoundTag();
		tag.putString("BonusName", bonusName);
		if (sourceId != null) tag.store("SourceId", net.minecraft.core.UUIDUtil.CODEC, sourceId);
		tag.putString("SourceName", sourceName == null ? "" : sourceName);
		CompoundTag statsTag = new CompoundTag();
		for (Map.Entry<String, Integer> entry : grantedStats.entrySet()) statsTag.putInt(entry.getKey(), entry.getValue());
		tag.put("GrantedStats", statsTag);
		tag.putLong("AcquiredAtGameTime", acquiredAtGameTime);
		return tag;
	}

	static Fields fromNbt(CompoundTag tag) {
		String bonusName = tag.getStringOr("BonusName", "");
		UUID sourceId = tag.read("SourceId", net.minecraft.core.UUIDUtil.LENIENT_CODEC).orElse(null);
		String sourceName = tag.getStringOr("SourceName", "");
		Map<String, Integer> grantedStats = new HashMap<>();
		CompoundTag statsTag = tag.getCompoundOrEmpty("GrantedStats");
		for (String key : statsTag.keySet()) grantedStats.put(key, statsTag.getIntOr(key, 0));
		long acquiredAtGameTime = tag.getLongOr("AcquiredAtGameTime", 0L);
		return new Fields(bonusName, sourceId, sourceName, grantedStats, acquiredAtGameTime);
	}

	record Fields(String bonusName, UUID sourceId, String sourceName, Map<String, Integer> grantedStats,
				  long acquiredAtGameTime) {
	}
}
