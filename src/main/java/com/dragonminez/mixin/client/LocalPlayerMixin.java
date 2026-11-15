package com.dragonminez.mixin.client;

import com.dragonminez.common.combat.util.MathHelper;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

	@Inject(method = "hurtTo", at = @At("HEAD"), cancellable = true)
	private void dragonminez$preventKaiokenHurtAnimation(float pHealth, CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, self).orElse(null);
		if (data == null) return;

		if (data.getCharacter().hasActiveForm() || data.getCharacter().hasActiveStackForm()) {
			float currentHealth = self.getHealth();
			float healthLoss = currentHealth - pHealth;
			if (healthLoss <= 0) return;

			double expectedDrain = Math.round(data.getAdjustedHealthDrain());

			if (healthLoss <= (expectedDrain + 2.0f)) {
				self.setHealth(pHealth);
				ci.cancel();
			}
		}
	}

	/**
	 * Slow the player down while a DMZ attack swing is playing.
	 *
	 * <h2>26.2 restoration</h2>
	 * 1.21 scaled {@code Input#forwardImpulse}/{@code leftImpulse}, both public floats. 26.2 replaced
	 * those with a {@code ClientInput} object whose movement vector is a derived {@link Vec2}:
	 * {@code ClientInput#moveVector} is {@code protected} and only a read-only
	 * {@code getMoveVector()} is public.
	 *
	 * <p>{@link ClientInputAccessor} writes the field directly, which is the supported route for a
	 * {@code protected} field. The hook runs at the tail of {@code aiStep}, after vanilla has filled
	 * the vector, so the scaled value is what the movement code consumes for this tick.
	 */
	@Inject(method = "aiStep", at = @At("TAIL"))
	private void dragonminez$aiStep_ModifyInput(CallbackInfo ci) {
		double baseMultiplier = 0.5D;
		boolean applySmoothly = true;
		boolean affectWhileMounting = false;

		double multiplier = Math.min(Math.max(baseMultiplier, 0.0), 1.0);
		if (multiplier == 1.0) return;

		LocalPlayer clientPlayer = (LocalPlayer) (Object) this;
		if (clientPlayer.getVehicle() != null && !affectWhileMounting) return;

		var client = (Minecraft_DMZ) Minecraft.getInstance();
		float swingProgress = client.getSwingProgress();
		if (swingProgress >= 0.98F) return;

		if (applySmoothly) {
			double p2 = swingProgress <= 0.5F
					? MathHelper.easeOutCubic(swingProgress * 2)
					: MathHelper.easeOutCubic(1 - ((swingProgress - 0.5F) * 2));
			multiplier = 1.0 - (1.0 - multiplier) * p2;
		}

		if (clientPlayer.isSprinting()) clientPlayer.setSprinting(false);

		ClientInput input = clientPlayer.input;
		if (input == null) return;

		Vec2 move = ((ClientInputAccessor) input).dragonminez$getMoveVector();
		if (move == null) return;

		((ClientInputAccessor) input).dragonminez$setMoveVector(
				new Vec2((float) (move.x * multiplier), (float) (move.y * multiplier)));
	}
}
