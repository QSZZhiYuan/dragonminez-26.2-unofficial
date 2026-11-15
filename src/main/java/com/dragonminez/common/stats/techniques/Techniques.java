package com.dragonminez.common.stats.techniques;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Techniques {
    private transient com.dragonminez.common.stats.StatsData statsData;
    public void setStatsData(com.dragonminez.common.stats.StatsData value) { statsData = value; }
    private int racialXp(int amount) { return statsData != null ? com.dragonminez.common.racial.RacialStatUtil.applyTechniqueXpBonus(statsData, amount) : amount; }
    private float racialXp(float amount) { return statsData != null ? com.dragonminez.common.racial.RacialStatUtil.applyTechniqueXpBonus(statsData, amount) : amount; }
	public static final int SLOT_COUNT = 8;
	public static final int MAX_UNLOCKED_TECHNIQUES = 256;

	@Getter
	private final Map<String, TechniqueData> unlockedTechniques = new HashMap<>();
	private final String[] equippedSlots = new String[SLOT_COUNT];
	private int selectedSlot = 0;
	private String chargingTechniqueId = "";
	private float techniqueChargePercent = 0.0f;
	@Setter
	private boolean techniqueCharging = false;
	@Setter
	private boolean chargeHolding = false;

	@Getter
	@Setter
	private transient int homingTargetId = -1;

	private final transient Map<String, Float> fractionalXpRemainder = new HashMap<>();
	public static final String[] OOZARU_LOADOUT = {"mouth_blast", "oozaru_slam", "rage_scream"};
	public static final int FORM_LOADOUT_SIZE = OOZARU_LOADOUT.length;
	@Getter
	private boolean formLoadoutActive;
	private final String[] formLoadoutBackup = new String[FORM_LOADOUT_SIZE];
	private final Set<String> formLoadoutGranted = new HashSet<>();

	public Techniques() {
		for (int i = 0; i < SLOT_COUNT; i++) equippedSlots[i] = "";
		java.util.Arrays.fill(formLoadoutBackup, "");
	}

	public boolean applyFormLoadout(String[] loadout) {
		boolean changed = false;
		if (!formLoadoutActive) {
			formLoadoutActive = true;
			formLoadoutGranted.clear();
			System.arraycopy(equippedSlots, 0, formLoadoutBackup, 0, FORM_LOADOUT_SIZE);
			changed = true;
		}
		for (int i = 0; i < Math.min(loadout.length, FORM_LOADOUT_SIZE); i++) {
			String id = loadout[i];
			if (!unlockedTechniques.containsKey(id)) {
				TechniqueData copy = PredefinedTechniques.copyOf(id);
				if (copy == null) continue;
				unlockedTechniques.put(id, copy);
				formLoadoutGranted.add(id);
				changed = true;
			}
			if (!id.equals(equippedSlots[i])) {
				for (int j = 0; j < SLOT_COUNT; j++) {
					if (j != i && id.equals(equippedSlots[j])) equippedSlots[j] = "";
				}
				equippedSlots[i] = id;
				changed = true;
			}
		}
		return changed;
	}

	public boolean restoreFormLoadout() {
		if (!formLoadoutActive) return false;
		formLoadoutActive = false;
		for (String id : formLoadoutGranted) {
			unlockedTechniques.remove(id);
			if (chargingTechniqueId.equals(id)) clearTechniqueCharge();
		}
		for (int i = FORM_LOADOUT_SIZE; i < SLOT_COUNT; i++) {
			if (formLoadoutGranted.contains(equippedSlots[i])) equippedSlots[i] = "";
		}
		for (int i = 0; i < FORM_LOADOUT_SIZE; i++) {
			String previous = formLoadoutBackup[i];
			equippedSlots[i] = previous != null && unlockedTechniques.containsKey(previous) ? previous : "";
			formLoadoutBackup[i] = "";
		}
		formLoadoutGranted.clear();
		return true;
	}

	public boolean isFormLoadoutGranted(String id) { return formLoadoutGranted.contains(id); }

	public void unlockTechnique(TechniqueData data) {
		if (!unlockedTechniques.containsKey(data.getId()) && unlockedTechniques.size() >= MAX_UNLOCKED_TECHNIQUES) return;
		unlockedTechniques.put(data.getId(), data);
	}

	public void equipTechnique(int slotIndex, String techniqueId) {
		if (slotIndex >= 0 && slotIndex < SLOT_COUNT && unlockedTechniques.containsKey(techniqueId)) {
			equippedSlots[slotIndex] = techniqueId;
		}
	}

	public void removeTechnique(String techniqueId) {
		unlockedTechniques.remove(techniqueId);
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (equippedSlots[i].equals(techniqueId)) {
				equippedSlots[i] = "";
				if (selectedSlot == i) selectedSlot = 0;
			}
		}
		if (chargingTechniqueId.equals(techniqueId)) clearTechniqueCharge();
	}

	public void selectSlot(int slotIndex) {
		if (slotIndex >= 0 && slotIndex < SLOT_COUNT) {
			this.selectedSlot = slotIndex;
		}
	}

	public TechniqueData getSelectedTechnique() {
		String id = equippedSlots[selectedSlot];
		return id.isEmpty() ? null : unlockedTechniques.get(id);
	}

	public String[] getEquippedSlots() {
		return this.equippedSlots;
	}

	public String getChargingTechniqueId() {
		return chargingTechniqueId;
	}

	public float getTechniqueChargePercent() {
		return techniqueChargePercent;
	}

	public boolean isTechniqueCharging() {
		return techniqueCharging;
	}

	public boolean isTechniqueChargeActive() {
		return !chargingTechniqueId.isEmpty() && techniqueChargePercent > 0.0f;
	}

	public void startTechniqueCharge(String techniqueId) {
		if (techniqueId == null || techniqueId.isEmpty()) return;
		if (!techniqueId.equals(this.chargingTechniqueId)) {
			this.chargingTechniqueId = techniqueId;
			this.techniqueChargePercent = 0.0f;
		}
		this.techniqueCharging = true;
		this.chargeHolding = true;
	}

	public void setTechniqueChargePercent(float percent) {
		this.techniqueChargePercent = Math.max(0.0f, Math.min(200.0f, percent));
	}

	public boolean isChargeHolding() {
		return chargeHolding;
	}

	public void clearTechniqueCharge() {
		this.chargingTechniqueId = "";
		this.techniqueChargePercent = 0.0f;
		this.techniqueCharging = false;
		this.chargeHolding = false;
		this.homingTargetId = -1;
	}

	public void clearAllTechniques() {
		formLoadoutActive = false;
		formLoadoutGranted.clear();
		java.util.Arrays.fill(formLoadoutBackup, "");
		fractionalXpRemainder.clear();
		this.unlockedTechniques.clear();
		for (int i = 0; i < this.equippedSlots.length; i++) {
			this.equippedSlots[i] = "";
		}
		this.selectedSlot = 0;
		clearTechniqueCharge();
	}

	public int getSelectedSlot() {
		return this.selectedSlot;
	}

	public void equipOrSwapTechnique(int slotIndex, String techniqueId) {
		if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return;

		boolean isEmpty = techniqueId == null || techniqueId.isEmpty();
		if (!isEmpty && !unlockedTechniques.containsKey(techniqueId)) return;

		int existingSlot = -1;
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (equippedSlots[i].equals(techniqueId)) {
				existingSlot = i;
				break;
			}
		}

		if (existingSlot != -1) {
			String temp = equippedSlots[slotIndex];
			equippedSlots[slotIndex] = techniqueId;
			equippedSlots[existingSlot] = temp;
		} else equippedSlots[slotIndex] = techniqueId;
	}

	public void addExperienceToSelected(int amount) {
		TechniqueData active = getSelectedTechnique();
		if (active != null) active.addExperience(racialXp(amount));
	}

	public void addExperienceToTechnique(String id, int amount) {
		if (unlockedTechniques.containsKey(id)) {
			unlockedTechniques.get(id).addExperience(racialXp(amount));
		}
	}

	public void addFractionalExperienceToTechnique(String id, float amount) {
		if (amount <= 0.0f || !unlockedTechniques.containsKey(id)) return;
		float total = fractionalXpRemainder.merge(id, racialXp(amount), Float::sum);
		int whole = (int) total;
		if (whole > 0) {
			unlockedTechniques.get(id).addExperience(whole);
			fractionalXpRemainder.put(id, total - whole);
		}
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("SelectedSlot", selectedSlot);
		tag.putString("ChargingTechniqueId", chargingTechniqueId);
		tag.putFloat("TechniqueChargePercent", techniqueChargePercent);
		tag.putBoolean("TechniqueCharging", techniqueCharging);
		tag.putBoolean("ChargeHolding", chargeHolding);

		CompoundTag slotsTag = new CompoundTag();
		for (int i = 0; i < SLOT_COUNT; i++) {
			slotsTag.putString("Slot" + i, equippedSlots[i]);
		}
		tag.put("EquippedSlots", slotsTag);

		ListTag unlockedTag = new ListTag();
		for (TechniqueData tech : unlockedTechniques.values()) {
			CompoundTag techTag = tech.save();
			techTag.putString("TechClassType", tech instanceof KiAttackData ? "KI"
					: tech instanceof EvasionAttackData ? "EVASION" : "STRIKE");
			unlockedTag.add(techTag);
		}
		tag.put("UnlockedTechniques", unlockedTag);
		tag.putBoolean("FormLoadoutActive", formLoadoutActive);
		CompoundTag backupTag = new CompoundTag();
		for (int i = 0; i < FORM_LOADOUT_SIZE; i++) backupTag.putString("Slot" + i, formLoadoutBackup[i]);
		tag.put("FormLoadoutBackup", backupTag);
		ListTag grantedTag = new ListTag();
		for (String id : formLoadoutGranted) grantedTag.add(StringTag.valueOf(id));
		tag.put("FormLoadoutGranted", grantedTag);

		return tag;
	}

	public void load(CompoundTag tag) {
		this.selectedSlot = Math.clamp(tag.getIntOr("SelectedSlot", 0), 0, SLOT_COUNT - 1);
		this.chargingTechniqueId = tag.getStringOr("ChargingTechniqueId", "");
		this.techniqueChargePercent = Math.max(0.0f, Math.min(200.0f, tag.getFloatOr("TechniqueChargePercent", 0.0F)));
		this.techniqueCharging = tag.getBooleanOr("TechniqueCharging", false);
		this.chargeHolding = tag.getBooleanOr("ChargeHolding", false);

		CompoundTag slotsTag = tag.getCompoundOrEmpty("EquippedSlots");
		for (int i = 0; i < SLOT_COUNT; i++) {
			this.equippedSlots[i] = slotsTag.getStringOr("Slot" + i, "");
		}

		this.unlockedTechniques.clear();
		ListTag unlockedTag = tag.getListOrEmpty("UnlockedTechniques");
		for (int i = 0; i < unlockedTag.size(); i++) {
			CompoundTag techTag = unlockedTag.getCompoundOrEmpty(i);
			String type = techTag.getStringOr("TechClassType", "");
            String savedId = techTag.getStringOr("Id", "");
            if (!"EVASION".equals(type) && PredefinedTechniques.EVASION_REGISTRY.containsKey(savedId)) {
                EvasionAttackData converted = (EvasionAttackData) PredefinedTechniques.copyOf(savedId);
                converted.setExperience(Math.max(0, techTag.getIntOr("Experience", 0)));
                converted.setCooldownLevel(Math.max(0, techTag.getIntOr("CooldownLevel", 0)));
                this.unlockedTechniques.put(savedId, converted);
                continue;
            }
			TechniqueData tech = type.equals("KI") ? new KiAttackData()
					: type.equals("EVASION") ? new EvasionAttackData() : new StrikeAttackData();
			tech.load(techTag);
			this.unlockedTechniques.put(tech.getId(), tech);
		}
		formLoadoutActive = tag.getBooleanOr("FormLoadoutActive", false);
		CompoundTag backupTag = tag.getCompoundOrEmpty("FormLoadoutBackup");
		for (int i = 0; i < FORM_LOADOUT_SIZE; i++) formLoadoutBackup[i] = backupTag.getStringOr("Slot" + i, "");
		formLoadoutGranted.clear();
		ListTag grantedTag = tag.getListOrEmpty("FormLoadoutGranted");
		for (int i = 0; i < grantedTag.size(); i++) {
			String id = grantedTag.getStringOr(i, "");
			if (!id.isEmpty()) formLoadoutGranted.add(id);
		}
		fractionalXpRemainder.clear();
	}

	public void copyFrom(Techniques other) {
		this.selectedSlot = other.selectedSlot;
		this.chargingTechniqueId = other.chargingTechniqueId;
		this.techniqueChargePercent = other.techniqueChargePercent;
		this.techniqueCharging = other.techniqueCharging;
		this.chargeHolding = other.chargeHolding;
		System.arraycopy(other.equippedSlots, 0, this.equippedSlots, 0, SLOT_COUNT);
		this.unlockedTechniques.clear();
		for (Map.Entry<String, TechniqueData> entry : other.unlockedTechniques.entrySet()) {
			TechniqueData clone = entry.getValue() instanceof KiAttackData ? new KiAttackData()
					: entry.getValue() instanceof EvasionAttackData ? new EvasionAttackData() : new StrikeAttackData();
			clone.load(entry.getValue().save());
			this.unlockedTechniques.put(entry.getKey(), clone);
		}
		formLoadoutActive = other.formLoadoutActive;
		System.arraycopy(other.formLoadoutBackup, 0, formLoadoutBackup, 0, FORM_LOADOUT_SIZE);
		formLoadoutGranted.clear();
		formLoadoutGranted.addAll(other.formLoadoutGranted);
	}
}

