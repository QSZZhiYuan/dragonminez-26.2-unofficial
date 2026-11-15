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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartySavedData extends SavedData {
	private static final String FILE_NAME = "dragonminez_parties";

	public static final SavedDataType<PartySavedData> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath("dragonminez", "parties"),
			PartySavedData::new,
			CompoundTag.CODEC.xmap(
					tag -> load(tag, null),
					data -> data.writeTo(new CompoundTag(), null)));

	private final Map<UUID, PartyInstance> parties = new HashMap<>();
	private final Map<UUID, UUID> playerPartyMap = new HashMap<>();

	public PartySavedData() {
	}

	public static PartySavedData get(MinecraftServer server) {
		SavedDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(TYPE);
	}

	public static PartySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		PartySavedData data = new PartySavedData();
		ListTag partiesList = tag.getListOrEmpty("Parties");
		for (int i = 0; i < partiesList.size(); i++) {
			CompoundTag partyTag = partiesList.getCompoundOrEmpty(i);
			UUID partyId = partyTag.read("PartyId", UUIDUtil.CODEC).orElseThrow(() -> new IllegalStateException("Party entry missing PartyId"));
			UUID leaderId = partyTag.read("LeaderId", UUIDUtil.CODEC).orElseThrow(() -> new IllegalStateException("Party entry missing LeaderId"));
			boolean pvpEnabled = partyTag.contains("PvpEnabled") && partyTag.getBooleanOr("PvpEnabled", false);
			ListTag membersList = partyTag.getListOrEmpty("Members");
			List<UUID> members = new ArrayList<>();
			for (int j = 0; j < membersList.size(); j++) {
				CompoundTag mTag = membersList.getCompoundOrEmpty(j);
				members.add(mTag.read("Id", UUIDUtil.CODEC).orElseThrow(() -> new IllegalStateException("Party member entry missing Id")));
			}
			PartyInstance instance = new PartyInstance(partyId, leaderId, members, pvpEnabled);
			data.parties.put(partyId, instance);
			for (UUID memberId : members) {
				data.playerPartyMap.put(memberId, partyId);
			}
		}
		return data;
	}

	public CompoundTag writeTo(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag partiesList = new ListTag();
		for (PartyInstance instance : parties.values()) {
			CompoundTag partyTag = new CompoundTag();
			partyTag.store("PartyId", UUIDUtil.CODEC, instance.getPartyId());
			partyTag.store("LeaderId", UUIDUtil.CODEC, instance.getLeaderId());
			partyTag.putBoolean("PvpEnabled", instance.isPvpEnabled());
			ListTag membersList = new ListTag();
			for (UUID memberId : instance.getMembers()) {
				CompoundTag mTag = new CompoundTag();
				mTag.store("Id", UUIDUtil.CODEC, memberId);
				membersList.add(mTag);
			}
			partyTag.put("Members", membersList);
			partiesList.add(partyTag);
		}
		tag.put("Parties", partiesList);
		return tag;
	}

	public PartyInstance getPartyOf(UUID playerId) {
		UUID partyId = playerPartyMap.get(playerId);
		return partyId != null ? parties.get(partyId) : null;
	}

	public PartyInstance getParty(UUID partyId) {
		return partyId != null ? parties.get(partyId) : null;
	}

	public PartyInstance createParty(UUID leaderId) {
		UUID partyId = UUID.randomUUID();
		PartyInstance party = new PartyInstance(partyId, leaderId, new ArrayList<>(List.of(leaderId)), false);
		parties.put(partyId, party);
		playerPartyMap.put(leaderId, partyId);
		setDirty();
		return party;
	}

	public void removePlayer(UUID playerId) {
		UUID partyId = playerPartyMap.remove(playerId);
		if (partyId != null) {
			PartyInstance party = parties.get(partyId);
			if (party != null) {
				party.getMembers().remove(playerId);
				if (party.getMembers().isEmpty()) {
					parties.remove(partyId);
				}
			}
			setDirty();
		}
	}

	public void addPlayerToParty(UUID partyId, UUID playerId) {
		PartyInstance party = parties.get(partyId);
		if (party != null && !party.getMembers().contains(playerId)) {
			party.getMembers().add(playerId);
			playerPartyMap.put(playerId, partyId);
			setDirty();
		}
	}


	public static class PartyInstance {
		private final UUID partyId;
		private UUID leaderId;
		private final List<UUID> members;
		private boolean pvpEnabled;

		public PartyInstance(UUID partyId, UUID leaderId, List<UUID> members, boolean pvpEnabled) {
			this.partyId = partyId;
			this.leaderId = leaderId;
			this.members = new ArrayList<>(members);
			this.pvpEnabled = pvpEnabled;
		}

		public UUID getPartyId() {
			return partyId;
		}

		public UUID getLeaderId() {
			return leaderId;
		}

		public List<UUID> getMembers() {
			return members;
		}

		public boolean isPvpEnabled() {
			return pvpEnabled;
		}

		@java.lang.SuppressWarnings("all")
		public void setLeaderId(final UUID leaderId) {
			this.leaderId = leaderId;
		}

		@java.lang.SuppressWarnings("all")
		public void setPvpEnabled(final boolean pvpEnabled) {
			this.pvpEnabled = pvpEnabled;
		}
	}
}
