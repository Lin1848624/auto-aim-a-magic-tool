# 自动瞄准模组（Auto-Aim）设计文档

日期：2026-08-26
目标环境：Minecraft NeoForge 1.21.1 / 21.1.252，纯客户端模组
（2026-10-01 由 Forge 1.20.1 适配到 NeoForge 1.21.1，详见第 10 节；1.20.1 版本见 git 历史）
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

## 2. 架构总览

纯客户端、零外部依赖。包结构 `com.autoaim`。

```
src/main/java/com/autoaim/
├── AutoAimMod.java            # @Mod(value="autoaim", dist=CLIENT)，注册配置与配置界面扩展点
├── Config.java                # ModConfigSpec（CLIENT）+ 缓存字段 + 重载监听
└── client/
    ├── KeyBindings.java       # 4 个按键注册
    ├── TargetManager.java     # 候选扫描/筛选/锁定状态机
    ├── AimController.java     # 纯数学：命中角、弹道求解、轨迹点列（可单测）
    ├── EspRenderer.java       # ESP 框线 + 弹道弧线渲染
    └── gui/ConfigScreen.java  # 自建简易配置界面
src/test/java/com/autoaim/client/AimControllerTest.java   # JUnit 单测
src/main/resources/
├── META-INF/neoforge.mods.toml # NeoForge 1.21.1 元数据（无 clientSideOnly，改用入口点 dist=CLIENT）
├── pack.mcmeta
└── assets/auto-aim/lang/en_us.json, zh_cn.json
```

### 必做修正
- `ExampleMod.MODID` 由 `examplemod` 修正为 `auto-aim`（与 `gradle.properties` 的 `mod_id` 一致，否则模组无法加载）。
- 删除 examplemod 示例代码（示例方块/物品/创造标签页/日志）。
- 包名改为 `com.autoaim`。
- 纯客户端：入口点用 `@Mod(dist = Dist.CLIENT)` 限定，专用服务器上不会构造主类（1.21.1 已移除 `clientSideOnly`/`displayTest`）。

## 3. 按键绑定（KeyMapping，可在游戏内按键设置修改）

| 按键 | 默认键 | 功能 |
|------|--------|------|
| 显示周围生物 | `H` | 切换 ESP 框线显示 |
| 锁定目标 | `K` | 锁定准星最近生物；再按一次取消 |
| 取消锁定 | `L` | 清除当前锁定 |
| 打开配置界面 | `O` | 弹出配置界面 |

按键分类名：`key.categories.autoaim`。

## 4. 目标扫描与锁定（TargetManager）

- **候选扫描**：客户端 tick 中（每 5 tick 节流），用 `level.getEntitiesOfClass(LivingEntity.class, AABB 范围)` 收集。
- **筛选**：存活、非玩家（除非 `includePlayers=true`）、非观战；可选 `targetHostilesOnly=true` 只取敌对生物。敌对判定启发式：`entity instanceof net.minecraft.world.entity.monster.Monster`（覆盖僵尸/骷髅/苦力怕/末影人/猪灵/凋灵等常见敌对生物；不含史莱姆、末影龙等，作为近似实现，后续可按需扩展）。
- **锁定规则（准星指向锁定）**：按 `K` →
  1. 对每个候选计算「玩家视线方向」与「指向该生物方向」的夹角，取最小夹角者；
  2. 若最小夹角 < `lockAngle`（默认 15°），锁定该生物；
  3. 否则锁定范围内距离最近的生物。
  - 再次按 `K` → 取消锁定（切换式）。
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
1. `specs = forItem(mainHand)`；为 null → `lookAt`；否则 → `solveLaunch`（`targetVel = target.getDeltaMovement()`）。
2. 计算 `yawDiff = wrapDegrees(targetYaw − player.getYRot())`、`pitchDiff`，`player.turn(yawDiff × aimSpeed, pitchDiff × aimSpeed)` 平滑旋转。
3. 同步 `player.yHeadRot` / `yBodyRot`。

## 6. 渲染（EspRenderer，RenderLevelStageEvent / AFTER_ENTITIES）

- **候选 ESP 框线**：显示开关开启时，对范围内每个候选生物画 `AABB` 框线（默认黄色），用 `LevelRenderer.drawLineBox(event.getPoseStack(), box, r, g, b, a)`，box 略外扩 0.02。
- **锁定目标框线**：红色高亮，**不受显示开关控制**，始终绘制（`showLockedTarget` 可关）。
- **弹道弧线**：主手为弹道武器且 `showTrajectory=true` 时，绘制 `computeTrajectory` 点列折线（青色，position-color），随视角俯仰/弓拉弓进度实时更新；不受「显示周围生物」开关控制。
- 事件提供的 `getPoseStack()` 已是相机空间，世界坐标直接可用。

## 7. 配置（Config.java + ConfigScreen）

配置文件自动生成到 `config/auto-aim-client.toml`（注释友好、可手改）。

| 配置项 | 类型 | 默认 | 范围 | 说明 |
|--------|------|------|------|------|
| displayRange | int | 24 | 8–128 | ESP 显示范围（格） |
| lockRange | int | 24 | 8–128 | 锁定搜索范围（格） |
| lockAngle | double | 15 | 1–60 | 准星容差角（度） |
| aimSpeed | double | 0.25 | 0.02–1.0 | 自动瞄准平滑速度 |
| targetHostilesOnly | bool | false | — | 仅锁定敌对生物 |
| includePlayers | bool | false | — | 是否把玩家作为目标 |
| showLockedTarget | bool | true | — | 锁定目标始终高亮 |
| showTrajectory | bool | true | — | 显示弹道弧线 |

（`maxFlightTime` 已按用户要求移出配置，内部固定 3.0 秒。）

- `ModConfigEvent.Reloading`/`Loading` → 刷新缓存静态字段，改配置即时生效。
- **配置界面**（`O` 键弹出）：简易 `Screen`，每行 = 名称 + 描述 + 当前值 + `-`/`+`（数字）或 `开/关`（布尔）按钮；底部「保存」「恢复默认」「关闭」。改动即时生效，关闭时 `config.save()` 写盘。
- 同时通过 `RegisterClientExtensionsEvent.registerConfigScreenHandler` 注册，使模组列表里的「配置」按钮也可打开同一界面。

## 8. 构建与测试

- 构建：`./gradlew build`（Java 21，见 gradle.properties 的 `org.gradle.java.home`），jar 产出到 `build/libs`。
- 构建插件：NeoForge 官方 ModDevGradle 2.0.148（`net.neoforged.moddev`）+ Gradle 8.14；首次构建会下载 MC 1.21.1/NeoForge 并运行 NeoForm 生成开发工件（约 3~7 分钟，之后走缓存）。
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

## 10. NeoForge 1.21.1 适配记录（2026-10-01）

### 10.1 构建体系

| 项目 | Forge 1.20.1（原） | NeoForge 1.21.1（现） |
|------|--------------------|------------------------|
| 构建插件 | ForgeGradle 6 `net.minecraftforge.gradle` | ModDevGradle 2.0.148 `net.neoforged.moddev` |
| Gradle | 8.7 | 8.14（腾讯云镜像分发） |
| Java | 17 | 21（`D:/App/zulu21.52.15-ca-jdk21.0.12-win_x64`） |
| 映射 | `mapping_channel=official` + `mapping_version` | ModDevGradle 内置官方映射，无需配置 |
| 元数据 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml`（`mandatory=true` → `type="required"`） |
| 资源包格式 | `pack_format` 15 | `pack_format` 34 |
| 运行配置 | runClient/runServer/runData/runGameTestServer | client/server/data（`.idea/runConfigurations` 下旧的 ForgeGradle 配置已删除，IDEA 重新同步 Gradle 后会生成新的） |
| 模组版本 | `1.0.0` | `1.1.0`（NeoForge 1.21.1 线单独编号，jar 名 `autoaim-1.1.0.jar`） |

### 10.2 源码改动

| 文件 | 改动要点 |
|------|----------|
| `AutoAimMod` | 包名 `net.minecraftforge.*` → `net.neoforged.*`；构造器改为注入 `ModContainer`（1.21.1 支持注入 IEventBus / ModContainer / FMLModContainer / Dist）；`@Mod(dist = Dist.CLIENT)` 取代 `clientSideOnly=true`，专用服务器不会构造主类；配置界面扩展点改用 `IConfigScreenFactory`，其方法签名为 `createScreen(ModContainer, Screen)` |
| `Config` | `ForgeConfigSpec` → `ModConfigSpec`（`net.neoforged.neoforge.common`）；事件改为 `net.neoforged.fml.event.config.ModConfigEvent`；新增 `event.getConfig().getSpec() != SPEC` 过滤，避免误刷新其他配置 |
| `ClientEvents` | `TickEvent.ClientTickEvent(Phase.END)` → `ClientTickEvent.Post`；渲染事件 → `net.neoforged.neoforge.client.event.RenderLevelStageEvent` |
| `Config` / `ClientModEvents` | `@EventBusSubscriber(bus = Bus.MOD)` 在 1.21.1 已弃用：框架会按事件类型（`IModBusEvent`）自动选择总线，因此去掉 `bus` 参数 |
| `EspRenderer` | `event.getPartialTick()` 由 `float` 变为 `DeltaTracker`，改为 `getGameTimeDeltaPartialTick(false)`；顶点缓冲 API 由 `Tesselator#getBuilder + begin + endVertex + end()` 改为 `Tesselator#begin + addVertex + setColor + buildOrThrow + BufferUploader#drawWithShader` |
| `ProjectileSpecsProvider` | `ItemStack#getUseDuration()` → `getUseDuration(player)`（1.20.5 起的变更） |
| `ConfigScreen` | `Screen#renderBackground(GuiGraphics)` 已移除：改由 `super.render(...)` 内部调用 `renderBackground(g, mx, my, partialTick)`；`ForgeConfigSpec.*` → `ModConfigSpec.*` |
| `AimController` / `TargetManager` / `KeyBindings` / lang | 逻辑无需改动，仅按键注册事件换包名 |

### 10.3 环境注意事项

- 本机访问 `maven.neoforged.net` 时 **IPv4 会被连接重置（Connection reset），只有 IPv6 可用**。已在用户级环境变量设置
  `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv6Addresses=true`，使 Gradle 守护进程、NeoForm Runtime 等所有 JVM 优先走 IPv6。
  缺少该变量时，首次构建会在解析 `net.neoforged:neoform-runtime` 时失败。撤销方式：删除该用户环境变量。
- Gradle 发行版改用腾讯云镜像（`services.gradle.org` 直连超时），见 `gradle/wrapper/gradle-wrapper.properties`。
- `~/.gradle/init.gradle` 会清空项目的插件仓库，因此 `settings.gradle` 里额外补了一次 NeoForged 仓库注册（否则离线/受限网络下插件解析可能失败）。

### 10.4 验证情况

- `gradlew build` 通过：编译无警告、JUnit `AimControllerTest` 8 个用例全部通过、产出 `build/libs/autoaim-1.1.0.jar`。
- jar 内 `META-INF/neoforge.mods.toml` 占位符全部展开正确（含中文描述，使用 `\uXXXX` 转义避免 properties 编码问题）、`pack.mcmeta` 为 `pack_format 34`。
- 游戏内实际运行（进入世界、按 K 锁定、ESP 轮廓、弹道弧线、配置界面与模组列表「配置」按钮）需由用户启动客户端实测。
