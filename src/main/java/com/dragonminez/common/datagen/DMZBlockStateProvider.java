package com.dragonminez.common.datagen;

/**
 * 占位类 —— 26.2 迁移中暂时停用。
 *
 * <h2>为什么停用</h2>
 * 本类原本继承 NeoForge 的 {@code net.neoforged.neoforge.client.model.generators.BlockStateProvider}，
 * 该包（连同 {@code ExistingFileHelper}、{@code ModelFile}）**在 26.2 被整体删除**。
 * MC 侧对应的模型生成能力迁到了 {@code net.minecraft.client.data.models.*}
 * （{@code BlockModelGenerators} / {@code ItemModelGenerators}，1.21.4+ 的数据驱动风格），
 * 两边的 builder API 完全不同，属于「重写」而非「改签名」。
 *
 * <h2>影响面</h2>
 * **零运行时影响**：{@code src/main/resources/assets/**} 与 {@code src/generated/resources/**}
 * 里的方块状态 / 模型 JSON 仍然随 mod 打包并被正常加载。
 * 断掉的只是「用 datagen 重新生成这些 JSON」这条路。
 *
 * <h2>原始实现</h2>
 * 已完整保存在同目录的 {@code DMZBlockStateProvider.java.262-disabled.txt}，
 * 供 M6 资产阶段按新 API 重写时对照。
 */
public final class DMZBlockStateProvider {
	private DMZBlockStateProvider() {}
}