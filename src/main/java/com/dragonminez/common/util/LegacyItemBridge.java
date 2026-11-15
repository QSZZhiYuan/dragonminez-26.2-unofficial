package com.dragonminez.common.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a legacy {@link IItemHandler} as the 26.2 {@link ResourceHandler}{@code <ItemResource>}
 * capability type.
 *
 * <h2>Why this exists</h2>
 * NeoForge 1.21.9 replaced the item capability: {@code Capabilities.ItemHandler} became
 * {@code Capabilities.Item}, and its value type changed from {@code IItemHandler} to
 * {@code ResourceHandler<ItemResource>}. {@code IItemHandler} and {@code ItemStackHandler} still
 * exist but are {@code @Deprecated(forRemoval = true)}; NeoForge only ships the reverse bridge
 * ({@code ItemResourceHandlerAdapter}, which wraps a {@code ResourceHandler} back into an
 * {@code IItemHandler}).
 *
 * <p>DragonMineZ's block entities still build their inventories on the deprecated
 * {@code ItemStackHandler}, so this adapter closes the gap in one place. When the mod moves to
 * {@code ItemStacksResourceHandler}, this class disappears.
 *
 * <p>Transactions: the legacy interface has no notion of one and mutates immediately, which is what
 * the pre-26.2 capability did. {@code insert}/{@code extract} therefore apply straight away and
 * ignore {@code tx}; a rollback will not undo them. That matches the previous behaviour rather than
 * pretending to be transactional.
 */
public final class LegacyItemBridge {

    private LegacyItemBridge() {}

    public static ResourceHandler<ItemResource> toResourceHandler(IItemHandler delegate) {
        return delegate == null ? null : new LegacyHandler(delegate);
    }

    private record LegacyHandler(IItemHandler delegate) implements ResourceHandler<ItemResource> {

        @Override
        public int size() {
            return delegate.getSlots();
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(delegate.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return delegate.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return delegate.getSlotLimit(index);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.isEmpty() || delegate.isItemValid(index, resource.toStack(1));
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            ItemStack toInsert = resource.toStack(amount);
            ItemStack remainder = delegate.insertItem(index, toInsert, false);
            return amount - remainder.getCount();
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) return 0;
            ItemStack current = delegate.getStackInSlot(index);
            if (!ItemResource.of(current).equals(resource)) return 0;
            ItemStack extracted = delegate.extractItem(index, amount, false);
            return extracted.getCount();
        }
    }
}
