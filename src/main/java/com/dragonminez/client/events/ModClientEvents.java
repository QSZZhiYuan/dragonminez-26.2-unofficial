package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.crowdin.CrowdinManager;
import com.dragonminez.client.crowdin.CrowdinPackResources;
import com.dragonminez.client.dragonball.DragonBallPackResources;
import com.dragonminez.client.animation.CombatAnimationResolver;
import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.hud.*;
import com.dragonminez.client.gui.tooltip.*;
import com.dragonminez.client.init.blocks.renderer.DragonBallBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.EnergyCableBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.FuelGeneratorBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.GravityDeviceBlockRenderer;
import com.dragonminez.client.init.blocks.renderer.KikonoStationBlockRenderer;
import com.dragonminez.client.init.entities.model.ki.*;
import com.dragonminez.client.init.entities.renderer.*;
import com.dragonminez.client.init.entities.renderer.ki.*;
import com.dragonminez.client.init.entities.renderer.rr.RedRibbonRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RedRibbonSoldierRenderer;
import com.dragonminez.client.init.entities.renderer.rr.RobotRRRenderer;
import com.dragonminez.client.init.entities.renderer.sagas.*;
import com.dragonminez.client.util.ArmorTextureResolver;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.SkinCacheManager;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.util.BetaWhitelist;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.armor.client.model.ArmorBaseModel;
import com.dragonminez.client.init.menu.screens.FuelGeneratorScreen;
import com.dragonminez.client.init.menu.screens.GravityDeviceScreen;
import com.dragonminez.client.init.menu.screens.KikonoStationScreen;
import com.dragonminez.common.init.particles.*;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.level.GrassColor;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {
	@SubscribeEvent
	public static void registerCharacterPreviews(RegisterPictureInPictureRenderersEvent event) {
		event.register(com.dragonminez.client.render.EntityPreviewRenderState.class,
				com.dragonminez.client.render.EntityPreviewRenderer::new);
	}

	@SubscribeEvent
	public static void registerGuiOverlays(RegisterGuiLayersEvent e) {
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "xenoversehud"), XenoverseHUD.HUD_XENOVERSE);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "alternativehud"), AlternativeHUD.HUD_ALTERNATIVE);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "modernhud"), ModernHUD.HUD_MODERN);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "minecrafthud"), MinecraftHUD.HUD_MINECRAFT);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "technique_charge_hud"), TechniqueChargeOverlay.HUD_TECHNIQUE_CHARGE);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "scouterhud"), ScouterHUD.HUD_SCOUTER);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "notifications"), NotificationHUD.HUD_NOTIFICATIONS);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "techniquehud"), TechniqueHotbarHUD.HUD_TECHNIQUES);
		e.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "beam_clash_hud"), BeamClashOverlay.HUD_BEAM_CLASH);
		e.registerAbove(VanillaGuiLayers.CROSSHAIR, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "lock_on_hud"), LockOnEvent.HUD_LOCK_ON);
	}
	@SubscribeEvent
	public static void onRegisterClientReloadListeners(AddClientReloadListenersEvent event) {
		// 26.2: the event exposes addListener(Identifier, PreparableReloadListener) instead.
		event.addListener(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "dmz_client_cache_clear"),
				new SimplePreparableReloadListener<Void>() {
			@Override
			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				return null;
			}

			@Override
			protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
				com.dragonminez.client.render.shader.FullscreenPostEffect.onResourceReload();
                com.dragonminez.client.render.effects.Aura3DRenderer.reset();
                com.dragonminez.client.render.effects.AuraBorderRenderer.reset();
                com.dragonminez.client.render.shader.EffectBloomRenderer.reset();
                com.dragonminez.client.render.shader.TransformationOutlineRenderer.reset();
                com.dragonminez.client.render.shader.KiBloomRenderer.reset();
				TextureCounter.clearCache();
                com.dragonminez.client.init.entities.model.MasterGlobalModel.clearCache();
                com.dragonminez.client.init.entities.model.QuestNPCModel.clearCache();
                com.dragonminez.client.init.entities.model.sagas.DBSagaModel.clearCache();
				ArmorTextureResolver.clearCache();
                com.dragonminez.client.util.SkinGathererProvider.clearShadedLayers();
				CombatAnimationResolver.reload(resourceManager);
                com.dragonminez.client.animation.EvasionAnimations.reload(resourceManager);
				SkinCacheManager.revalidate();
			}
		});
	}

	@SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBinds.registerAll(event);
    }

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
			if (CrowdinManager.isLiveTranslationsEnabled()) {
				String currentLang = Minecraft.getInstance().options.languageCode;
				CrowdinManager.fetchLanguage(currentLang);
			}

			event.addRepositorySource((packConsumer) -> {
				PackLocationInfo crowdinInfo = new PackLocationInfo("dmz_crowdin_ota", Component.literal("DMZ Live Translations"), PackSource.BUILT_IN, java.util.Optional.empty());
				Pack.ResourcesSupplier crowdinSupplier = new Pack.ResourcesSupplier() {
					@Override public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo location) { return new CrowdinPackResources(location); }
					@Override public net.minecraft.server.packs.PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) { return openPrimary(location); }
				};
				Pack crowdinPack = Pack.readMetaAndCreate(crowdinInfo, crowdinSupplier, PackType.CLIENT_RESOURCES,
						new PackSelectionConfig(true, Pack.Position.TOP, false));
				if (crowdinPack != null) packConsumer.accept(crowdinPack);

				PackLocationInfo dragonballInfo = new PackLocationInfo("dmz_dragonballs_runtime", Component.literal("DMZ Dragonballs Runtime Resources"), PackSource.BUILT_IN, java.util.Optional.empty());
				Pack.ResourcesSupplier dragonballSupplier = new Pack.ResourcesSupplier() {
					@Override public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo location) { return new DragonBallPackResources(location); }
					@Override public net.minecraft.server.packs.PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) { return openPrimary(location); }
				};
				Pack dragonBallRuntimePack = Pack.readMetaAndCreate(dragonballInfo, dragonballSupplier, PackType.CLIENT_RESOURCES,
						new PackSelectionConfig(true, Pack.Position.TOP, false));
				if (dragonBallRuntimePack != null) packConsumer.accept(dragonBallRuntimePack);
			});
		}
	}

	@SubscribeEvent
	public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MainMenus.KIKONO_STATION_MENU.get(), KikonoStationScreen::new);
		event.register(MainMenus.FUEL_GENERATOR_MENU.get(), FuelGeneratorScreen::new);
		event.register(MainMenus.GRAVITY_DEVICE_MENU.get(), GravityDeviceScreen::new);
	}


	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			setCustomWindowIcon();
			NotificationHUD.pin("tracked_quest", TrackedQuestHUD::notification, NotificationHUD.PRIORITY_TRACKED_QUEST);

			BetaWhitelist.reload();
			SkinCacheManager.init();

			//Bloques
			BlockEntityRenderers.register(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), DragonBallBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.ENERGY_CABLE_BE.get(), EnergyCableBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.KIKONO_STATION_BE.get(), KikonoStationBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.FUEL_GENERATOR_BE.get(), FuelGeneratorBlockRenderer::new);
			BlockEntityRenderers.register(MainBlockEntities.GRAVITY_DEVICE_BE.get(), GravityDeviceBlockRenderer::new);

        });

        UtilityMenuScreen.initMenuSlots();
		// 26.2: RenderTarget#enableStencil is gone - the stencil buffer is requested at target creation
	}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        //MAESTROS
        for (var masterEntity : MainEntities.getMasterEntities()) {
            event.registerEntityRenderer((EntityType) masterEntity.get(), context -> new MasterEntityRenderer(context));
        }

        // Quest NPC — single renderer for all data-driven quest NPCs | usa un renderer genérico para los NPCs de misiones, después usa gráficos.json para asignar modelos/texturas específicos a cada npcId
        event.registerEntityRenderer(MainEntities.QUEST_NPC.get(), QuestNPCRenderer::new);

        for (var sagaEntity : MainEntities.getSagaEntities()) {
            event.registerEntityRenderer((EntityType) sagaEntity.get(), context -> new DBSagasRenderer(context));
        }

        regRender(event, SagaSaibamanRenderer::new,
                MainEntities.SAGA_SAIBAMAN, MainEntities.SAGA_SAIBAMAN2, MainEntities.SAGA_SAIBAMAN3,
                MainEntities.SAGA_SAIBAMAN4, MainEntities.SAGA_SAIBAMAN5, MainEntities.SAGA_SAIBAMAN6);

        event.registerEntityRenderer(MainEntities.DINOSAUR1.get(), DinosRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR2.get(), GranDinoRenderer::new);
        event.registerEntityRenderer(MainEntities.DINOSAUR3.get(), DinoFlyRenderer::new);
        event.registerEntityRenderer(MainEntities.DINO_KID.get(), DinosRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_FROG.get(), NamekFrogRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_FROG_GINYU.get(), NamekFrogRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_TRADER.get(), NamekianRenderer::new);
        event.registerEntityRenderer(MainEntities.CC_NAMEKIAN.get(), NamekianRenderer::new);
        event.registerEntityRenderer(MainEntities.NAMEK_WARRIOR.get(), NamekianWarriorRenderer::new);
        event.registerEntityRenderer(MainEntities.SABERTOOTH.get(), DinosRenderer::new);

        event.registerEntityRenderer(MainEntities.BANDIT.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT1.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT2.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_ROBOT3.get(), RobotRRRenderer::new);
        event.registerEntityRenderer(MainEntities.RED_RIBBON_SOLDIER.get(), RedRibbonSoldierRenderer::new);
        event.registerEntityRenderer(MainEntities.SPACE_POD.get(), SpacePodRenderer::new);
        event.registerEntityRenderer(MainEntities.FLYING_NIMBUS.get(), FlyingNimbusRenderer::new);
        event.registerEntityRenderer(MainEntities.BLACK_NIMBUS.get(), BlackNimbusRenderer::new);
        event.registerEntityRenderer(MainEntities.ROBOT_XENOVERSE.get(), RedRibbonRenderer::new);
        event.registerEntityRenderer(MainEntities.PUNCH_MACHINE.get(), PunchMachineRenderer::new);

		for (var entity : MainEntities.getDragonWishEntities().values()) {
			event.registerEntityRenderer(entity.get(), DragonDBRenderer::new);
		}

        event.registerEntityRenderer(MainEntities.KI_BLAST.get(), KiProjectileRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_EXPLOSION.get(), KiExplosionRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_BLUE_HURRICANE.get(), SPBlueHurricaneRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_DRAGON_FIST.get(), SPDragonFistRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_OZARU_FIST.get(), SPOzaruFistRenderer::new);
        event.registerEntityRenderer(MainEntities.SP_MAJIN_CANDY.get(), SPMajinCandyRenderer::new);
        event.registerEntityRenderer(MainEntities.GUM_BIND.get(), GumBindRenderer::new);
        event.registerEntityRenderer(MainEntities.GUM_BALL.get(), GumBindRenderer.BallRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_LASER.get(), KiLaserRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_WAVE.get(), KiWaveRenderer::new);
        event.registerEntityRenderer(MainEntities.MAJIN_SKILL.get(), MajinSkillRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_DISC.get(), KiDiskRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_BARRIER.get(), KiBarrierRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_EXPLOSION_VISUAL.get(), KiExplosionVisualRenderer::new);
        event.registerEntityRenderer(MainEntities.KI_AREA.get(), KiProjectileRenderer::new);

    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ArmorBaseModel.LAYER_LOCATION, ArmorBaseModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(MainParticles.KI_FLASH.get(), KiFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SPLASH_WAVE.get(), KiSplashWaveParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_TRAIL.get(), KiTrailParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_SHEDDING.get(), KiSheddingParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_LIGHTNING.get(), KiLightningParticle.Provider::new);

        event.registerSpriteSet(MainParticles.KI_EXPLOSION_FLASH.get(), KiExplosionFlashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION_SPLASH.get(), KiExplosionSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KI_EXPLOSION.get(), KiExplosionParticle.Provider::new);
        event.registerSpriteSet(MainParticles.KINTON.get(), KintonParticle.Provider::new);
        event.registerSpriteSet(MainParticles.PUNCH_PARTICLE.get(), PunchParticle.Provider::new);
        event.registerSpriteSet(MainParticles.BLOCK_PARTICLE.get(), BlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.GUARD_BLOCK.get(), GuardBlockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.SPARKS.get(), KiSplashParticle.Provider::new);
        event.registerSpriteSet(MainParticles.AURA.get(), AuraParticle.Provider::new);
        event.registerSpriteSet(MainParticles.DUST.get(), DustParticle.Provider::new);
        event.registerSpriteSet(MainParticles.ROCK.get(), RockParticle.Provider::new);
        event.registerSpriteSet(MainParticles.DIVINE.get(), DivineParticle.Provider::new);

    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void regRender(EntityRenderersEvent.RegisterRenderers event, EntityRendererProvider provider, DeferredHolder... entities) {
        for (DeferredHolder reg : entities) {
            event.registerEntityRenderer((EntityType) reg.get(), provider);
        }
    }

	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
		// 26.2: block tints are BlockTintSource lists, not positional lambdas.
		// grass() = biome colour in world, default grass colour in hand.
		event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grass()),
				MainBlocks.SACRED_PLANET_GRASS_BLOCK.get());
	}

	// 26.2 note: RegisterColorHandlersEvent.ItemTintSources registers MapCodec<ItemTintSource>
	// entries into a registry that already contains the vanilla codecs. Re-registering
	// GrassColorSource.MAP_CODEC - the same instance vanilla installed - throws
	//   IllegalArgumentException: value already present: RecordCodec[UnitDecoder[GrassColorSource...]]
	// and aborts client startup. Vanilla's grass tint is already available to DMZ items by
	// resource path, so nothing needs to be registered here.

	private static void setCustomWindowIcon() {
		Minecraft mc = Minecraft.getInstance();

		if ((net.minecraft.util.Util.getPlatform() == net.minecraft.util.Util.OS.OSX)) {
			try {
				Identifier macLoc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "icons/minecraft.icns");
				var res = mc.getResourceManager().getResource(macLoc);
				if (res.isPresent()) MacosUtil.loadIcon(res.get()::open);
			} catch (Exception ignored) {}
			return;
		}

		long windowId = mc.getWindow().handle();
		String[] iconNames = {"icon_16x16.png", "icon_32x32.png", "icon_48x48.png", "icon_128x128.png", "icon_256x256.png"};
		List<NativeImage> loadedImages = new ArrayList<>();

		for (String name : iconNames) {
			try {
				Identifier loc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "icons/" + name);
				var resource = mc.getResourceManager().getResource(loc);
				if (resource.isPresent()) {
					try (InputStream is = resource.get().open()) {
						loadedImages.add(NativeImage.read(is));
					}
				}
			} catch (Exception ignored) {}
		}

		if (loadedImages.isEmpty()) return;
		List<ByteBuffer> buffersToFree = new ArrayList<>();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			GLFWImage.Buffer glfwImages = GLFWImage.malloc(loadedImages.size(), stack);

			for (int i = 0; i < loadedImages.size(); i++) {
				NativeImage image = loadedImages.get(i);
				ByteBuffer byteBuffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
				buffersToFree.add(byteBuffer);
				byteBuffer.asIntBuffer().put(image.getPixelsABGR());
				glfwImages.position(i);
				glfwImages.width(image.getWidth());
				glfwImages.height(image.getHeight());
				glfwImages.pixels(byteBuffer);
			}
			GLFW.glfwSetWindowIcon(windowId, glfwImages.position(0));
		} finally {
			buffersToFree.forEach(MemoryUtil::memFree);
			loadedImages.forEach(NativeImage::close);
		}
	}

	@SubscribeEvent
	public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(CustomTooltipNodes.HeaderNode.class, CustomTooltipRenderers.HeaderRenderer::new);
		event.register(CustomTooltipNodes.PaddingNode.class, CustomTooltipRenderers.PaddingRenderer::new);
		event.register(CustomTooltipNodes.SeparatorNode.class, CustomTooltipRenderers.SeparatorRenderer::new);
	}
}

