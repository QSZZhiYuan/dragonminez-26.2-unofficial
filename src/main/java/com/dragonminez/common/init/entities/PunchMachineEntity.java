package com.dragonminez.common.init.entities;

import net.minecraft.server.level.ServerLevel;

import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animatable.manager.AnimatableManager;

public class PunchMachineEntity extends Mob implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private long lastHitTime = 0;
	private double accumulatedDamage = 0;
	private long combatStartTime = 0;

    public PunchMachineEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;

		if (source.getEntity() instanceof Player player) {
			if (player.isCrouching()) {
				if (!this.isRemoved()) {
					if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) this.spawnAtLocation(sl, MainItems.PUNCH_MACHINE_ITEM.get());
					this.discard();
				}
				return false;
			}
		}

		if (MainDamageTypes.isStrikeAttackDamage(source) || MainDamageTypes.isKiblastDamage(source)) {
			if (source.getEntity() instanceof Player player) processHit(amount, player);
			return false;
		}

		return super.hurtServer(level, source, amount);
    }

	public void processHit(float damage, Player attacker) {
		long currentTime = System.currentTimeMillis();

		if (currentTime - lastHitTime > 5000) {
			accumulatedDamage = 0;
			combatStartTime = currentTime;
		}

		if (accumulatedDamage == 0) {
			combatStartTime = currentTime;
		}

		lastHitTime = currentTime;
		accumulatedDamage += damage;

		double seconds = (currentTime - combatStartTime) / 1000.0;
		if (seconds < 1.0) seconds = 1.0;

		double dps = accumulatedDamage / seconds;

		spawnDamageIndicator(damage);

		String msg = String.format("DMG: %.1f | DPS: %.1f", damage, dps);
		attacker.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	private void spawnDamageIndicator(float damage) {
		AreaEffectCloud indicator = new AreaEffectCloud(this.level(), this.getX(), this.getY() + 0.8, this.getZ());
		indicator.setRadius(0.0F);
		indicator.setDuration(20);
		// 26.2: BlockParticleOption is constructed directly (no create() factory).
		indicator.setCustomParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
		indicator.setWaitTime(0);

		String dmgText = String.format("%.0f", damage);
		indicator.setCustomName(Component.literal(dmgText).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		indicator.setCustomNameVisible(true);

		this.level().addFreshEntity(indicator);
	}

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationTest<T> tAnimationState) {
        tAnimationState.controller().setAnimation(RawAnimation.begin().then("idle", LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity pEntity) {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void push(Entity pEntity) {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity p_20971_) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

