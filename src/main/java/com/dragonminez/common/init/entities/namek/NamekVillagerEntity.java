package com.dragonminez.common.init.entities.namek;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.goals.VillageAlertSystem;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NamekVillagerEntity extends Villager implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    // These shops have fixed custom trades and no vanilla job-site block. Building
    // on Villager's provider would also install ResetProfession and immediately
    // invalidate the trading menu of a novice merchant without a fletching table.
    private static final Brain.Provider<Villager> NAMEK_BRAIN_PROVIDER = Brain.provider(
            List.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE,
                    MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT),
            List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS,
                    SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY,
                    SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES,
                    SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED),
            body -> List.of(
                    ActivityData.create(Activity.CORE, ImmutableList.of(
                            Pair.of(0, new Swim<>(0.8F)),
                            Pair.of(0, InteractWithDoor.create()),
                            Pair.of(0, new LookAtTargetSink(45, 90)),
                            Pair.of(0, new VillagerPanicTrigger()),
                            Pair.of(0, WakeUp.create()),
                            Pair.of(1, new MoveToTargetSink()),
                            Pair.of(3, new LookAndFollowTradingPlayerSink(0.5F)))),
                    ActivityData.create(Activity.IDLE, VillagerGoalPackages.getIdlePackage(0.5F)),
                    ActivityData.create(Activity.PLAY, VillagerGoalPackages.getPlayPackage(0.5F)),
                    ActivityData.create(Activity.REST, VillagerGoalPackages.getRestPackage(0.5F)),
                    ActivityData.create(Activity.PANIC, VillagerGoalPackages.getPanicPackage(0.5F)),
                    ActivityData.create(Activity.PRE_RAID, VillagerGoalPackages.getPreRaidPackage(0.5F)),
                    ActivityData.create(Activity.RAID, VillagerGoalPackages.getRaidPackage(0.5F)),
                    ActivityData.create(Activity.HIDE, VillagerGoalPackages.getHidePackage(0.5F))));

    public NamekVillagerEntity(EntityType<? extends Villager> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
        // 26.2: VillagerData#profession is a Holder and the setters require the registry access,
        // so the profession is resolved through the level's registry now.
        this.setVillagerData(this.getVillagerData()
                .withProfession(pLevel.registryAccess(), VillagerProfession.FLETCHER));
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(20);
		}
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // The brain's LookAndFollowTradingPlayerSink owns trading movement. The legacy
        // goal clears the trading player whenever ground/water/motion state changes,
        // making the merchant menu invalid even though the player is still trading.
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 2.0D));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));

    }

    @Override
    public void setVillagerData(VillagerData data) {
        // Also repairs merchants saved with NONE by the old job-site AI. Preserve
        // their type, level and offers when the fixed profession already matches.
        super.setVillagerData(data.withProfession(this.level().registryAccess(), VillagerProfession.FLETCHER));
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean isHurt = super.hurtServer(level, source, amount);

        if (isHurt && source.getEntity() instanceof Player) {
            Player player = (Player) source.getEntity();
            VillageAlertSystem.alertAll(player); // Alertar a todos los guerreros
        }

        return isHurt;
    }

    private static List<CustomTrade> TRADES;

    /**
     * 26.2: item stacks cannot be built while the class initialiser runs, because the item
     * components are not bound yet ({@code NullPointerException: Components not bound yet}). The
     * trade table is therefore built lazily on first use, which happens long after registration.
     */
    private static synchronized List<CustomTrade> trades() {
        if (TRADES != null) return TRADES;
        List<CustomTrade> list = new ArrayList<>();
        // (new ItemStack(ITEM REQUERIDO, CANTIDAD), new ItemStack(ITEM REQUERIDO 2, CANTIDAD), new ItemStack(ITEM COMPRADO, CANTIDAD), USOS MAXIMOS, XP));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 1), new ItemStack(Items.CARROT, 3), new ItemStack(MainItems.FROG_LEGS_RAW.get(), 2), 10, 6));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.BUCKET, 1), new ItemStack(MainItems.HEALING_BUCKET.get(), 1), 3, 5));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.AIR), new ItemStack(MainBlocks.NAMEK_BLOCK.get(), 8), 64, 3));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 32), new ItemStack(Items.AIR), new ItemStack(MainItems.T2_RADAR_CHIP.get(), 1), 2, 9));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 16), new ItemStack(Items.ANCIENT_DEBRIS, 1), new ItemStack(Items.NETHERITE_SCRAP, 2), 16, 6));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 28), new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.NETHERITE_SCRAP, 2), 4, 7));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.AIR), new ItemStack(Items.CARROT, 3), 10, 3));
        list.add(new CustomTrade(new ItemStack(Items.CARROT, 8), new ItemStack(Items.AIR), new ItemStack(Items.EMERALD, 2), 10, 5));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.AIR), new ItemStack(Items.POTATO, 3), 10, 3));
        list.add(new CustomTrade(new ItemStack(Items.POTATO, 8), new ItemStack(Items.AIR), new ItemStack(Items.EMERALD, 2), 10, 5));
        list.add(new CustomTrade(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.AIR), new ItemStack(Items.BEETROOT, 3), 10, 3));
        list.add(new CustomTrade(new ItemStack(Items.BEETROOT, 8), new ItemStack(Items.AIR), new ItemStack(Items.EMERALD, 2), 10, 5));
        TRADES = List.copyOf(list);
        return TRADES;
    }

    @Override
    public boolean wantsToSpawnGolem(long pGameTime) {
        return false;
    }

    @Override
    protected Brain<Villager> makeBrain(net.minecraft.world.entity.ai.Brain.Packed packedBrain) {
        Brain<Villager> brain = NAMEK_BRAIN_PROVIDER.makeBrain(this, packedBrain);
        this.registerBrainGoals(brain);
        return brain;
    }

    @Override
    public void refreshBrain(ServerLevel serverLevel) {
        Brain<Villager> brain = this.getBrain();
        brain.stopAll(serverLevel, this);
        // 26.2 removed Brain#copyWithoutBehaviors - rebuilding from the packed form is the equivalent.
        this.brain = this.makeBrain(brain.pack());
    }

    protected void registerBrainGoals(Brain<Villager> brain) {
        brain.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        // Los tradeos NO deben cambiar aleatoriamente
        if (this.offers == null) {
            this.offers = new MerchantOffers();
        }

        // Si el aldeano no tiene intercambios, asigna los primeros 3 intercambios
        if (this.offers.isEmpty()) {
            Random random = new Random();
            List<CustomTrade> availableTrades = new ArrayList<>(trades());
            for (int i = 0; i < 3; i++) {
                if (availableTrades.isEmpty()) break;
                CustomTrade randomTrade = availableTrades.remove(random.nextInt(availableTrades.size()));
                this.offers.add(randomTrade.createOffer());
            }
        } else {
            // Si el aldeano ya tiene intercambios (ha subido de nivel), agrega 1 nuevo intercambio no repetido
            Random random = new Random();
            List<CustomTrade> availableTrades = new ArrayList<>(trades());

            for (MerchantOffer offer : this.offers) {
                availableTrades.removeIf(trade -> trade.createOffer().equals(offer));
            }

            if (!availableTrades.isEmpty()) {
                CustomTrade randomTrade = availableTrades.get(random.nextInt(availableTrades.size()));
                this.offers.add(randomTrade.createOffer());
            }
        }
    }

    private static class CustomTrade {
        private final ItemStack input;
        private final ItemStack secInput;
        private final ItemStack output;
        private final int maxUses;
        private final int xp;

        public CustomTrade(ItemStack input, ItemStack secInput, ItemStack output, int maxUses, int xp) {
            this.input = input;
            this.secInput = secInput;
            this.output = output;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        public MerchantOffer createOffer() {
            var secondaryCost = secInput.isEmpty() ? java.util.Optional.<net.minecraft.world.item.trading.ItemCost>empty()
                    : java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(secInput.getItem(), secInput.getCount()));
            return new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(input.getItem(), input.getCount()),
                    secondaryCost, output.copy(), maxUses, xp, 0.15F);
        }
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            // 26.2: AbstractVillager#updateTrades takes the ServerLevel it is being called from.
            // Offers are only ever generated server-side, so the cast is safe here.
            if (this.level() instanceof ServerLevel serverLevel) {
                updateTrades(serverLevel);
            }
        }
        return this.offers;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

}

