package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class SetFlightSpeedLimitC2S {
	private final int limit;

	public SetFlightSpeedLimitC2S(int limit) {
		this.limit = limit;
	}

	public SetFlightSpeedLimitC2S(FriendlyByteBuf buffer) {
		this.limit = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(limit);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || player.hasEffect(com.dragonminez.common.init.MainEffects.STUN)) return;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter() || data.getSkills().getSkillLevel("fly") <= 0) return;
				int snapped = Math.max(5, Math.min(100, (limit / 5) * 5));
				data.getResources().setFlightSpeedLimit(snapped);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}
