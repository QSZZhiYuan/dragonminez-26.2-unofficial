package com.dragonminez.common.init.entities.dragon;

import com.dragonminez.client.init.entities.DragonWishClientHooks;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animation.*;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animatable.manager.AnimatableManager;

public class DragonWishEntity extends Mob implements GeoEntity {
	private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> GRANTED_WISH = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> DRAGON_DEFINITION_ID = SynchedEntityData.defineId(DragonWishEntity.class, EntityDataSerializers.STRING);

	private long invokingTime;
	private int despawnDelay = 20 * 5;
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private final String defaultDragonDefinitionId;

	public DragonWishEntity(EntityType<? extends Mob> entityType, Level level, String dragonDefinitionId) {
		super(entityType, level);
		this.defaultDragonDefinitionId = dragonDefinitionId;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1000.0D)
				.add(Attributes.MOVEMENT_SPEED, 2.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(OWNER_NAME, "");
		builder.define(GRANTED_WISH, false);
		builder.define(DRAGON_DEFINITION_ID, defaultDragonDefinitionId == null ? "" : defaultDragonDefinitionId);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 35.0f));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && hasGrantedWish() && --despawnDelay <= 0) this.discard();
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide()) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DragonWishClientHooks.openWishMenu(this, player));
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide() && reason == RemovalReason.DISCARDED) {
			onDespawn();
		}
		super.remove(reason);
	}

	private void onDespawn() {
		DragonDefinition definition = getDragonDefinition();
		if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
			// 26.2: ServerLevel#setWeatherParameters was replaced by the WeatherData setters.
			net.minecraft.world.level.saveddata.WeatherData weather = serverLevel.getWeatherData();
			weather.setClearWeatherTime(6000);
			weather.setRaining(false);
			weather.setThundering(false);
			// 26.2: ServerLevel#setDayTime is gone; world clocks are not settable through the public API.
			if (definition != null && ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls()) {
				DragonBallsHandler.scatterDragonBalls(serverLevel, definition.getBallSetId());
				ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayerByName(this.getOwnerName());
				if (owner != null) {
					DragonBallsHandler.syncRadar(owner.level());
				}
			}
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>("controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationTest<T> animationState) {
		animationState.controller().setAnimation(RawAnimation.begin().then("idle", LoopType.LOOP));
		return PlayState.CONTINUE;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, amount);
		}
		return false;
	}

	@Override
	public boolean canBeCollidedWith(net.minecraft.world.entity.Entity entity) { return false; }
	@Override
	public boolean canCollideWith(Entity entity) { return false; }
	@Override
	public boolean canBeHitByProjectile() { return false; }
	@Override
	public void push(Entity entity) { }
	@Override
	public boolean isPushable() { return false; }
	@Override
	protected void doPush(Entity entity) { }
	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	public void setOwnerName(String name) { this.entityData.set(OWNER_NAME, name); }
	public String getOwnerName() { return this.entityData.get(OWNER_NAME); }
	public void setGrantedWish(boolean granted) { this.entityData.set(GRANTED_WISH, granted); }
	public boolean hasGrantedWish() { return this.entityData.get(GRANTED_WISH); }
	public void setInvokingTime(long time) { this.invokingTime = time; }
	public long getInvokingTime() { return this.invokingTime; }
	public void setDragonDefinitionId(String id) { this.entityData.set(DRAGON_DEFINITION_ID, id); }
	public String getDragonDefinitionId() { return this.entityData.get(DRAGON_DEFINITION_ID); }

	public DragonDefinition getDragonDefinition() {
		String definitionId = getDragonDefinitionId();
		if (definitionId == null || definitionId.isBlank()) {
			var key = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
			definitionId = key == null ? defaultDragonDefinitionId : key.getPath();
		}
		return DragonBallDefinitions.getDragon(definitionId);
	}

	@Override
	public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput compound) {
		super.addAdditionalSaveData(compound);
		compound.putLong("InvokingTime", this.invokingTime);
		compound.putInt("DespawnDelay", this.despawnDelay);
		compound.putString("OwnerName", this.getOwnerName());
		compound.putBoolean("GrantedWish", this.hasGrantedWish());
		compound.putString("DragonDefinitionId", this.getDragonDefinitionId());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput compound) {
		super.readAdditionalSaveData(compound);
		this.invokingTime = compound.getLongOr("InvokingTime", 0L);
		this.despawnDelay = compound.getIntOr("DespawnDelay", 20 * 5);
		this.setOwnerName(compound.getStringOr("OwnerName", ""));
		this.setGrantedWish(compound.getBooleanOr("GrantedWish", false));
		this.setDragonDefinitionId(compound.getStringOr("DragonDefinitionId", ""));
	}
}

