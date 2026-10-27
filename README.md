# 自动瞄准模组（Auto-Aim）设计文档

日期：2026-08-26
目标环境：Minecraft Forge 1.20.1 / 47.4.22，纯客户端模组
项目目录：`D:\mcmod\auto-aim\auto-aim`（产物 jar 也保存在此）

## 1. 需求

1. 快捷键切换「显示周围生物」（ESP 框线，范围可配置）。
2. 自由选择锁定目标（准星指向锁定：锁定距离准星最近的生物）。
3. 锁定后自动将准心对准目标（平滑持续追踪）。
4. 随时可取消锁定（快捷键 / 目标死亡自动解除 / 超出范围自动解除）。
5. 配置修改简单：快捷键弹出配置界面。
6. 弹道武器（弓/弩/三叉戟/投掷物）额外触发**弹道预测模式**：
   - 按主手武器自动切换模式；
   - 自动瞄准包含重力与提前量求解；
   - 实时显示预测弹道弧线（随发射角/拉弓进度变化）。
7. 头部锁定模式（1.2.0-1.20.1）：配置界面 / `J` 键切换「锁定部位」，把瞄准点从碰撞箱中心改为头部（眼睛高度），
   以便在枪械模组（如 TaCZ 永恒枪械工坊：零）中打出爆头额外伤害。

## 2. 架构总览

纯客户端、零外部依赖。包结构 `com.autoaim`。

```
src/main/java/com/autoaim/
├── AutoAimMod.java            # @Mod("auto-aim")，注册配置/事件/按键
├── Config.java                # ForgeConfigSpec（CLIENT）+ 缓存字段 + 重载监听
└── client/
    ├── KeyBindings.java       # 5 个按键注册
    ├── TargetManager.java     # 候选扫描/筛选/锁定状态机 + 瞄准点计算
    ├── LockPart.java          # 锁定部位枚举：身体中心 / 头部（无 Minecraft 依赖）
    ├── AimController.java     # 纯数学：命中角、瞄准点高度、弹道求解、轨迹点列（可单测）
    ├── EspRenderer.java       # ESP 框线 + 弹道弧线渲染
    └── gui/ConfigScreen.java  # 自建简易配置界面
src/test/java/com/autoaim/client/AimControllerTest.java   # JUnit 单测
src/main/resources/
├── META-INF/mods.toml         # clientSideOnly=true
├── pack.mcmeta
└── assets/auto-aim/lang/en_us.json, zh_cn.json
```

### 必做修正
- `ExampleMod.MODID` 由 `examplemod` 修正为 `auto-aim`（与 `gradle.properties` 的 `mod_id` 一致，否则模组无法加载）。
- 删除 examplemod 示例代码（示例方块/物品/创造标签页/日志）。
- 包名改为 `com.autoaim`。
- `mods.toml` 设 `clientSideOnly=true`（服务器端不加载，displayTest 自动为 IGNORE_ALL_VERSION）。

## 3. 按键绑定（KeyMapping，可在游戏内按键设置修改）

| 按键 | 默认键 | 功能 |
|------|--------|------|
| 显示周围生物 | `H` | 切换 ESP 框线显示 |
| 锁定目标 | `K` | 锁定准星最近生物；再按一次取消 |
| 取消锁定 | `L` | 清除当前锁定 |
| 切换锁定部位 | `J` | 在「身体中心 / 头部」间切换，准星下方提示当前模式 |
| 打开配置界面 | `O` | 弹出配置界面 |

按键分类名：`key.categories.autoaim`。

## 4. 目标扫描与锁定（TargetManager）

- **候选扫描**：客户端 tick 中（每 5 tick 节流），用 `level.getEntitiesOfClass(LivingEntity.class, AABB 范围)` 收集。
- **筛选**：存活、非玩家（除非 `includePlayers=true`）、非观战；可选 `targetHostilesOnly=true` 只取敌对生物。敌对判定启发式：`entity instanceof net.minecraft.world.entity.monster.Monster`（覆盖僵尸/骷髅/苦力怕/末影人/猪灵/凋灵等常见敌对生物；不含史莱姆、末影龙等，作为近似实现，后续可按需扩展）。
- **锁定规则（准星指向锁定）**：按 `K` →
  1. 对每个候选计算「玩家视线方向」与「指向该生物瞄准点方向」的夹角，取最小夹角者；
  2. 若最小夹角 < `lockAngle`（默认 15°），锁定该生物；
  3. 否则锁定范围内距离最近的生物。
  - 再次按 `K` → 取消锁定（切换式）。
- 瞄准点（`TargetManager#aimPointOf`）与自动瞄准共用：`x/z` 取碰撞箱中心，`y` 由「锁定部位」决定（见第 10 节），
  保证「瞄到的」就是「锁到的」。
- **自动解除**：目标死亡/移除、与玩家距离 > `lockRange × 1.5`、玩家死亡、切世界。
- 锁定目标实体引用存在 `TargetManager`，供渲染与瞄准读取。

## 5. 自动瞄准（AimController + tick 应用）

纯静态数学函数放在 `AimController`（可单元测试）：

```
record ProjectileSpecs(double speed, double gravity)
record LookAngles(float yaw, float pitch)
```

- `forItem(ItemStack) → ProjectileSpecs | null`：主手武器 → 弹道参数；非弹道武器返回 `null`（普通模式）。
- `lookAt(eye, target) → LookAngles`：普通命中角（`atan2(dz,dx)-90`、`-atan2(dy,horiz)`）。
- `aimHeightOffset(bbHeight, eyeHeight, part, headOffset) → double`：瞄准点离脚底的高度（身体中心 / 头部，见第 10 节）。
- `aimPoint(feetCenter, bbHeight, eyeHeight, part, headOffset) → V3`：瞄准点世界坐标（`x/z` 沿用碰撞箱中心）。
- `solveLaunch(eye, targetPos, targetVel, specs, maxFlightTime) → Optional<LookAngles>`：弹道求解。
- `computeTrajectory(eye, yaw, pitch, specs, steps) → List<Vec3>`：世界坐标轨迹点列。

### 武器 → 弹道参数表（按 1.20.1 原版射击代码取值，实现时对照反编译源码核实）

| 物品 | speed | gravity |
|------|-------|---------|
| 弓 `bow` | 满弓 3.0；拉弓中按 `BowItem.getPowerForTime(useTicks) × 3.0` 实时计算 | 0.05 |
| 弩 `crossbow` | 3.15 | 0.05 |
| 三叉戟 `trident` | 2.5 | 0.05 |
| 雪球/鸡蛋/末影珍珠/附魔之瓶 | 1.5 | 0.03 |
| 喷溅/滞留药水 | 0.5 | 0.05 |
| 其他 | — | 普通模式（null） |

### 弹道求解（solveLaunch）

给定起点 `eye`、目标当前位置 `P`、目标速度 `V`、弹速 `v0`、重力 `g`。
目标 t 秒后位置 `P(t) = P + V·t`，相对位移 `r(t) = P(t) - eye`，水平分量 `r_h`、竖直 `r_y`。
飞行时间 t 满足（单位方向分解，`sin²+cos²=1`）：

```
q(t) = (v0·t)² − |r_h(t)|² − (r_y(t) + ½·g·t²)² = 0
```

- `q(t)` 在 t→0 为负、t 足够大为正 → 在 `[0.05, 3.0]`（3.0 为内部固定常量，不暴露配置）内先大步扫描再二分/牛顿精化，取**最小正根**（最快命中）。
- 解得 t 后：
  - `yaw = atan2(r_z(t), r_x(t))`（含水平提前量）；
  - `pitch = atan2(r_y(t) + ½·g·t², |r_h(t)|)`。
- 无解（目标超高/超远不可达）→ 返回 `Optional.empty()`，tick 内退回仅水平对准或不动。

### 轨迹点列（computeTrajectory）

- 从 `eye`、当前 `yaw/pitch`、当前弹速（弓按当前拉弓进度），逐 tick 模拟：`pos += vel; vel.y -= g; vel ×= drag`（箭阻力 0.99，投掷物 1.0）。
- 采样约 48 点（覆盖 2.5 秒），返回世界坐标点列供渲染折线。

### tick 应用（ClientTickEvent.Post）

锁定且条件满足（非观战、`mc.screen == null`、非暂停、玩家存活）时：
1. 瞄准点 = `TargetManager.aimPointOf(target)`（身体中心或头部，由配置决定）；
   `specs = forItem(mainHand)`；为 null → `lookAt`；否则 → `solveLaunch`（`targetVel = target.getDeltaMovement()`）。
2. 计算 `yawDiff = wrapDegrees(targetYaw − player.getYRot())`、`pitchDiff`，`player.turn(yawDiff × aimSpeed, pitchDiff × aimSpeed)` 平滑旋转。
3. 同步 `player.yHeadRot` / `yBodyRot`。

## 6. 渲染（EspRenderer，RenderLevelStageEvent / AFTER_ENTITIES）

- **候选 ESP 框线**：显示开关开启时，对范围内每个候选生物画 `AABB` 框线（默认黄色），用 `LevelRenderer.drawLineBox(event.getPoseStack(), box, r, g, b, a)`，box 略外扩 0.02。
- **锁定目标框线**：红色高亮，**不受显示开关控制**，始终绘制（`showLockedTarget` 可关）。
- **弹道弧线**：主手为弹道武器且 `showTrajectory=true` 时，绘制 `computeTrajectory` 点列折线（青色，position-color），随视角俯仰/弓拉弓进度实时更新；不受「显示周围生物」开关控制。
- 事件提供的 `getPoseStack()` 已是相机空间，世界坐标直接可用。

## 7. 配置（Config.java + ConfigScreen）

配置文件自动生成到 `config/autoaim-client.toml`（注释友好、可手改）。

| 配置项 | 类型 | 默认 | 范围 | 说明 |
|--------|------|------|------|------|
| displayRange | int | 24 | 8–128 | ESP 显示范围（格） |
| lockRange | int | 24 | 8–128 | 锁定搜索范围（格） |
| lockAngle | double | 15 | 1–60 | 准星容差角（度） |
| aimSpeed | double | 1.0 | 0.02–1.0 | 自动瞄准平滑速度 |
| lockPart | enum | CENTER | CENTER/HEAD | 锁定部位：身体中心 / 头部 |
| headOffset | double | 0.0 | -0.3–0.3 | 头部瞄准点上下微调（格）；仅 HEAD 生效 |
| targetHostilesOnly | bool | false | — | 仅锁定敌对生物 |
| includePlayers | bool | false | — | 是否把玩家作为目标 |
| showLockedTarget | bool | true | — | 锁定目标始终高亮 |
| showTrajectory | bool | true | — | 显示弹道弧线 |

（`maxFlightTime` 已按用户要求移出配置，内部固定 3.0 秒。）

- `ModConfigEvent.Reloading`/`Loading` → 刷新缓存静态字段，改配置即时生效。
- **配置界面**（`O` 键弹出）：简易 `Screen`，每行 = 名称 + 当前值控件；数值行用输入框（允许 `-` 号，供头部微调使用），
  布尔行用 `开/关` 按钮，枚举行（锁定部位）用循环切换按钮；底部「恢复默认」「关闭」。改动即时生效，关闭时 `config.save()` 写盘。
- 锁定部位还可以在游戏内按 `J` 直接切换（会同时写盘并在准星下方提示当前模式）。
- 同时通过 `RegisterClientExtensionsEvent.registerConfigScreenHandler` 注册，使模组列表里的「配置」按钮也可打开同一界面。

## 8. 构建与测试

- 构建：`./gradlew build`（JDK17，见 gradle.properties 的 `org.gradle.java.home`），jar 产出到 `build/libs`。
- **单元测试**：`AimControllerTest` 覆盖：
  - 静态目标命中角（已知距离/弹速 → 期望 pitch 在容差内）；
  - 移动目标提前量（水平跑动目标 → yaw 超前）；
  - 无解分支（超高/超远 → `Optional.empty`）；
  - 轨迹点列单调递增、终点距离合理。
  - build.gradle 增加 `testImplementation junit-jupiter` + `useJUnitPlatform()`；若网络不可用导致拉依赖失败，退化为编译验证并说明。
- GUI/渲染/输入逻辑无法自动测试，靠编译 + 用户进游戏实测。
- 所有源码、构建产物均在 `D:\mcmod\auto-aim` 目录内。

## 9. 明确不做（YAGNI）

- 不自动攻击/自动开火（只负责把准心对准、显示弹道）。
- 不做目标速度预测之外的提前量高级模型（如风向、多跳）。
- 不引第三方库（Cloth Config 等）。

## 10. 头部锁定模式（2026-10-27，1.2.0-1.20.1）

### 10.1 判定依据（核对过 TaCZ 源码，不是推测）

TaCZ（永恒枪械工坊：零）的爆头判定在 `com.tacz.guns.util.EntityUtil`，核心逻辑：

```java
Vec3 hitBoxPos = hitPos.subtract(entity.position());              // 命中点相对实体脚底
AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);              // 同步配置里可按生物覆盖
if (aabb != null) return new EntityKineticBullet.EntityResult(entity, hitPos, aabb.contains(hitBoxPos));
float eyeHeight = entity.getEyeHeight();                           // 默认规则
boolean headshot = (eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25);
```

- 官方 1.20.1 源码与 1.21.1 NeoForge 移植版规则一致；`HeadShotAABB` 默认是空列表，
  因此默认全部走「眼睛高度 ±0.25 格」这条带。
- **结论**：头部模式瞄准「脚底 + 眼睛高度」，正好是判定带的正中心，上下各留 0.25 格余量。
  瞄准碰撞箱顶部反而会出界（例：末影人高 2.9 / 眼 2.55，箱顶 2.9 在带外）。
- 子弹出生点在 `eyeY - 0.1`，所以实际命中点比瞄准点低约 0.1 格，仍在带内。

### 10.2 实现

- `LockPart`：`CENTER`（身体中心）/ `HEAD`（头部）两个模式，刻意不引用 Minecraft 类。
- `AimController#aimHeightOffset`：`CENTER` → `bbHeight / 2`；`HEAD` → `eyeHeight + headOffset`，
  并把结果收进 `[0.05, bbHeight - 0.05]`，避免正微调时瞄准点跑到箱顶之外、子弹擦着头顶飞过。
- `TargetManager#aimPointOf`：把上述高度换算成世界坐标，自动瞄准与准星锁定选取共用同一点。
- 配置项 `lockPart`（默认 `CENTER`）/ `headOffset`（默认 `0.0`），配置界面两行，`J` 键快捷切换并提示。
- 旧配置兼容：不切换时行为与 1.0.0 完全一致。
- 头部瞄准点只是坐标运算，不调用 TaCZ 的任何 API，所以对其他枪械模组、对原版弓/弩同样生效。

### 10.3 游戏内验证要点（由用户实测）

1. 进入游戏按 `O` 打开配置界面，把「锁定部位」切到「头部」（或直接按 `J`，准星下方会提示当前模式）。
2. 按 `K` 锁定一只僵尸/骷髅，确认准心落在**头部**而不是腰部。
3. 开枪，看命中反馈：TaCZ 的爆头准心/音效与普通命中不同，且伤害明显更高。
4. 想直接看判定带：TaCZ 客户端配置 `config/tacz-client.toml` 里把 `HeadShotDebugHitbox` 改为 `true`，
   再按 `F3 + B` 打开碰撞箱显示，生物头部会出现一个黄框——那个框就是爆头判定带。
5. 再按 `J` 切回「身体中心」，行为应与 1.0.0 一致（瞄腰）。
