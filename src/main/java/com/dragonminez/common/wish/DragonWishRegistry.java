package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallPackManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.util.adapters.GenericItemTypeAdapter;
import com.dragonminez.common.util.adapters.WishTypeAdapter;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class DragonWishRegistry extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
	public static final String ROOT_DIRECTORY = "dragonminez/dragonballs";
	public static final DragonWishRegistry INSTANCE = new DragonWishRegistry();
	@Getter
	private static Map<String, List<Wish>> serverWishes = Map.of();
	private static Map<String, List<Wish>> clientWishes = Map.of();

	/**
	 * The Gson instance is kept for parsing individual files.
	 *
	 * <h2>26.2 migration</h2>
	 * {@code SimpleJsonResourceReloadListener} became codec-based: its constructor now takes a
	 * {@code Codec<T>} and derives the prepared map type from it. {@link Wish} is decoded by a
	 * {@link WishTypeAdapter}, so no codec exists for it; this listener therefore extends
	 * {@link SimplePreparableReloadListener} directly and keeps the Gson path.
	 */
	private static final com.google.gson.Gson GSON = new GsonBuilder()
			.registerTypeAdapter(Wish.class, new WishTypeAdapter())
			.registerTypeAdapter(GenericItemDTO.class, new GenericItemTypeAdapter())
			.setPrettyPrinting()
			.create();

	private DragonWishRegistry() {
	}

	@Override
	protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
		// The payload is resolved by DragonBallPackManager, so nothing has to be read here; the
		// signature only exists to satisfy SimplePreparableReloadListener.
		return Map.of();
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
		DragonBallPackManager.LoadedDefinitions external = DragonBallPackManager.loadAll();
		Map<String, List<Wish>> loaded = new LinkedHashMap<>(external.wishes);
		for (var dragon : DragonBallDefinitions.getDragons()) loaded.putIfAbsent(dragon.getId(), List.of());
		serverWishes = Map.copyOf(loaded);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) from the dragonballs system", serverWishes.size());
	}

	public static void setServerWishes(Map<String, List<Wish>> wishes) { serverWishes = Map.copyOf(wishes); }
	@OnlyIn(Dist.CLIENT) public static Map<String, List<Wish>> getClientWishes() { return clientWishes; }
	@OnlyIn(Dist.CLIENT) public static void setClientWishes(Map<String, List<Wish>> wishes) { clientWishes = Map.copyOf(wishes); }

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() != null) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(getServerWishes()), event.getPlayer());
			return;
		}
		for (ServerPlayer player : event.getPlayerList().getPlayers()) {
			NetworkHandler.sendToPlayer(new SyncWishesS2C(getServerWishes()), player);
		}
	}
}

