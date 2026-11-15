package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.compat.capabilities.Capability;
import com.dragonminez.compat.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import com.dragonminez.compat.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Player stats provider backed by a serializable NeoForge data attachment.
 */
public class StatsProvider implements ICapabilityProvider, ValueIOSerializable {
    public static final Identifier ID = Identifier.parse(Reference.MOD_ID);

    private final StatsData data;
    private final LazyOptional<StatsData> optional;

    public StatsProvider(Player player) {
        this.data = new StatsData(player);
        this.optional = LazyOptional.of(() -> data);
    }

	public static StatsProvider getOrCreate(Player player) {
		return player.getData(StatsCapability.PLAYER_STATS.get());
	}

	public static void remove(Player player) {
		// Attachments are owned by the entity and discarded with it.
	}

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == StatsCapability.INSTANCE) {
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    public static @NotNull <T> LazyOptional<T> get(Capability<T> cap, Entity entity) {
		if (!(entity instanceof Player player)) {
			return LazyOptional.empty();
		}
		if (cap != StatsCapability.INSTANCE) {
			return LazyOptional.empty();
		}
		return getOrCreate(player).getCapability(cap, null);
    }

    void invalidate() {
        this.optional.invalidate();
    }

		/**
		 * 26.2 迁移：NeoForge 删除了 INBTSerializable，改用 ValueIOSerializable。
		 * 它不再直接收发 CompoundTag，而是写进 ValueOutput / 从 ValueInput 读，两者都要 Codec。
		 * StatsData 自身的存档契约（NBT 键名）不变，这里只是把整棵 CompoundTag 作为一个
		 * 子节点 "dmz_stats" 存进去，内部键名完全保持原样。
		 */
		@Override
		public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
			output.store("dmz_stats", net.minecraft.nbt.CompoundTag.CODEC, data.save());
		}

		@Override
		public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
			input.read("dmz_stats", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(tag -> {
				try {
					data.load(tag);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			});
		}

}
