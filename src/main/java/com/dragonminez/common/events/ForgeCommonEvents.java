package com.dragonminez.common.events;

import net.minecraft.core.component.DataComponents;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.alignment.NpcAlignmentRules;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.util.Player_DMZ;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.dragonminez.common.compat.WorldGuardCompat;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.init.CapsuleCorpMapTrade;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEnchants;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainVillagers;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.armor.DbzArmorTextured;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.ki.KiBarrierEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.TrainingSessionTracker;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.SyncWeaponRegistryS2C;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.dragonball.DragonDefinitionReloadListener;
import com.dragonminez.common.wish.DragonWishRegistry;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.DMZServer;
import com.dragonminez.server.commands.DMZPermissions;
import com.dragonminez.server.commands.LocateCommand;
import com.dragonminez.server.events.DragonBallsHandler;
import com.dragonminez.server.storage.StorageManager;
import com.dragonminez.server.util.FusionLogic;
import com.dragonminez.server.world.data.DragonBallSavedData;
import com.dragonminez.server.world.dimension.*;
import com.dragonminez.server.world.npc.NPCPlacementManager;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.brigadier.ParseResults;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.*;

import static com.dragonminez.common.diagnostics.JsonLoadReport.logConsoleReport;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class ForgeCommonEvents {

	/**
	 * 胶囊公司地图商人的报价表。
	 *
	 * <h2>26.2 迁移（方案 B）</h2>
	 * 1.21 用 {@code VillagerTradesEvent} 往交易表里塞 {@code ItemListing}。
	 * 26.2 删除了 {@code ItemListing} 接口，村民交易改为纯数据驱动的
	 * {@code VILLAGER_TRADE} 注册表 + {@code TradeSet}；数据驱动条目**无法做世界查询**
	 * （{@code StructureLocator.locateStructure}），也**无法定期刷新已卖出地图的目标点**。
	 *
	 * <p>因此改为在玩家与商人交互时（{@link #onMerchantInteract}）动态构建报价，
	 * 直接写入 {@link net.minecraft.world.item.trading.MerchantOffers}。动态定位与过期刷新都保留。
	 *
	 * <p>分级（原 trades.get(1..5)）在 26.2 没有直接的「等级表」概念 —— 报价全部给出，
	 * 具体解锁等级改由 Villager 的 profession 等级机制之外的方式表达。
	 * TODO(26.2 trading)：如需按等级解锁，可在 {@link #onMerchantInteract} 里按
	 * {@code merchant.getVillagerData().getLevel()} 过滤。
	 */
	private static void addCapsuleCorpMapOffers(ServerLevel level, AbstractVillager merchant, List<MerchantOffer> out) {
		addMapOfferSafely(level, merchant, out, DMZStructures.GOKU_HOUSE,      "dragonminez.goku_house",      new ItemStack(MainItems.DINO_MEAT_COOKED.get(), 10));
		addMapOfferSafely(level, merchant, out, DMZStructures.ROSHI_HOUSE,     "dragonminez.roshi_house",     new ItemStack(Items.COD, 8));
		addMapOfferSafely(level, merchant, out, DMZStructures.PICCOLO_HOUSE,   "dragonminez.piccolo_house",   new ItemStack(Items.WATER_BUCKET, 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.YAMCHA_HOUSE,    "dragonminez.yamcha_house",    new ItemStack(Items.BONE, 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.KAMILOOKOUT,     "dragonminez.kamilookout",     new ItemStack(Items.SPRUCE_SAPLING, 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.CELL_ARENA,      "dragonminez.cell_arena",      new ItemStack(MainItems.T1_RADAR_CHIP.get(), 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.GERO_LAB,        "dragonminez.gero_lab",        new ItemStack(Items.IRON_INGOT, 16));
		addMapOfferSafely(level, merchant, out, DMZStructures.TRUNKS_SHIP,     "dragonminez.trunks_ship",     new ItemStack(Items.IRON_SWORD, 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.VEGETA_POD,      "dragonminez.vegeta_pod",      new ItemStack(MainItems.RED_SCOUTER.get(), 1));
		addMapOfferSafely(level, merchant, out, DMZStructures.BABIDI,          "dragonminez.babidi",          new ItemStack(Items.ENDER_PEARL, 4));
	}

	private static void addMapOfferSafely(ServerLevel level, AbstractVillager merchant, List<MerchantOffer> out,
										  ResourceKey<Structure> destination, String displayName, ItemStack cost) {
		MerchantOffer offer = CapsuleCorpMapTrade.createMapOffer(
				level, merchant, destination, displayName, cost, MapDecorationTypes.RED_X);
		if (offer != null) out.add(offer);
	}

	/**
	 * 玩家与商人交互时，为胶囊公司商人注入地图报价并刷新过期地图。
	 *
	 * <p>结构可能因重定位而与卖出时生成的地图不符，报价里持久化的地图会指向旧点，
	 * 因此在打开界面之前重算一次。整个过程包在 try 里：交易注入失败不应该阻断交互本身。
	 */
	@SubscribeEvent
	public static void onMerchantInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
		if (!(event.getTarget() instanceof AbstractVillager merchant)) return;

		try {
			boolean isCapsuleCorp = merchant instanceof Villager villager
					&& villager.getVillagerData().profession().is(MainVillagers.CAPSULE_CORP_ASSISTANT.getKey());
			if (isCapsuleCorp) {
				MerchantOffers offers = merchant.getOffers();
				if (offers.isEmpty()) {
					List<MerchantOffer> generated = new java.util.ArrayList<>();
					addCapsuleCorpMapOffers(serverLevel, merchant, generated);
					offers.addAll(generated);
				}
			}
			CapsuleCorpMapTrade.injectMapOffers(serverLevel, merchant);
		} catch (Exception ignored) {
			// 交易注入失败不应阻断与商人的交互
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		String version = Reference.VERSION;
		if (version.contains("-beta") || version.contains("-alpha")) {
			Player player = event.getEntity();
			String username = player.getGameProfile().name();

			if (!BetaWhitelist.isAllowed(username)) {
				LogUtil.error(Env.SERVER, "User {} tried to join but is not in the beta whitelist.", username);
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.connection.disconnect(Component.literal("""
							§c[DragonMine Z]
							
							§7You are not allowed to play this Beta/Alpha version.
							§fAre you a §cPatreon§f? Use the §cVerify here§f button to connect Discord and request access automatically.
							
							§7If you have been recently whitelisted, restart Minecraft to apply the changes!
							§7Your Minecraft nickname is: §f""" + username));
				} else {
					throw new IllegalStateException("DMZ: User not allowed.");
				}
			}
		}

		if (event.getEntity() instanceof ServerPlayer player) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(WishManager.getAllWishes()), player);
			DragonBallsHandler.syncRadar(player.level());

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout()) {
					if (data.getCooldowns().hasCooldown(Cooldowns.COMBAT)) player.kill((net.minecraft.server.level.ServerLevel) player.level());
				}
				endFusionIfNeeded(player);
			});

			Map<String, WeaponAttributes> stringMap = new java.util.HashMap<>();
			WeaponRegistry.registrations.forEach((k, v) -> stringMap.put(k.toString(), v));

			String jsonRegistry = new com.google.gson.Gson().toJson(stringMap);
			NetworkHandler.sendToPlayer(new SyncWeaponRegistryS2C(jsonRegistry), player);
			SpacePodDestinationRegistry.syncToPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			TrainingSessionTracker.end(player.getUUID());
			PacketRateLimiter.clear(player.getUUID());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (ConfigManager.getCombatConfig().getKillPlayersOnCombatLogout()) {
					if (data.getCooldowns().hasCooldown(Cooldowns.COMBAT)) player.kill((net.minecraft.server.level.ServerLevel) player.level());
				}
				endFusionIfNeeded(player);
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DragonBallsHandler.syncRadar(player.level());
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				endFusionIfNeeded(player);

				if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
					if (data.getStatus().isAlive())
						data.getCooldowns().addCooldown(Cooldowns.REVIVE_BABA, ConfigManager.getServerConfig().getGameplay().getReviveCooldownSeconds() * 20);
					if (data.getStatus().isHasCreatedCharacter()) data.getStatus().setAlive(false);
					if (!data.getStatus().isInKaioPlanet()) data.getStatus().setInKaioPlanet(true);
				}

				if (data.getSkills().hasSkill("kaioken") && data.getSkills().isSkillActive("kaioken"))
					data.getSkills().setSkillActive("kaioken", false);
				data.getCooldowns().removeCooldown(Cooldowns.COMBAT);
				data.getCharacter().clearActiveForm(player);
				data.getCharacter().clearActiveStackForm(player);
				data.getEffects().removeAllEffects();
				data.getSecondaryStatEffects().clear();
				player.refreshDimensions();
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			player.refreshDimensions();
		}
	}

	@SubscribeEvent
	public static void onGamemodeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getNewGameMode() != GameType.SPECTATOR) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> endFusionIfNeeded(player));
		}
	}

	@SubscribeEvent
	public static void onBlockBreak(BreakBlockEvent event) {
		if (event.getPlayer() instanceof ServerPlayer player) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (data.getStatus().isBlocking()) event.setCanceled(true);
			});
		}
	}

	public static double getCriticalChance(Player player) {
		double chance = player.getAttributeValue(MainAttributes.CRIT_CHANCE);
		int chanceLevel = MainEnchants.level(player.getMainHandItem(), player.level(), MainEnchants.CRIT_CHANCE);
		if (chanceLevel > 0) chance += (chanceLevel * 0.05D);
		DMZEvent.CritChanceEvent event = new DMZEvent.CritChanceEvent(player, chance);
		NeoForge.EVENT_BUS.post(event);
		return event.getChance();
	}

	public static double getCriticalDamage(Player player) {
		double multiplier = player.getAttributeValue(MainAttributes.CRIT_DAMAGE);
		int damageLevel = MainEnchants.level(player.getMainHandItem(), player.level(), MainEnchants.CRIT_DAMAGE);
		if (damageLevel > 0) multiplier += (damageLevel * 0.05D);
		return multiplier;
	}

	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		Entity target = event.getTarget();
		Player attacker = event.getEntity();
		Level level = attacker.level();

		if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
			TargetHelper.Relation relation = TargetHelper.getRelation(attacker, target);
			if (!TargetHelper.canAttack(attacker, target, attacker.distanceTo(target) + 1.0D)) {
				event.setCanceled(true);
				return;
			}
			TargetHelper.onSuccessfulAttack(attacker, target, relation);

			double chance = getCriticalChance(attacker);
			boolean isCrit = ((Player_DMZ) attacker).rollAndGetCriticalStatus(chance);

			boolean isValidTarget = false;
			if (target instanceof ServerPlayer targetPlayer) {
				isValidTarget = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer)
						.map(targetData -> !targetData.getStatus().isBlocking() && !targetPlayer.isCreative())
						.orElse(false);
			} else if (!(target instanceof MastersEntity) && !target.isInvulnerable() && !(target instanceof PunchMachineEntity)) {
				isValidTarget = true;
			}

			if (isValidTarget) {
				double x = target.getX();
				double y = target.getY() + (target.getBbHeight() * 0.65);
				double z = target.getZ();
				float[] rgb = ColorUtils.rgbIntToFloat(0xFFFFFF);

				if (!isCrit) serverLevel.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, rgb[0], rgb[1], rgb[2], 1.0);
			}

			if (isCrit) {
				DeferredHolder<SoundEvent, ? extends SoundEvent>[] sonidosCritico = new DeferredHolder[]{
						MainSounds.CRITICO1, MainSounds.CRITICO2
				};
				int indiceRandom = level.getRandom().nextInt(sonidosCritico.length);
				SoundEvent sonidoCritico = sonidosCritico[indiceRandom].get();
				attacker.level().playSound(null, attacker.blockPosition(), sonidoCritico, SoundSource.PLAYERS, 1.0F, 1.0F);
			} else if (attacker.getMainHandItem() == ItemStack.EMPTY || attacker.getMainHandItem().is(Items.AIR)) {
				DeferredHolder<SoundEvent, ? extends SoundEvent>[] sonidosGolpe = new DeferredHolder[]{
						MainSounds.GOLPE1,
						MainSounds.GOLPE2,
						MainSounds.GOLPE3,
						MainSounds.GOLPE4,
						MainSounds.GOLPE5,
						MainSounds.GOLPE6
				};

				int indiceRandom = level.getRandom().nextInt(sonidosGolpe.length);
				SoundEvent sonidoElegido = sonidosGolpe[indiceRandom].get();
				attacker.level().playSound(null, attacker.blockPosition(), sonidoElegido, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
		}
	}

	@SubscribeEvent
	public static void onMeleeCriticalHit(CriticalHitEvent event) {
		Player player = event.getEntity();
		if (player == null || player.level().isClientSide()) return;

		double chance = getCriticalChance(player);
		boolean isCrit = ((Player_DMZ) player).rollAndGetCriticalStatus(chance);

		if (isCrit) {
			event.setDamageMultiplier((float) getCriticalDamage(player));
			event.setCriticalHit(true);
		} else {
			event.setCriticalHit(false);
		}
	}

	@SubscribeEvent
	public static void onRangedCriticalHit(LivingDamageEvent.Pre event) {
		if (event.getSource().getDirectEntity() instanceof AbstractArrow && event.getSource().getEntity() instanceof Player player) {
			if (player.level().isClientSide()) return;

			double chance = getCriticalChance(player);
			boolean isCrit = ((Player_DMZ) player).rollAndGetCriticalStatus(chance);

			if (isCrit) event.setNewDamage((float) (event.getNewDamage() * getCriticalDamage(player)));
		}
	}

	@SubscribeEvent
	public static void suppressVanillaCritSounds(PlayLevelSoundEvent.AtPosition event) {
		if (event.getSound().value() == SoundEvents.PLAYER_ATTACK_CRIT) event.setCanceled(true);
	}

	private static final UUID WEAPON_CRIT_CHANCE_UUID = UUID.fromString("c37e2055-7783-4559-b18a-1b3e6f5a0410");
	private static final UUID WEAPON_CRIT_DAMAGE_UUID = UUID.fromString("496f2714-a61f-41e2-aed5-265e7fa0e0fa");

	@SubscribeEvent
	public static void attachDynamicWeaponAttributes(ItemAttributeModifierEvent event) {
		ItemStack stack = event.getItemStack();
		WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
		if (attributes == null) return;

		double weaponCritChance = attributes.getSafeCritChance();
		double weaponCritDamage = attributes.getSafeCritDamage();

		if (weaponCritChance > 0.0D) {
			event.addModifier(
					MainAttributes.CRIT_CHANCE,
					com.dragonminez.common.util.AttributeMods.of(WEAPON_CRIT_CHANCE_UUID, "Weapon innate crit chance", weaponCritChance, AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
		}

		if (weaponCritDamage > 0.0D) {
			event.addModifier(
					MainAttributes.CRIT_DAMAGE,
					com.dragonminez.common.util.AttributeMods.of(WEAPON_CRIT_DAMAGE_UUID, "Weapon innate crit damage", weaponCritDamage, AttributeModifier.Operation.ADD_VALUE),
					net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
		}
	}

	private static String fusionDisplayName(Player player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> data.getStatus().isFused() ? data.getStatus().getFusionName() : "")
				.filter(name -> name != null && !name.isEmpty())
				.orElse(null);
	}

	@SubscribeEvent
	public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
		String fusionName = fusionDisplayName(event.getEntity());
		if (fusionName != null) event.setDisplayname(Component.literal(fusionName));
	}

	@SubscribeEvent
	public static void onPlayerTabListNameFormat(PlayerEvent.TabListNameFormat event) {
		String fusionName = fusionDisplayName(event.getEntity());
		if (fusionName != null) event.setDisplayName(Component.literal(fusionName));
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) DragonBallsHandler.syncRadarForPlayer(player);
	}

	@SubscribeEvent
	public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) {
		if (ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()) {
			OtherworldRegionLoader.loadPreGeneratedRegions(event.getServer());
		}

		com.dragonminez.server.world.structure.helper.VillagePoolInjector.injectAll(event.getServer());
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		StorageManager.init();
		BetaWhitelist.reload();
		WishManager.loadWishes(event.getServer());
		DMZPermissions.init();
		QuestRegistry.loadAll(event.getServer());
		NpcAlignmentRules.load(event.getServer());
		NPCPlacementManager.load(event.getServer());

		WorldGuardCompat.init();

		if (ConfigManager.getServerConfig().getWorldGen().getGenerateDragonBalls()) {
			for (var definition : DragonBallDefinitions.getBallSets()) {
				ServerLevel targetLevel = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, definition.getValidDimensions().iterator().next()));
				if (targetLevel == null) continue;
				DragonBallSavedData data = DragonBallSavedData.get(targetLevel);
				if (!data.isFirstSpawnComplete(definition.getId())) {
					DragonBallsHandler.scatterDragonBalls(targetLevel, definition.getId());
					LogUtil.info(Env.COMMON, "First DragonBalls Spawn setup for set: " + definition.getId());
				}
			}
		} else {
			LogUtil.info(Env.COMMON, "DragonBalls generation is disabled in the config.");
		}
		logConsoleReport();
	}

	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		com.dragonminez.server.world.structure.placement.StructureSpawnPlanner.precomputeAndWait(event.getServer());

		ServerLevel otherworld = event.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);

		if (otherworld != null) {
			LogUtil.info(Env.SERVER, "ServerStartedEvent: Attempting to load Otherworld regions, double-checking dimension presence.");
			OtherworldRegionLoader.loadPreGeneratedRegions(event.getServer());
		}
		NPCPlacementManager.spawnForLoadedLevels(event.getServer());
	}

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
		try {
			com.dragonminez.server.world.structure.placement.StructureRepairManager.tick(serverLevel);
		} catch (Throwable ignored) {
		}
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel serverLevel) {
			try {
				com.dragonminez.server.world.structure.placement.StructureSpawnPlanner.onLevelLoad(serverLevel);
			} catch (Throwable ignored) {
			}

			if (serverLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
				LogUtil.info(Env.SERVER, "LevelEvent.Load: Detected Otherworld dimension load, attempting to load regions.");
				OtherworldRegionLoader.loadPreGeneratedRegions(serverLevel.getServer());
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		StorageManager.shutdown();
		com.dragonminez.server.world.structure.placement.StructureSpawnPlanner.reset();
		com.dragonminez.server.world.structure.placement.StructureRepairManager.reset();
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		DMZServer.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onLocateCommand(CommandEvent event) {
		try {
			ParseResults<CommandSourceStack> parse = event.getParseResults();
			var nodes = parse.getContext().getNodes();
			if (nodes.size() < 3) return;
			if (!nodes.get(0).getNode().getName().equals("locate")) return;
			if (!nodes.get(1).getNode().getName().equals("structure")) return;

			String argument = nodes.get(2).getRange().get(parse.getReader().getString());
			if (argument.isEmpty() || argument.charAt(0) == '#') return;

			Identifier id = Identifier.tryParse(argument);
			if (id == null || !id.getNamespace().equals(Reference.MOD_ID)) return;

			CommandSourceStack source = parse.getContext().getSource();
			ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);

			if (!StructureLocator.usesCustomPlacement(source.getLevel(), key)) return;

			event.setCanceled(true);
			LocateCommand.locate(source, key);
		} catch (Exception ignored) {}
	}

	@SubscribeEvent
	public static void onMobSpawn(MobSpawnEvent.PositionCheck event) {
		Mob mob = event.getEntity();
		if (mob.getType().getCategory() != MobCategory.MONSTER) return;
		if (mob.level().dimension().equals(HTCDimension.HTC_KEY)) return;

		List<MastersEntity> masters = mob.level().getEntitiesOfClass(MastersEntity.class, new AABB(mob.blockPosition()).inflate(80));
		if (masters.isEmpty()) return;

		event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);

	}

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		if (event.getSlot() != EquipmentSlot.CHEST) {
			return;
		}

		ItemStack newStack = event.getTo();

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {

			boolean shouldBeArmored = false;

			if (!newStack.isEmpty() && newStack.has(DataComponents.EQUIPPABLE)) {
				boolean isVanilla = BuiltInRegistries.ITEM.getKey(newStack.getItem()).getNamespace().equals("minecraft");
				boolean isDbzArmor = newStack.getItem() instanceof DbzArmorTextured;

				if (!isVanilla && !isDbzArmor) {
					shouldBeArmored = true;
				}
			}
			if (stats.getCharacter().getArmored() != shouldBeArmored) {
				stats.getCharacter().setArmored(shouldBeArmored);
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			}
		});
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingIncomingDamageEvent event) {
		LivingEntity victim = event.getEntity();
		if (isCharacterCreationProtected(victim)) {
			event.setCanceled(true);
			return;
		}
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
		if (!(victim instanceof Player) && !(victim instanceof DBSagasEntity)) return;

		AABB searchArea = victim.getBoundingBox().inflate(3.0D);
		List<KiBarrierEntity> barriers = victim.level().getEntitiesOfClass(KiBarrierEntity.class, searchArea);

		for (KiBarrierEntity barrier : barriers) {
			if (barrier.isActive() && barrier.protects(victim)) {
				event.setCanceled(true);
				barrier.absorbDamage(event.getAmount(), event.getSource().getEntity());
				return;
			}
		}
	}

	@SubscribeEvent
	public static void onLivingDamage(LivingDamageEvent.Post event) {
		// Post is not cancellable in 1.21; protection handled in LivingIncomingDamageEvent above.
	}

	private static boolean isCharacterCreationProtected(LivingEntity entity) {
		if (!ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) return false;
		if (!(entity instanceof ServerPlayer player)) return false;
		final boolean[] protectedPlayer = {false};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) protectedPlayer[0] = true;
		});
		return protectedPlayer[0];
	}

	public static void endFusionIfNeeded(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getStatus().isFused() || data.getStatus().getFusionPartnerUUID() != null) {
				UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
				if (partnerUUID != null) {
					ServerPlayer partner = player.level().getServer().getPlayerList().getPlayer(partnerUUID);
					if (partner != null) {
						if (!partner.isDeadOrDying()) partner.kill((net.minecraft.server.level.ServerLevel) partner.level());
						StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(partnerData -> FusionLogic.endFusion(partner, partnerData, true));
					}
				}
				FusionLogic.endFusion(player, data, true);
			}
		});
	}

	@SubscribeEvent
	public static void onAddReloadListeners(net.neoforged.neoforge.event.AddServerReloadListenersEvent event) {
		// 26.2: NeoForge replaced AddReloadListenerEvent with AddServerReloadListenersEvent, whose
		// addListener now takes an explicit Identifier key (listeners are wired into a dependency graph).
		event.addListener(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "space_pod_destinations"), SpacePodDestinationRegistry.INSTANCE);
		event.addListener(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "dragon_definitions"), DragonDefinitionReloadListener.INSTANCE);
		event.addListener(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "dragon_wishes"), DragonWishRegistry.INSTANCE);
		event.addListener(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "weapon_attributes"), new SimplePreparableReloadListener<Void>() {
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
				return null;
			}

			@Override
			protected void apply(Void dummy, ResourceManager resourceManager, ProfilerFiller profiler) {
				WeaponRegistry.loadAttributes(resourceManager);
			}
		});
	}

	@SubscribeEvent
	public static void onHandSwap(LivingSwapItemsEvent.Hands event) {
		if (event.getEntity() instanceof Player player) {
			var offHandStack = player.getOffhandItem();
			event.setItemSwappedToOffHand(player.getMainHandItem());
			event.setItemSwappedToMainHand(offHandStack);
		}
	}
}


