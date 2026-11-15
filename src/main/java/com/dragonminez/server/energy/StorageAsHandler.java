package com.dragonminez.server.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Presents a legacy {@link IEnergyStorage} as the 26.2 {@link EnergyHandler} capability type.
 *
 * <h2>Why this exists</h2>
 * NeoForge 1.21.9 renamed the energy capability: {@code Capabilities.EnergyStorage} became
 * {@code Capabilities.Energy}, and the value type changed from {@code IEnergyStorage} to
 * {@link EnergyHandler}. {@code IEnergyStorage} itself is still present but
 * {@code @Deprecated(forRemoval = true)}, and NeoForge supplies only the reverse bridge
 * ({@link IEnergyStorage#of(EnergyHandler)}).
 *
 * <p>DragonMineZ's own storage ({@link StarEnergyStorage}, built on the deprecated
 * {@code EnergyStorage} reference class) is used as {@code IEnergyStorage} in many places, so rather
 * than reshuffle that whole subsystem during the compile migration, this adapter exposes it through
 * the new capability. It is the only place that knows about both types; when the mod moves to
 * {@code SimpleEnergyHandler} this class disappears.
 *
 * <p>Transaction handling: the legacy interface has no notion of a transaction and mutates
 * immediately, which is exactly what the pre-26.2 capability did. {@code insert}/{@code extract}
 * therefore apply the change straight away and ignore {@code tx}; a rollback will not undo it. That
 * matches the previous behaviour rather than pretending to be transactional.
 */
public final class StorageAsHandler implements EnergyHandler {

    private final IEnergyStorage delegate;

    public StorageAsHandler(IEnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getAmountAsLong() {
        return delegate.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        if (amount <= 0 || !delegate.canReceive()) return 0;
        int accepted = delegate.receiveEnergy(amount, true);
        if (accepted <= 0) return 0;
        return delegate.receiveEnergy(accepted, false);
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        if (amount <= 0 || !delegate.canExtract()) return 0;
        int extracted = delegate.extractEnergy(amount, true);
        if (extracted <= 0) return 0;
        return delegate.extractEnergy(extracted, false);
    }
}
