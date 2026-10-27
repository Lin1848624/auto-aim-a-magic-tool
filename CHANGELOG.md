# Changelog

All notable changes to **Auto Aim: A Magic Tool** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> Version lines are split by mod loader / Minecraft version: `1.0.x` = Minecraft Forge 1.20.1, `1.1.x` = NeoForge 1.21.1.

---

## [1.2.0] - 2026-10-27

### English

**Added**

- **Lock part (head lock) mode**: a new `lockPart` setting chooses which point of the target the crosshair is pulled to — `CENTER` (hitbox middle, the previous behaviour) or `HEAD` (eye height, plus an adjustable `headOffset` of −0.3…+0.3 blocks).
- `HEAD` matches the headshot rule used by Timeless and Classics Zero (TaCZ), whose default check is "hit point within eye height ±0.25 blocks above the entity's feet" — aiming at eye height puts the shot in the middle of that band and leaves the largest margin.
- New key binding `J` (configurable): toggles between body centre and head in-game, with an action-bar hint showing the active mode. The same toggle is available in the config screen as a `锁定部位` row, plus a `头部微调` numeric row.
- `headOffset` is clamped inside the hitbox (0.05 blocks margin), so a large positive offset can never aim above the target's head.

**Changed**

- The crosshair-based candidate selection now measures the angle to the same aim point used by the auto-aim, so "what you point at" and "what gets locked" agree.
- Config screen row spacing is now computed from the actual row count (9 → 10 rows fit at any GUI scale).

**Notes**

- Existing configs keep working: `lockPart` defaults to `CENTER`, so behaviour is unchanged until you switch it.
- The head point is a plain coordinate — no TaCZ API is used, so it works with any gun mod and with vanilla bows/crossbows (projectile aiming simply targets the head instead of the middle).

### 中文

**新增**

- **锁定部位（头部锁定）模式**：新增配置项 `lockPart`，决定准心被拉到目标的哪一点 —— `CENTER`（碰撞箱中点，原行为）或 `HEAD`（眼睛高度，另加 `headOffset` 上下微调，范围 −0.3 ~ +0.3 格）。
- `HEAD` 与永恒枪械工坊：零（TaCZ）的爆头判定对齐：TaCZ 默认规则是「命中点离脚底高度落在眼睛高度 ±0.25 格内」，瞄准眼睛高度即落在判定带正中心，余量最大。
- 新增可改键的 `J` 键：游戏内直接切换「身体中心 / 头部」，准星下方会提示当前模式；配置界面同时新增「锁定部位」按钮行与「头部微调」输入行。
- `headOffset` 会被收进碰撞箱内部（留 0.05 格边距），正微调调到最大也不会瞄到头顶之外。

**变更**

- 准星锁定选取改为与自动瞄准使用同一个瞄准点，做到「瞄到的」就是「锁到的」。
- 配置界面的行距改为按实际行数计算（9 → 10 行，任何 GUI 缩放下都不溢出）。

**说明**

- 旧配置可直接沿用：`lockPart` 默认 `CENTER`，不切换时行为与 1.1.0 完全一致。
- 头部瞄准点只是一个坐标，不依赖 TaCZ 的任何 API，所以对其他枪械模组同样有效；原版弓/弩的弹道模式也会改为瞄准头部。

---

## [1.1.0] - 2026-10-01

### English

**Changed**

- Ported the mod from Minecraft Forge 1.20.1 to **NeoForge 1.21.1** (built against NeoForge 21.1.252, Java 21).
- Build system migrated from ForgeGradle 6 to ModDevGradle 2.0.148 (Gradle 8.14); run configurations are now `client` / `server` / `data`.
- Mod metadata moved to `META-INF/neoforge.mods.toml` (dependency entries now use `type="required"`); resource pack format bumped to 34.
- Client-only handling now uses `@Mod(dist = Dist.CLIENT)` instead of `clientSideOnly=true` (`clientSideOnly` and `displayTest` no longer exist in NeoForge 1.21.1).
- Config API migrated from `ForgeConfigSpec` to `ModConfigSpec`; the config screen is registered through `IConfigScreenFactory`.
- Tick and rendering events updated (`ClientTickEvent.Post`, `RenderLevelStageEvent`); removed the deprecated `EventBusSubscriber.Bus` parameter.
- World rendering updated to the 1.21.1 pipeline: `DeltaTracker` for partial ticks, and `Tesselator#begin` + `BufferUploader#drawWithShader` for the trajectory lines.
- Item API update: `ItemStack#getUseDuration(player)` (changed in 1.20.5).

**Compatibility**

- Requires Minecraft 1.21.1 with NeoForge 21.1.x.
- Client-side only: do not install it on a dedicated server (it will not crash, but it has no effect there).
- The config file `config/autoaim-client.toml` keeps the same keys, so existing settings carry over.

**Notes**

- Version numbering continues on a separate line: `1.0.0` stays the Forge 1.20.1 release, `1.1.0` is the first NeoForge 1.21.1 release.
- No gameplay or behaviour changes in this release.

### 中文

**变更**

- 模组从 Minecraft Forge 1.20.1 移植到 **NeoForge 1.21.1**（基于 NeoForge 21.1.252、Java 21 构建）。
- 构建体系由 ForgeGradle 6 迁移到 ModDevGradle 2.0.148（Gradle 8.14）；运行配置改为 `client` / `server` / `data`。
- 模组元数据改为 `META-INF/neoforge.mods.toml`（依赖项改用 `type="required"`）；资源包格式升级到 34。
- 纯客户端判定改用 `@Mod(dist = Dist.CLIENT)`，替代 `clientSideOnly=true`（NeoForge 1.21.1 已移除 `clientSideOnly` 与 `displayTest`）。
- 配置 API 由 `ForgeConfigSpec` 迁移到 `ModConfigSpec`；配置界面改由 `IConfigScreenFactory` 注册。
- 事件适配新版本（`ClientTickEvent.Post`、`RenderLevelStageEvent`），并去掉已弃用的 `EventBusSubscriber.Bus` 参数。
- 世界渲染适配 1.21.1 新管线：插值使用 `DeltaTracker`，弹道线改用 `Tesselator#begin` + `BufferUploader#drawWithShader`。
- 物品 API 变更：`ItemStack#getUseDuration(player)`（1.20.5 起生效）。

**兼容性**

- 需要 Minecraft 1.21.1 + NeoForge 21.1.x。
- 纯客户端模组，请勿安装在专用服务器（不会崩溃，但也不起作用）。
- 配置文件 `config/autoaim-client.toml` 键名未变，旧配置可直接沿用。

**说明**

- 版本号分线管理：`1.0.0` 仍为 Forge 1.20.1 版，`1.1.0` 是首个 NeoForge 1.21.1 版。
- 本版本没有玩法/行为改动。

---

## [1.0.0] - 2026-08-28

### English

**Added**

- First public release, for Minecraft Forge 1.20.1.
- ESP highlight: glowing outlines for nearby living entities (yellow = candidates, red = locked target), toggled with `H`.
- Target locking: `K` locks the entity closest to the crosshair (within the tolerance angle, default 15°), falling back to the nearest entity inside the lock range (default 24 blocks); press `K` again or `L` to unlock. Auto-unlocks when the target dies or is removed, moves too far away, or the player dies.
- Smooth auto-aim that continuously turns the camera toward the locked target (adjustable speed).
- Projectile prediction mode for bow (using the current draw progress), crossbow, trident, snowball, egg, ender pearl, experience bottle and splash/lingering potions: computes the launch angle including gravity and target leading (max 3.0 s flight time) and draws the predicted trajectory arc.
- In-game config screen (`O`, or the "Config" button in the mod list) with 8 options: display range, lock range, tolerance angle, aim speed, hostiles only, include players, highlight locked target, show trajectory. Changes apply instantly and are saved to `config/autoaim-client.toml`.
- Unit tests for the aiming and trajectory math.

**Notes**

- Pure client-side mod: no networking, no server component.

### 中文

**新增**

- 面向 Minecraft Forge 1.20.1 的首个公开版本。
- ESP 高亮：周围生物显示发光轮廓（黄色 = 候选，红色 = 锁定目标），`H` 键开关。
- 目标锁定：`K` 锁定准星最接近的生物（容差角内，默认 15°），否则锁定范围内最近的生物（默认 24 格）；再按 `K` 或按 `L` 取消。目标死亡/被移除、距离过远、玩家死亡时自动解锁。
- 平滑自动瞄准：持续把视角转向锁定目标（速度可调）。
- 弹道预测模式：弓（按当前拉弓进度）、弩、三叉戟、雪球、鸡蛋、末影珍珠、附魔之瓶、喷溅/滞留药水会计算含重力与提前量的发射角（最长 3.0 秒飞行时间），并绘制预测弹道弧线。
- 游戏内配置界面（`O` 键或模组列表的「配置」按钮），共 8 项：显示范围、锁定范围、容差角、瞄准速度、仅敌对生物、包含玩家、锁定目标高亮、显示弹道轨迹；改动即时生效并写入 `config/autoaim-client.toml`。
- 瞄准与弹道数学的单元测试。

**说明**

- 纯客户端模组，无网络通信、无服务端组件。

---

[1.2.0]: https://github.com/Lin1848624/auto-aim-a-magic-tool/releases/tag/v1.2.0
[1.1.0]: https://github.com/Lin1848624/auto-aim-a-magic-tool/releases/tag/v1.1.0
[1.0.0]: https://github.com/Lin1848624/auto-aim-a-magic-tool/releases/tag/release
