package com.dragonminez.mixin.client;

import com.dragonminez.client.clash.BeamClashCinematicCamera;
import com.dragonminez.client.flight.FlightRollHandler;
import com.dragonminez.client.flight.RollCamera;
import com.dragonminez.client.render.camera.OverShoulderCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements RollCamera {

	@Shadow protected abstract void setPosition(Vec3 pos);

	// 26.2: Camera#getPosition() was renamed to position().
	@Shadow
	public abstract Vec3 position();

	@Shadow protected abstract void setRotation(float yRot, float xRot);

	@Shadow protected abstract void move(float forwards, float up, float right);
	@Shadow public abstract Entity entity();
	@Shadow public abstract boolean isDetached();

	@Unique private float dragonminez$roll = 0F;
	@Unique private float dragonminez$lastRoll = 0F;
	@Unique private float dragonminez$tickDelta = 0F;

	// 26.2: Camera#setup(BlockGetter, Entity, boolean, boolean, float) was split into
	// tick() + update(DeltaTracker); the DMZ hooks ride on update.
	@Inject(method = "update", at = @At("HEAD"))
	private void dragonminez$captureTickDelta(DeltaTracker deltaTracker, CallbackInfo ci) {
		this.dragonminez$tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
		this.dragonminez$lastRoll = this.dragonminez$roll;
	}

	@Inject(method = "update", at = @At("TAIL"))
	private void dragonminez$modifyCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
		final float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		final BlockGetter level = Minecraft.getInstance().level;
		final Entity entity = this.entity();
		final boolean detached = this.isDetached();
		if (FlightRollHandler.hasActiveRoll()) {
			float newRoll = FlightRollHandler.getRoll(partialTick);
			float delta = Mth.wrapDegrees(newRoll - this.dragonminez$lastRoll);
			this.dragonminez$roll = this.dragonminez$lastRoll + delta;
			dragonminez$rebaseRoll();
		} else this.dragonminez$roll = Mth.lerp(0.1F, this.dragonminez$roll, 0F);

		if (entity instanceof LocalPlayer clashPlayer && BeamClashCinematicCamera.isActive()) {
			BeamClashCinematicCamera.Shot shot = BeamClashCinematicCamera.computeShot(level, clashPlayer, partialTick);
			if (shot != null) {
				this.setPosition(shot.pos());
				this.setRotation(shot.yaw(), shot.pitch());
				return;
			}
		}

	}

	/**
	 * Replaces the camera's pull-back move with DMZ's over-the-shoulder offset.
	 *
	 * <p>The redirect handler mirrors the target signature: the first parameter is the receiver of the
	 * redirected call, and the remaining ones are {@code move}'s own arguments. 26.2 calls
	 * {@code Camera#move(float,float,float)} from {@code alignWithEntity(float)} twice, so ordinal 0 is
	 * pinned to the first (the third-person pull-back).
	 */
	@Redirect(
			method = "alignWithEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V", ordinal = 0)
	)
	private void dragonminez$shoulderSurf(Camera camera, float forwards, float up, float right) {
		Minecraft mc = Minecraft.getInstance();
		float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		BlockGetter level = mc.level;
		Entity entity = this.entity();
		boolean thirdPersonReverse = mc.options.getCameraType().isMirrored();

		// The shoulder helper and its collision rays use left; 26.2 Camera.move uses right.
		Vec3 move = OverShoulderCamera.computeMove(camera, level, entity, thirdPersonReverse, forwards, up, -right, partialTick);
		this.move((float) move.x, (float) move.y, (float) -move.z);
	}

	@Override
	public float dragonminez$getRoll() {
		return Mth.lerp(dragonminez$tickDelta, dragonminez$lastRoll, dragonminez$roll);
	}

	@Unique
	private void dragonminez$rebaseRoll() {
		float wrapped = Mth.wrapDegrees(this.dragonminez$roll);
		float offset = this.dragonminez$roll - wrapped;
		if (offset != 0F) {
			this.dragonminez$roll = wrapped;
			this.dragonminez$lastRoll -= offset;
		}
	}
}

