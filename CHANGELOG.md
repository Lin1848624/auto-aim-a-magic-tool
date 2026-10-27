# Changelog

All notable changes to **Auto Aim: A Magic Tool** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> This file documents the **Minecraft Forge 1.20.1** line. The NeoForge 1.21.1 line is kept on the `main` branch with its own `CHANGELOG.md`.

---

## [1.2.0-1.20.1] - 2026-10-27

### English

**Added**

- **Lock part mode**: the new `lockPart` setting moves the aim point from the hitbox centre (`CENTER`, the previous behaviour) to the entity's eye height (`HEAD`), with an adjustable `headOffset` of −0.3…+0.3 blocks.
- `HEAD` matches the headshot rule used by Timeless and Classics Zero (TaCZ): the hit point counts as a headshot when it lands within eye height ±0.25 blocks above the entity's feet, so aiming at eye height keeps the shot in the middle of that band.
- New key binding `J`: switches between body centre and head in-game and shows the active mode on the action bar. The same toggle is available in the config screen as a `锁定部位` row, together with a `头部微调` numeric row.
- `headOffset` is clamped inside the hitbox (0.05 blocks margin), so a large positive offset never aims above the target's head.

**Changed**

- The crosshair-based candidate selection now uses the same aim point as the auto-aim, so "what you point at" and "what gets locked" agree.
- Config screen row spacing is derived from the actual row count, and numeric fields accept a minus sign.

**Fixed**

- The mod description no longer shows mojibake in the mod list (`gradle.properties` is parsed as ISO-8859-1, so the text now uses `\uXXXX` escapes).

**Notes**

- Configurations from `1.0.0` keep working: `lockPart` defaults to `CENTER`, so nothing changes until you switch it.
- The head point is plain coordinate math — no TaCZ API is used, so it also works with other gun mods and with vanilla bows/crossbows.

### 中文

**新增**

- **锁定部位（头部锁定）模式**：新增配置项 `lockPart`，把瞄准点从碰撞箱中点（`CENTER`，原行为）改为头部（`HEAD`，眼睛高度），并可用 `headOffset` 在 −0.3~+0.3 格范围内上下微调。
- `HEAD` 与永恒枪械工坊：零（TaCZ）的爆头判定一致：命中点离脚底高度落在眼睛高度 ±0.25 格内即判爆头，瞄准眼睛高度正好落在判定带中心，上下余量最大。
- 新增可改键的 `J` 键：游戏内直接切换「身体中心 / 头部」，准星下方提示当前模式；配置界面同时新增「锁定部位」按钮行与「头部微调」输入行。
- `headOffset` 会被收进碰撞箱内部（留 0.05 格边距），正微调调到最大也不会瞄到头顶之外。

**变更**

- 准星锁定选取改为与自动瞄准使用同一个瞄准点，做到「瞄到的」就是「锁到的」。
- 配置界面行距改为按实际行数计算；数值输入框允许负号。

**修复**

- 模组列表里的中文描述不再乱码（`gradle.properties` 按 ISO-8859-1 解析，改为 `\uXXXX` 转义）。

**说明**

- `1.0.0` 的旧配置可直接沿用：`lockPart` 默认 `CENTER`，不切换时行为与旧版完全一致。
- 头部瞄准点只是坐标运算，不依赖 TaCZ 的任何 API，所以对其他枪械模组、对原版弓/弩同样生效。

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

[1.2.0-1.20.1]: https://github.com/Lin1848624/auto-aim-a-magic-tool/releases/tag/v1.2.0-1.20.1
[1.0.0]: https://github.com/Lin1848624/auto-aim-a-magic-tool/releases/tag/release
