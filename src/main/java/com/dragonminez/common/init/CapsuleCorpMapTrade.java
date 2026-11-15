package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 胶囊公司地图商人的「结构地图」报价生成器。
 *
 * <h2>26.2 迁移（方案 B：保留动态结构定位能力）</h2>
 * 1.21 这里 {@code implements VillagerTrades.ItemListing}，由
 * {@code VillagerTradesEvent} 在交易表里注册。
 *
 * <p>26.2 把村民交易改造成**纯数据驱动**：{@code Registries.VILLAGER_TRADE} 注册表 +
 * {@code TradeSet}，{@code VillagerTrades.ItemListing} 接口被**整个删除**。
 * 数据驱动的 {@code VillagerTrade} 只能表达静态报价，**无法在报价时做世界查询**
 * （{@code StructureLocator.locateStructure}），也**没法定期刷新已卖出地图的目标点**。
 *
 * <p>因此采用方案 B：本类不再实现任何交易接口，改为一个**无状态的报价工厂**，
 * 由 {@code ForgeCommonEvents#onMerchantInteract} 在玩家打开交易界面时调用
 * {@link #injectMapOffers(ServerLevel, AbstractVillager)}，
 * 直接往 {@link MerchantOffers} 里追加/刷新地图报价。
 * 这样动态定位与过期刷新两项能力都完整保留。
 */
public final class CapsuleCorpMapTrade {

	/** Custom data key on the map stack with the structure destination id. */
	public static final String STRUCTURE_TAG = "dmz_structure";

	/** 地图报价的固定经验值。 */
	private static final int MAP_XP = 40;
	/** 地图报价的固定最大使用次数。 */
	private static final int MAP_MAX_USES = 8;
	/** 动态注入报价时用于识别「本 mod 的图」的 NBT 标记之外的兜底：翻译键前缀。 */
	private static final String TRANSLATION_PREFIX = "filled_map." + Reference.MOD_ID + ".";

	private CapsuleCorpMapTrade() {}

	/**
	 * 为一个具体目的地构造一条地图报价。
	 *
	 * @param level       服务端世界（需要它来生成地图与定位结构）
	 * @param trader      触发报价的商人（只用它的位置做搜索原点）
	 * @param destination 目标结构的注册键
	 * @param displayName 翻译键后缀，拼成 {@code filled_map.<displayName>}
	 * @param cost        售价
	 * @param destinationType 地图上目标点的图标类型
	 * @return 报价；结构定位失败或在错误线程调用时返回 {@code null}
	 */
	@Nullable
	public static MerchantOffer createMapOffer(ServerLevel level, Entity trader,
											   ResourceKey<Structure> destination, String displayName,
											   ItemStack cost, Holder<MapDecorationType> destinationType) {
		if (level == null || trader == null) return null;
		if (!level.getServer().isSameThread()) return null;

		BlockPos pos = StructureLocator.locateStructure(level, destination, trader.blockPosition());
		if (pos == null) return null;

		ItemStack map = createMapStack(level, pos, destination, displayName, destinationType);
		ItemCost costA = toCost(cost);
		return new MerchantOffer(costA, Optional.empty(), map, MAP_MAX_USES, MAP_XP, 0.2F);
	}

	private static ItemCost toCost(ItemStack stack) {
		return new ItemCost(stack.getItem(), stack.getCount());
	}

	private static ItemStack createMapStack(ServerLevel level, BlockPos pos, ResourceKey<Structure> destination,
											String displayName, Holder<MapDecorationType> destinationType) {
		ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		ItemStack decorated = map;
		MapItemSavedData.addTargetDecoration(decorated, pos, "+", destinationType);
		decorated.set(DataComponents.CUSTOM_NAME, Component.translatable("filled_map." + displayName));
		CustomData.update(DataComponents.CUSTOM_DATA, decorated,
				tag -> tag.putString(STRUCTURE_TAG, destination.identifier().toString()));
		return decorated;
	}

	/**
	 * 把胶囊公司的地图报价注入到商人当前的报价表，并刷新已经过期的地图目标点。
	 *
	 * <p>1.21 由 {@code VillagerTradesEvent} 在建表时塞进去；26.2 改为每次玩家打开界面时调用，
	 * 因此这里必须**幂等**：已存在的地图报价只刷新目标点，不重复追加。
	 */
	public static void injectMapOffers(ServerLevel level, AbstractVillager merchant) {
		MerchantOffers offers = merchant.getOffers();
		refreshStaleMapOffers(level, offers, merchant);
	}

	/**
	 * 刷新已经卖出/生成的、目标点与当前结构规划不符的地图报价。
	 *
	 * <p>结构可能因为 `/locate` 解析或重定位而移动，卖出时生成的地图会指向旧点。
	 * 这里在每次打开界面前重算一遍。
	 */
	private static void refreshStaleMapOffers(ServerLevel level, MerchantOffers offers, Entity trader) {
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer offer = offers.get(i);
			ItemStack result = offer.getResult();
			if (!result.is(Items.FILLED_MAP)) continue;

			ResourceKey<Structure> key = structureKeyFrom(level, result);
			if (key == null) continue;

			BlockPos pos = StructureLocator.locateStructure(level, key, trader.blockPosition());
			if (pos == null || matchesTarget(result, pos)) continue;

			ItemStack fresh = createMapStack(level, pos, key,
					Reference.MOD_ID + "." + key.identifier().getPath(), MapDecorationTypes.RED_X);
			offers.set(i, new MerchantOffer(offer.getItemCostA(), offer.getItemCostB(), fresh,
					offer.getUses(), offer.getMaxUses(), offer.getXp(),
					offer.getPriceMultiplier(), offer.getDemand()));
		}
	}

	@Nullable
	private static ResourceKey<Structure> structureKeyFrom(ServerLevel level, ItemStack map) {
		Identifier id = null;
		CustomData custom = map.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (custom.contains(STRUCTURE_TAG)) {
			id = Identifier.tryParse(custom.copyTag().getStringOr(STRUCTURE_TAG, ""));
		} else if (map.getHoverName().getContents() instanceof TranslatableContents translatable) {
			String translationKey = translatable.getKey();
			if (translationKey.startsWith(TRANSLATION_PREFIX)) {
				id = Identifier.fromNamespaceAndPath(Reference.MOD_ID,
						translationKey.substring(TRANSLATION_PREFIX.length()));
			}
		}
		if (id == null) return null;
		if (!level.registryAccess().lookupOrThrow(Registries.STRUCTURE).containsKey(id)) return null;
		return ResourceKey.create(Registries.STRUCTURE, id);
	}

	private static boolean matchesTarget(ItemStack map, BlockPos pos) {
		MapDecorations decorations = map.get(DataComponents.MAP_DECORATIONS);
		if (decorations == null) return false;
		MapDecorations.Entry entry = decorations.decorations().get("+");
		if (entry == null) return false;
		return (int) entry.x() == pos.getX() && (int) entry.z() == pos.getZ();
	}
}
