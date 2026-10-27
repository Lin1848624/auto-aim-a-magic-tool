package com.autoaim;

import com.autoaim.client.LockPart;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = AutoAimMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue DISPLAY_RANGE = BUILDER
            .comment("ESP 显示范围（格）")
            .defineInRange("displayRange", 24, 8, 128);
    public static final ForgeConfigSpec.IntValue LOCK_RANGE = BUILDER
            .comment("锁定搜索范围（格）")
            .defineInRange("lockRange", 24, 8, 128);
    public static final ForgeConfigSpec.DoubleValue LOCK_ANGLE = BUILDER
            .comment("准星容差角（度）")
            .defineInRange("lockAngle", 15.0, 1.0, 60.0);
    public static final ForgeConfigSpec.DoubleValue AIM_SPEED = BUILDER
            .comment("自动瞄准速度（1.0 = 立即瞄准，越小越平滑）")
            .defineInRange("aimSpeed", 1.0, 0.02, 1.0);
    public static final ForgeConfigSpec.EnumValue<LockPart> LOCK_PART = BUILDER
            .comment("锁定部位：CENTER = 碰撞箱中心（腰部）；HEAD = 头部（眼睛高度）",
                    "枪械模组（如 TaCZ）的爆头判定默认取「离脚底高度落在眼睛高度 ±0.25 格内」，故 HEAD 模式瞄准眼睛高度")
            .defineEnum("lockPart", LockPart.CENTER);
    public static final ForgeConfigSpec.DoubleValue HEAD_OFFSET = BUILDER
            .comment("头部瞄准点的上下微调（格，正数 = 更靠上）",
                    "仅在锁定部位 = HEAD 时生效；结果会被收进碰撞箱内部，避免瞄到箱外导致子弹擦头飞过")
            .defineInRange("headOffset", 0.0, -0.3, 0.3);
    public static final ForgeConfigSpec.BooleanValue TARGET_HOSTILES_ONLY = BUILDER
            .comment("仅锁定敌对生物")
            .define("targetHostilesOnly", false);
    public static final ForgeConfigSpec.BooleanValue INCLUDE_PLAYERS = BUILDER
            .comment("是否将玩家作为目标")
            .define("includePlayers", false);
    public static final ForgeConfigSpec.BooleanValue SHOW_LOCKED_TARGET = BUILDER
            .comment("锁定目标始终高亮")
            .define("showLockedTarget", true);
    public static final ForgeConfigSpec.BooleanValue SHOW_TRAJECTORY = BUILDER
            .comment("显示弹道轨迹弧线")
            .define("showTrajectory", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int displayRange;
    public static int lockRange;
    public static double lockAngle;
    public static double aimSpeed;
    public static LockPart lockPart;
    public static double headOffset;
    public static boolean targetHostilesOnly;
    public static boolean includePlayers;
    public static boolean showLockedTarget;
    public static boolean showTrajectory;

    private Config() {}

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // ModConfigEvent 会对所有配置触发，这里只处理本模组的配置
        if (event.getConfig().getSpec() != SPEC) return;
        refresh();
    }

    public static void refresh() {
        displayRange = DISPLAY_RANGE.get();
        lockRange = LOCK_RANGE.get();
        lockAngle = LOCK_ANGLE.get();
        aimSpeed = AIM_SPEED.get();
        lockPart = LOCK_PART.get();
        headOffset = HEAD_OFFSET.get();
        targetHostilesOnly = TARGET_HOSTILES_ONLY.get();
        includePlayers = INCLUDE_PLAYERS.get();
        showLockedTarget = SHOW_LOCKED_TARGET.get();
        showTrajectory = SHOW_TRAJECTORY.get();
    }

    public static void save() {
        SPEC.save();
    }
}
