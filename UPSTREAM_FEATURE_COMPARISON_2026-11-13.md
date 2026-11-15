# DragonMineZ 上游新版与当前移植版功能对照

对比日期：2026-11-13。上游为 main 分支，源码版本 2.2.1，提交 f24b89d6a3c740c5cc125065a4b6eb366777817b；适配 Minecraft Forge 1.20.1。对照对象是本目录的 Minecraft 26.2 NeoForge 移植版，包版本标为 3.0.0。两者版本编号独立，本文按实际源码内容比较。

统计来自默认注册及任务生成定义。本次没有运行客户端或游戏测试；这里确认的是源码存在的内容，不能据此保证上游全部功能在游戏中运行正常，也不能直接覆盖到 26.2 使用。

**可量化的差异**

| 项目 | 我们当前版 | 上游新版 |
| --- | ---: | ---: |
| 默认种族 | 6 | 7 |
| 默认主线剧情分类 | 6 | 9 |
| 默认主线任务定义 | 121 | 264 |
| SkillsConfig 有效显式登记的默认技能（排除注释） | 45 | 76 |
| 防具套装注册定义 | 90 | 94 |
| 主要 HUD 样式 | 2 | 4 |

技能净增 31 项。武器、刷怪蛋、弹体和同一角色不同形态会分别注册；不能把文件或注册 ID 的增量当成新增独立角色数量。物品和方块还有动态注册，因此本文不以简单正则统计宣称它们的总新增数量。

**新增与扩展清单**

| 内容 | 上游增加或改进 | 我们当前版的情况 |
| --- | --- | --- |
| 剧情 | 新增初代龙珠、GT、DAIMA 三条默认剧情；初代含皮拉夫、天下第一武道会、红缎带军团、比克大魔王等；GT 含贝比、超级 17 号、邪恶龙；DAIMA 延伸至第三眼戈玛 | 缺少这三条线；原有六条线的部分任务也少于新版 |
| 新种族 | Glind（格林德）及九种形态定义；神之气隐藏、恢复增强、抑制敌人治疗、善恶对立加成等种族能力 | 默认仍为原有六种族 |
| 种族能力 | 赛亚人濒死倒地与恢复提升；那美克同化槽及目标捕获；魔人吸收槽和释放；冰冻恶魔气储备；生物人造人形态相关吸收、自爆及小沙鲁；人类与改造人的适应/屏障能力体系 | 已有旧版种族天赋，这些是规则与实现扩展，不能将所有天赋概念都算成首次新增 |
| 赛亚人变身玩法 | 假超级赛亚人、超级赛亚人之神仪式；仪式默认需要五名供能者和一名容器，并有善恶与持续时间规则 | 缺少这些服务端流程；已有普通神形态不等于已有仪式 |
| 默认技能 | 净增 9 个气功、17 个打击技、5 个闪避/辅助技，详见后表；太阳拳、狂怒吼只是被归入闪避分类，不计入新增 | 缺少这 31 项默认技能及其完整处理流程 |
| Ki Surge 满气爆发 | 满气后继续聚气默认 4 秒，进入默认持续 12 秒、冷却 15 秒的强化状态；气伤、攻击/移动/飞行速度、消耗、气焰均联动，并有范围爆发 | 我们只补了聚气 UI 发光、满气短震和短暂爆发视觉；没有完整 Surge 状态和强化数值 |
| 气焰与特效 | 可选 3D 气焰、轮廓、拖尾与 Bloom 管线；新增残像、斩击、次元破碎、同化、神仪式等特效 | 保留旧气焰并修复了部分渲染；没有这整套新增管线。上游 3D 气焰默认关闭，可由用户开启 |
| HUD | 新增 Modern 与 Minecraft 风格；增加可拖拽 HUD 编辑器、队伍状态/世界标记、通知、气储备和 Surge 等状态显示 | 主要是 Xenoverse 与 Alternative 两种旧样式；数字、圆环、六维图等已有我们的修复 |
| 角色外观编辑 | 新发型编辑器，分段/关节、历史记录、碰撞与物理模拟；皮肤/纹身像素编辑器；技能外观与预览界面升级 | 已有旧发型编辑器和技能创建界面，尚未迁移这些新版编辑能力 |
| 魔界 | 新增 dragonminez:demon_realm 维度，以及第一、第二、第三魔界对应的三个生物群系、魔界村庄/村民、地形与材料 | 没有魔界维度。这里是一个新维度中的三个群系，不是三个独立维度 |
| 地府与建筑 | 地府地标、武道会场及地形内容扩展；新 Boss 场地；结构贴地与分批生成处理 | 已有旧版地府与结构系统，新增场地和生成机制尚未迁移 |
| 世界 Boss | 邪念波、达列斯、金属古拉/比高星、三个塔玛加米、戈玛等世界 Boss 场地/会话；贡献统计、奖励与倒地救援界面 | 只有旧袭击等系统，没有新版世界 Boss 框架 |
| 武道会 | 可配置淘汰赛/车轮赛、报名、规则确认、准备、超时、场地保护和奖励；包含占卜婆婆、沙鲁、地府、天下第一武道会主持入口配置 | 缺少新版武道会流程 |
| 战斗与联机 | 增加速度闪避、闪避技、打击连段/演出/对抗、攻击前摇与命中判定处理；气共享/输气及元气支援；敌人 AI 协作与招式预警等扩展 | 已有旧战斗、对波、锁定和队伍功能；新版扩展未整体迁移 |
| 新载具 | 红/黄汽车、CC576 飞行器、弗利萨悬浮舱；座位/操控/飞行模式、引擎声和载具零件 | 已有太空舱和筋斗云，新车与飞行器未迁移 |
| 新武器与服装 | 塔玛加米剑、三叉戟、大锤；增加四套服装注册：brightlys_regular_wear、scarletblaze、taiyou_gi、trunks_xeno | 没有三种塔玛加米武器和这四套新增服装。部分服装属于特殊/赞助内容，注册存在不代表都有常规生存合成配方 |
| 工作台与物品 | Pattern Station 六乘六合成界面，以染料等制作服装图纸；熟练度胶囊、圣水、魔眼、仙豆袋、红/蓝/黄仙豆与对应种子 | 缺少这些新增道具与工作台；已有服装图纸和普通仙豆不能算成新版首次新增 |
| 医疗虫与合体 | 医疗虫提供恢复、减伤、熟练度等效果；成对甲虫可用于玩家合体；合体外观、头发与种族特征处理扩展 | 已有旧舞蹈/耳环合体；新增虫类途径和外观处理尚未迁移 |
| 农业与方块 | 那美克苔藓等八类增益作物、巨鱼/巨龟和食物；精神树木/根/叶/果方块；魔界矿石和建筑材料、地府材料等 | 部分精神树果实、农业、食物已存在；新版扩展内容未迁移 |
| 任务与对话 | 新增护送、配送、检查点竞速、切磋、波次生存等目标实现与扩展接口；队伍任务协同；重做 NPC 对话的头像、打字机、选项和任务页 | 具有旧任务树、NPC 对话与组队；不是从零新增这些基础功能 |
| 修炼小游戏 | 加强服务端会话和成绩核算；音游校准、谱面与唱片相关处理；导师/挑战流程扩展 | 已有修炼小游戏，此处主要是升级和重构 |
| 死亡与许愿 | 占卜婆婆临时还阳（默认一小时）、地府死亡状态修炼倍率；增加复活愿望及属性/属性点奖励类型，默认许愿表调整 | 已有死亡/复活和许愿基础，但没有这些新增服务与愿望类型；属性点字段本身在当前版已存在 |
| 扩展支持 | 角色菜单标签、任务目标/奖励扩展接口；Apotheosis、WeaponLeveling 兼容类；配置和同步机制调整 | 尚未迁移这些新增接口与适配；大量 API 差异只属于版本适配，未算作新玩法 |

**默认主线任务定义数量**

| 剧情 | 当前版 | 上游新版 |
| --- | ---: | ---: |
| 初代龙珠 | 0 | 52 |
| 赛亚人 | 8 | 16 |
| 弗利萨 | 16 | 22 |
| 人造人/沙鲁 | 15 | 22 |
| 未来 | 7 | 7 |
| 布欧 | 35 | 42 |
| 剧场版 | 40 | 40 |
| GT | 0 | 46 |
| DAIMA | 0 | 17 |
| 合计 | 121 | 264 |

这是 QuestDefaults 中的任务步骤数；旧线任务增加还包含战斗阶段拆分与流程调整。相同数量也不意味着具体内容完全相同。

**31 项新增默认技能**

| 分类 | 数量 | 上游技能名及 ID |
| --- | ---: | --- |
| 气功 | 9 | Heat Dome Attack（heat_dome）；Tri-Beam 气功炮（kikoho）；Neo Tri-Beam 新气功炮（neo_kikoho）；Dodon Ray 洞洞波（dodonpa）；Mouth Blast（mouth_blast）；Assault Rain（assault_rain）；Blaster Meteor（blaster_meteor）；Kill Driver（kill_driver）；Hellzone Grenade（hellzone_grenade） |
| 打击 | 17 | Oozaru Slam（oozaru_slam）；Gum Punch（gum_punch）；Blue Hurricane（blue_hurricane）；Dimensional Fist（dimensional_punch）；Dimensional Slash（dimensional_sword_attack）；Spirit Breaking Cannon（spirit_breaking_cannon）；Grab Throw（grab）；Shining Sword Attack（shining_sword_attack）；Meteor Combo（combo_meteor）；Meteor Burst（meteor_burst）；Mystic Ball Attack（mystic_ball）；Darkness Eye Beam（darkness_eye_beam）；Death Chaser（death_chaser）；Android Combo（android_combo）；Beam Sword Slash（beam_sword_slash）；Perfect Rush（perfect_rush）；Spirit Breaker（spirit_breaker） |
| 闪避/辅助 | 5 | Afterimage 残像（afterimage）；Sleep Recovery（sleep_recovery）；Dimensional Warp（dimensional_teleport）；Candy Beam 糖果光线（candy_beam）；Gum Bind（gum_bind） |

**关键源码依据**

- 版本与基准：本目录 gradle.properties；上游下载目录内 gradle.properties 和 update.json。
- 剧情：当前 [QuestDefaults.java](src/main/java/com/dragonminez/common/quest/QuestDefaults.java)；上游 [QuestDefaults.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/quest/QuestDefaults.java)、[SideQuestDefaults.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/quest/SideQuestDefaults.java)。
- 技能：当前 [SkillsConfig.java](src/main/java/com/dragonminez/common/config/SkillsConfig.java)；上游 [SkillsConfig.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/SkillsConfig.java)。
- 种族与变身：[ConfigManager.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/ConfigManager.java)、[DefaultFormsFactory.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/DefaultFormsFactory.java)、[GeneralServerConfig.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/GeneralServerConfig.java)、[DMZCommon.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/DMZCommon.java)。
- Surge：[CombatConfig.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/CombatConfig.java)、[KiSurgeService.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/events/players/KiSurgeService.java)。当前补过的视觉见 [KiChargeVisualState.java](src/main/java/com/dragonminez/client/render/effects/KiChargeVisualState.java)、[KiChargeGlow.java](src/main/java/com/dragonminez/client/gui/hud/KiChargeGlow.java)。
- HUD/3D 气焰：[HudLayoutConfig.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/HudLayoutConfig.java)、[HudEditorScreen.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/gui/config/HudEditorScreen.java)、[GeneralUserConfig.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/GeneralUserConfig.java)、[Aura3DRenderer.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/render/effects/Aura3DRenderer.java)。
- 外观编辑：[PixelEditorScreen.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/gui/character/pixel/PixelEditorScreen.java)、[HairEditorScreen.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/gui/hair/HairEditorScreen.java)、[HairSimulation.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/render/hair/HairSimulation.java)、[TechniqueStyleScreen.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/client/gui/character/TechniqueStyleScreen.java)。
- 魔界：[DemonRealmDimension.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/world/dimension/DemonRealmDimension.java)、[DemonRealmBiomes.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/world/biome/DemonRealmBiomes.java)。
- 世界 Boss/武道会：[WorldBossManager.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/world/worldboss/WorldBossManager.java)、[TournamentDefinition.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/config/TournamentDefinition.java)、[Tournament.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/world/tournament/Tournament.java)。
- 物品与服装：当前 [MainItems.java](src/main/java/com/dragonminez/common/init/MainItems.java)；上游 [MainItems.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/init/MainItems.java)、[MainBlocks.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/init/MainBlocks.java)、[PatternStationMenu.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/init/menu/menutypes/PatternStationMenu.java)、[DMZPatternRecipeProvider.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/datagen/DMZPatternRecipeProvider.java)。
- 联机/合体/还阳：[KiShareService.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/events/players/KiShareService.java)、[BeetleFusionTracker.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/util/BeetleFusionTracker.java)、[BabaReviveService.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/server/util/BabaReviveService.java)、[DefaultWishes.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/wish/DefaultWishes.java)。
- 任务/修炼扩展：[QuestParty.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/quest/QuestParty.java)、[MinigameSessionManager.java](https://github.com/DragonMineZ/dragonminez/blob/f24b89d6a3c740c5cc125065a4b6eb366777817b/src/main/java/com/dragonminez/common/training/MinigameSessionManager.java)。

对照中保留我们已做的汉化、第一人称、模型、属性上限及其他移植修复，不把上游有同名文件当作已修复相同问题的证据。

