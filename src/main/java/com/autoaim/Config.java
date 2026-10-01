package com.autoaim;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CLIENT 配置定义。
 * <p>
 * 1.21.1 用 {@link ModConfigSpec} 取代了 Forge 的 ForgeConfigSpec，
 * 配置事件改到 net.neoforged.fml.event.config。静态字段是运行时读取的缓存，加载/重载时刷新。
 */
// 1.21.1 会自动根据事件类型（IModBusEvent）选择 mod 总线，无需再写 bus = Bus.MOD
@EventBusSubscriber(modid = AutoAimMod.MODID)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DISPLAY_RANGE = BUILDER
            .comment("ESP 显示范围（格）")
            .defineInRange("displayRange", 24, 8, 128);
    public static final ModConfigSpec.IntValue LOCK_RANGE = BUILDER
            .comment("锁定搜索范围（格）")
            .defineInRange("lockRange", 24, 8, 128);
    public static final ModConfigSpec.DoubleValue LOCK_ANGLE = BUILDER
            .comment("准星容差角（度）")
            .defineInRange("lockAngle", 15.0, 1.0, 60.0);
    public static final ModConfigSpec.DoubleValue AIM_SPEED = BUILDER
            .comment("自动瞄准速度（1.0 = 立即瞄准，越小越平滑）")
            .defineInRange("aimSpeed", 1.0, 0.02, 1.0);
    public static final ModConfigSpec.BooleanValue TARGET_HOSTILES_ONLY = BUILDER
            .comment("仅锁定敌对生物")
            .define("targetHostilesOnly", false);
    public static final ModConfigSpec.BooleanValue INCLUDE_PLAYERS = BUILDER
            .comment("是否将玩家作为目标")
            .define("includePlayers", false);
    public static final ModConfigSpec.BooleanValue SHOW_LOCKED_TARGET = BUILDER
            .comment("锁定目标始终高亮")
            .define("showLockedTarget", true);
    public static final ModConfigSpec.BooleanValue SHOW_TRAJECTORY = BUILDER
            .comment("显示弹道轨迹弧线")
            .define("showTrajectory", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int displayRange;
    public static int lockRange;
    public static double lockAngle;
    public static double aimSpeed;
    public static boolean targetHostilesOnly;
    public static boolean includePlayers;
    public static boolean showLockedTarget;
    public static boolean showTrajectory;

    private Config() {}

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // ModConfigEvent 会对所有配置触发，这里只处理本模组的配置
        if (event.getConfig().getSpec() != SPEC) return;
        refresh();
    }

    /** 把配置值同步到静态缓存字段（配置界面改动后也会调用）。 */
    public static void refresh() {
        displayRange = DISPLAY_RANGE.get();
        lockRange = LOCK_RANGE.get();
        lockAngle = LOCK_ANGLE.get();
        aimSpeed = AIM_SPEED.get();
        targetHostilesOnly = TARGET_HOSTILES_ONLY.get();
        includePlayers = INCLUDE_PLAYERS.get();
        showLockedTarget = SHOW_LOCKED_TARGET.get();
        showTrajectory = SHOW_TRAJECTORY.get();
    }

    /** 立即写盘（NeoForge 的 ModConfigSpec#save）。 */
    public static void save() {
        SPEC.save();
    }
}
