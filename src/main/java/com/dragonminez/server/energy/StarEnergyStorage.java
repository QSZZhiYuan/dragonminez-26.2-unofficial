package com.dragonminez.server.energy;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

public class StarEnergyStorage extends EnergyStorage {
	public StarEnergyStorage(int capacity, int maxTransfer) {
		super(capacity, maxTransfer);
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		int extracted = super.extractEnergy(maxExtract, simulate);
		if (extracted != 0) {
			onEnergyChanged();
		}
		return extracted;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int received = super.receiveEnergy(maxReceive, simulate);
		if (received != 0) {
			onEnergyChanged();
		}
		return received;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
		onEnergyChanged();
	}

	public void onEnergyChanged() {
	}

	/**
	 * 26.2: block-entity persistence goes through {@code ValueOutput}/{@code ValueInput} instead of
	 * raw {@code CompoundTag}. {@code putInt}/{@code getIntOr} kept their names and signatures, so the
	 * stored key ("energy") and its format are unchanged - existing worlds load as before.
	 */
	public void saveNBT(net.minecraft.world.level.storage.ValueOutput tag) {
		tag.putInt("energy", energy);
	}

	public void loadNBT(net.minecraft.world.level.storage.ValueInput tag) {
		energy = tag.getIntOr("energy", 0);
	}
}
