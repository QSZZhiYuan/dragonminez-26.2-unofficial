# DragonMineZ 26.2 — 非官方移植 / Unofficial Port

**这是面向 Minecraft 26.2 / NeoForge 的社区非官方移植，由本仓库独立维护。原作者未参与本移植版的维护，本仓库不代表 DragonMineZ 官方。**

This is an independently maintained, unofficial community port of DragonMineZ to Minecraft 26.2 / NeoForge. It is not an official DragonMineZ release and is not affiliated with or endorsed by the original developers.

原项目：<https://github.com/DragonMineZ/dragonminez>。原作者及贡献者的署名、版权声明与许可文件均予保留。

本仓库保存移植源码、资源、中文本地化与修复记录。当前内部构建版本为 `3.0.0`；这个编号属于本移植版，不表示与官方同号版本功能一致。移植仍在进行，尚未完成的内容和未验证的问题见下方进度记录。

## 使用与依赖

本仓库提供移植源码，不托管发行 JAR。请按下方说明自行构建，并安装以下依赖。客户端和服务器应使用同一版模组。

将模组 JAR 和所需依赖放入对应游戏实例的 `mods` 文件夹。

## 环境要求

- Minecraft 26.2
- NeoForge 26.2.0.59 或更高的 26.2 版本
- Java 25
- GeckoLib：26.2 对应的 5.5.6 版本
- TerraBlender：26.2-26.2.0.0.2 或符合模组声明的兼容版本
- Curios：16.0.0+26.2 或符合模组声明的兼容版本

## 构建

使用 Java 25，在仓库根目录执行。Gradle Wrapper 使用 Gradle 9.2.1：

~~~powershell
.\gradlew.bat build
~~~

Linux / macOS：

~~~sh
sh ./gradlew build
~~~

构建输出为 `build/libs/dragonminez-3.0.0.jar`。打包会自动运行数据生成并优化资源；首次构建需要下载 Gradle、NeoForge 和其他构建依赖。GeckoLib 的编译依赖保存在 `libs/`，说明见 [libs/README.md](libs/README.md)。

发布构建包含 MariaDB 和 HikariCP 的 jar-in-jar 依赖。运行游戏仍需要单独安装 GeckoLib、TerraBlender 和 Curios。

如果要运行开发客户端：

~~~powershell
.\gradlew.bat runClient
~~~

## 移植内容与进度

- Minecraft 26.2 / NeoForge API、界面与渲染适配。
- 中文本地化、第一人称手部、武器与装备显示相关修复。
- 气焰、聚气爆发、气功和后处理效果的移植与修复。
- 飞行模式、维度天空、任务进度及多人状态同步相关修复。

以上列出本分支涉及的工作范围，具体完成状态和验证范围以记录为准：

- [问题跟踪清单](BUG_TRACKER_2026-11-14.md)
- [上游内容移植进度](UPSTREAM_PORT_STATUS.md)
- [上游功能对照（2026-11-13）](UPSTREAM_FEATURE_COMPARISON_2026-11-13.md)

移植问题请提交到本仓库的 [Issues](../../issues)。

## 致谢

DragonMineZ 原作者：**Yuseix300 & ezShokkoh**，以及原项目的所有贡献者。Dragon Ball 原作为 **Akira Toriyama（鸟山明）**。本仓库保留原有素材和代码的归属声明。

## 许可

项目保留原有 [GNU GPL v3](LICENSE) 许可及版权声明。第三方许可见 [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES)；各第三方依赖同时保留其自身许可。
