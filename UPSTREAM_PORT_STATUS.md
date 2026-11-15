# 2.2.1 内容移植进度

目标：将 DragonMineZ 2.2.1 的新增内容移植到当前 NeoForge 26.2，保留已有汉化、存档字段及移植修复。

上游基准：f24b89d6a3c740c5cc125065a4b6eb366777817b。2026-11-14 再次核对 main，提交未改变。
完整差异见 UPSTREAM_FEATURE_COMPARISON_2026-11-13.md。

## 已独立交付的退出修复

- 全员退出日志 latest (3).log：缺失 impact_frame_inv_80，资源恢复导致房主关闭局域网。
- 超四日志 latest (4).log：缺失旧路径 shaders/post/transformation_outline.json，相同退出路径。
- 调用处检查资源存在，绑定前确认加载成功；ShaderManagerMixin 对 dragonminez 命名空间的可选后处理单独缓存和编译，失败跳过，重载时重试，不调用原版恢复回调。
- 已编译打包：build/libs/dragonminez-3.0.0-multiplayer-disconnect-fix-r2.jar。
- 未启动游戏测试此修复。此包仍不包含恢复后的变身描边。
- 移植前源码备份：build/backups/pre-upstream-port-20261114.zip（包含最初退出修复；R2 新增 ShaderManagerMixin 另见源码）。

## 移植清单

- [x] 渲染基础：实时参数、冲击帧、太阳拳、菜单模糊与灰度、Bloom 和变身描边已编译并进入开发客户端验证。
- [ ] 3D 气焰：smooth、sparking 已显示；god、拖尾及全部新增技能特效尚未验证/完成。
- [x] Ki Surge 状态、数值、同步和 HUD：实际按键聚气 80 tick 进入，持续 241 tick，冷却和打断通过。
- [ ] HUD：四种样式、布局编辑、通知、侦测器与技能栏已迁移；队伍、储备、怒气、还阳和 Boss 依赖对应服务端系统。
- [ ] 新种族 Glind、种族能力、变身与神仪式。
- [ ] 31 项新技能及其服务端和动画流程。
- [ ] 264 步默认剧情和扩展任务类型、对话。
- [ ] 魔界、世界 Boss、武道会、结构及生成。
- [ ] 新武器、防具、载具、图纸工作台、道具、农业与合体。
- [ ] 头发/像素外观编辑与其他扩展。
- [ ] 保留汉化与旧档数据，编译、客户端验证和最终打包。

验证授权：退出修复只编译打包；后续新版内容可以启动客户端测试。

## 当前验证证据（2026-11-14）

- 开发客户端：build/upstream-hud-client-r4.log；仅使用 run/saves/新的世界，未操作用户实际客户端目录。
- Ki Surge：run/ui-smoke-surge.txt，RESULT=PASS、INTERRUPT=PASS。
- 四种 HUD：run/screenshots/hud-xenoverse.png、hud-compact.png、hud-modern.png、hud-vanilla.png。
- 头像：portrait-angles-r3.png 证实 26.2 额外旋转半圈；hud-portrait-r4.png 已修正为正面。
- 编辑器：ui-smoke-hud-moved/resized/undone/redone/cancelled.txt 记录操作与恢复；saved/reopened.txt 的主 HUD 位置、缩放完全一致。测试配置已恢复。
- 百分比：hud-full-release-r4.png 显示 100%，使用完整种族图标区域；取整为 100 时立即填满。
- 中文：补齐当前 121 个旧主线任务的 249 个名称、说明、目标键；hud-quest-zh-r4.png 显示中文任务通知。新增 2.2.1 任务的汉化仍随任务系统迁移。
- 29 个旧合成配方已转换为 26.2 ingredient 格式，r2/r4 世界载入不再出现这些配方的解析错误。
- 尚未验证：Iris 光影包兼容性、多人完整回归、所有变身描边精度、god 气焰。尚有 misplaced global_loot_modifiers 和旧模型资源警告待处理。

开发驱动位于 src/uiSmoke，仅由 build/ui-smoke.init.gradle 加载，不进入发行 JAR。

## 本轮爆发效果修复（2026-11-14）

- 用户提供 QQ20261114-112509-HD.mp4 作为旧版效果参考；普通气焰已经由用户确认正常。本轮只补爆发相关差异。
- 人物闪电接入上游 LightningBoltRenderer 的分叉 ribbon 几何，保留第一人称边缘、第三人称、Bloom 和 Iris 后绘制路径。
- 新增独立 AuraBorderRenderer：捕获人物实际动画轮廓，按上游三档模糊场绘制贴身火焰及 Bloom；仅在 Surge 生效和短暂消退时出现，不受变身描边开关影响。
- 满气爆发保留服务端的爆炸实体、粒子、声音和震动，移除附加的客户端重复球形闪光；普通能量同步不再预测额外爆发。
- 爆炸、闪电和神性粒子恢复加法混合，并关闭深度写入，避免遮住后续气焰外壳。
- 先前动作修复：ki_controller 在播放结束、停止或释放保持姿态时 reset，防止 PLAY_AND_HOLD 继续占用骨骼，覆盖走路与攻击。
- 先前菜单修复：11 个菜单的 scissor 坐标不再重复乘 UI 缩放；尚待逐菜单人工核对。
- 神仪式光壳改为在人物原始绘制上叠加，完整六人仪式仍未验证，下一阶段优先完成神仪式，再继续气功。
- 纯回归检查：build/surge-effects-regression-20261114.log，41 项通过，0 项失败。使用 build/render-regression.init.gradle 排除游戏启动用 junit-fml listener；默认 Gradle test 的 FML 引导仍无法独立启动。
- 开发客户端第 10 轮已确认贴身火焰和第一/第三人称闪电；Surge 79 tick 进入、320 tick 结束、打断检查通过。最后的粒子深度修正另在第 12 轮验证。第 11 轮因运行期间重编译造成临时类缺失，已停止该并行操作。
- 第 14 轮针对聚气/爆发时世界变暗，将 Bloom 合成改为仅叠加发光并固定发光层 alpha 为 0；客户端热重载后 Bloom pipeline 编译成功，固定天空区域亮度差约 0.9%，青色气焰与闪电正常可见。截图见 run/screenshots/bloom14-before.png、bloom14-charging.png，测量见 build/bloom-no-dimming-20261114.json。
- 最终发行包：build/libs/dragonminez-3.0.0-26.2-surge-effects-r1.jar；build/surge-effects-release-20261114.log 记录打包成功。已检查 ZIP 完整性、关键效果资源和类存在，并确认没有开发用 src/uiSmoke 驱动类。
- 本次按用户要求先交付当前阶段 JAR，2.2.1 的完整内容移植仍按上面的未完成清单继续。
