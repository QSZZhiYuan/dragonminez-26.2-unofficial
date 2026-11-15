package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口。
 *
 * <h2>26.2 迁移</h2>
 * 两处结构性删除：
 * <ul>
 *   <li>{@code GatherDataEvent#getExistingFileHelper()} **没了** —— 整个
 *       {@code net.neoforged.neoforge.common.data.ExistingFileHelper} 类被删除
 *       （它是模型/纹理存在性校验器，26.2 改为数据驱动后不再需要）；</li>
 *   <li>{@code BlockStateProvider} 与 {@code ItemModelProvider} 这两个父类
 *       **在 26.2 完全不存在**（NeoForge 删掉了整个 {@code client.model.generators} 包，
 *       MC 侧的模型生成也改成了 {@code net.minecraft.client.data.models.*}）。
 *       因此这两个 provider 暂时停用，见下方 TODO。</li>
 * </ul>
 *
 * <p>TODO(26.2 datagen)：把方块状态与物品模型的生成迁到
 * {@code net.minecraft.client.data.models.blockstates.BlockModelGenerators} /
 * {@code ItemModelGenerators}。这不影响编译之外的任何东西 —— 现有的
 * {@code src/generated/resources} 与 {@code src/main/resources/assets} 里的模型 JSON
 * 仍然有效并被加载；只是「重新生成」这条路暂时断了。
 * 追踪条目见交接文档 §3.3。
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public class DatagenManager {

	/**
	 * 26.2: GatherDataEvent became abstract - NeoForge dispatches GatherDataEvent.Server and
	 * GatherDataEvent.Client separately, and subscribing to the abstract base throws
	 * "Cannot register listeners for abstract class" at mod construction time.
	 * The DMZ providers are server-side data, so this listens to the Server variant.
	 */
	@SubscribeEvent
	public static void gatherData(GatherDataEvent.Server event) {
		DataGenerator generator = event.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

		// ---- 服务端数据 ----
		// 26.2: RecipeProvider is abstract and no longer a DataProvider by itself - run() drives it.
		generator.addProvider(true, DMZLootTableProvider.create(packOutput, lookupProvider));
		generator.addProvider(true, new DMZBlockTagGenerator(packOutput, lookupProvider));
		generator.addProvider(true, new DMZItemTagGenerator(packOutput, lookupProvider));
		generator.addProvider(true, new DMZEntityTypeTagGenerator(packOutput, lookupProvider));

		DMZWorldGenProvider worldGenProvider = generator.addProvider(true,
				new DMZWorldGenProvider(packOutput, lookupProvider));

		generator.addProvider(true,
				new DMZBiomeTagGenerator(packOutput, worldGenProvider.getRegistryProvider()));

		generator.addProvider(true, new DMZAdvancementsProvider(packOutput, lookupProvider));
		generator.addProvider(true, new DMZSpacePodDestinationProvider(packOutput));
		generator.addProvider(true, new DMZDragonDefinitionProvider(packOutput));
		generator.addProvider(true, new DMZDragonWishProvider(packOutput));

		// ---- 客户端模型 ----
		// 26.2 没有 BlockStateProvider / ItemModelProvider 父类，这两个 provider 无法编译，
		// 已移出流水线。模型 JSON 仍随 mod 发布，只是不再由 datagen 重建。
		// generator.addProvider(true, new DMZBlockStateProvider(packOutput));
		// generator.addProvider(true, new DMZItemModelProvider(packOutput));
	}
}
