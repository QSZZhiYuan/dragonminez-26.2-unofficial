package com.dragonminez.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Field accessors on {@link LivingEntity}.
 *
 * <h2>26.2 migration</h2>
 * {@code lastHurtByPlayerTime} was renamed to {@code lastHurtByPlayerMemoryTime} (the field that
 * tracks how long the "hurt by player" credit is remembered). The old name no longer exists, so the
 * accessor failed at apply time with "No candidates were found matching lastHurtByPlayerTime:I".
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

	@Accessor("lastHurtByPlayerMemoryTime")
	int getLastHurtByPlayerMemoryTime();

	@Accessor("lastHurtByPlayerMemoryTime")
	void setLastHurtByPlayerMemoryTime(int lastHurtByPlayerMemoryTime);

	@Accessor("attackStrengthTicker")
	int getAttackStrengthTicker();

	/** Sets partial cooldown progress after vanilla resets an interrupted block break. */
	@Accessor("attackStrengthTicker")
	void setAttackStrengthTicker(int attackStrengthTicker);
}
