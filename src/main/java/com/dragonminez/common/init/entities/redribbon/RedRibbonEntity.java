package com.dragonminez.common.init.entities.redribbon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

public class RedRibbonEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);
    private boolean isAttacking = false;

    public RedRibbonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.6D, false));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("base_controller", 5, this::walkPredicate));
        controllers.add(new AnimationController<>("attack_controller", 0, this::attackPredicate));

    }

    private <T extends GeoAnimatable> PlayState walkPredicate(AnimationTest<T> event) {
        RedRibbonEntity entity = (RedRibbonEntity) event.animatable();

        if (event.isMoving()) {
            if (entity.isAggressive() || entity.getTarget() != null) {
                event.setAnimation(RawAnimation.begin().thenLoop("run"));
            } else {
                event.setAnimation(RawAnimation.begin().thenLoop("walk"));
            }
            return PlayState.CONTINUE;
        }

        event.setAnimation(RawAnimation.begin().thenLoop("idle"));
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState attackPredicate(AnimationTest<T> event) {
        RedRibbonEntity entity = (RedRibbonEntity) event.animatable();

        if (entity.swingTime > 0 && !isAttacking) {
            isAttacking = true;

            event.controller().reset();

            event.setAnimation(RawAnimation.begin().thenPlay("attack"));

            return PlayState.CONTINUE;
        }

        if (isAttacking) {
            if (event.controller().hasAnimationFinished()) {
                isAttacking = false;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }

        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor pLevel, EntitySpawnReason reason) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL && this.checkSpawnObstruction(pLevel);
    }

    public static boolean canSpawnHere(EntityType<? extends RedRibbonEntity> entity, ServerLevelAccessor world, EntitySpawnReason spawn, BlockPos pos, RandomSource random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;

        if (world.getBrightness(LightLayer.BLOCK, pos) > 7) return false;

        BlockState stateAtPos = world.getBlockState(pos);
        if (!stateAtPos.isAir() && !stateAtPos.canBeReplaced()) return false;

        BlockState ground = world.getBlockState(pos.below());
        if (!ground.isFaceSturdy(world, pos.below(), Direction.UP)) return false;

        return world.noCollision(entity.getDimensions().makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }
}

