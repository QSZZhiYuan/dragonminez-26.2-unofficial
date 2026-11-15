package com.dragonminez.mixin.common;

import com.dragonminez.common.util.IHealthFixable;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carries the pre-respawn max health across to the new {@link ServerPlayer} instance.
 *
 * <h2>26.2 migration</h2>
 * 1.21 injected on the {@code ServerPlayer#setHealth(float)} call inside
 * {@code respawn(ServerPlayer, boolean)} using {@code LocalCapture} to get hold of the freshly
 * created player. Two things broke in 26.2:
 *
 * <ul>
 *   <li>the signature gained a third parameter —
 *       {@code respawn(ServerPlayer, boolean, Entity.RemovalReason)};</li>
 *   <li>the method body was rewritten (NeoForge respawn-position events, restructured level
 *       transition), so the captured local-variable order the injection relied on no longer holds.</li>
 * </ul>
 *
 * <p>{@code LocalCapture} is therefore dropped: the injection sits at {@code TAIL} and reads the
 * method's own return value instead. {@code setHealth} is still the last thing the method does, so
 * the timing relative to it is equivalent, and this no longer depends on the method's internals at
 * all.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

	@Inject(method = "respawn", at = @At("RETURN"), require = 1)
	private void dragonminez$onPlayerRespawn(ServerPlayer oldPlayer, boolean keepAllPlayerData,
											 net.minecraft.world.entity.Entity.RemovalReason removalReason,
											 CallbackInfoReturnable<ServerPlayer> callback) {
		ServerPlayer newPlayer = callback.getReturnValue();
		if (newPlayer == null) return;

		if (newPlayer instanceof IHealthFixable fixable) {
			fixable.dragonminez$setHealthRestorePoint(oldPlayer.getMaxHealth());
		}

		// Some server implementations recreate player attachments before the clone event has
		// finished copying them. Learned skills are persistent character data, so make sure the
		// returned respawn player did not end up with an empty skill set while the old player had
		// learned skills. Keep this narrowly scoped: normal respawn-side changes to other stats
		// (health, resources, racial effects) remain untouched.
		StatsProvider.get(StatsCapability.INSTANCE, oldPlayer).ifPresent(oldData -> {
			if (oldData.getSkills().getAllSkills().isEmpty()) return;
			StatsProvider.get(StatsCapability.INSTANCE, newPlayer).ifPresent(newData -> {
				if (newData.getSkills().getAllSkills().isEmpty()) {
					newData.getSkills().copyFrom(oldData.getSkills());
				}
			});
		});
	}
}
