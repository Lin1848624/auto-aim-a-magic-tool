package com.autoaim.client;

import com.autoaim.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 候选扫描、目标筛选与锁定状态机。纯客户端。 */
public final class TargetManager {
    private static boolean displayEnabled = false;
    private static Entity lockedTarget = null;
    private static long lastScanTick = -1;
    private static List<LivingEntity> candidates = new ArrayList<>();

    private TargetManager() {}

    public static boolean isDisplayEnabled() { return displayEnabled; }
    public static void setDisplayEnabled(boolean b) { displayEnabled = b; }
    public static void toggleDisplay() {
        displayEnabled = !displayEnabled;
    }
    public static Entity getLockedTarget() { return lockedTarget; }
    public static List<LivingEntity> getCandidates() { return candidates; }

    /**
     * 当前锁定部位对应的瞄准点（世界坐标）：x/z 取碰撞箱中心，y 由 {@link AimController#aimHeightOffset} 决定。
     * 自动瞄准与准星锁定选取共用同一点，保证「瞄到的」就是「锁到的」。
     */
    public static Vec3 aimPointOf(Entity e) {
        return new Vec3(e.getX(),
                e.getY() + AimController.aimHeightOffset(e.getBbHeight(), e.getEyeHeight(),
                        Config.lockPart, Config.headOffset),
                e.getZ());
    }

    public static void toggleLock() {
        if (lockedTarget == null) lock(); else unlock();
    }

    public static void unlock() { lockedTarget = null; }

    private static void lock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        LivingEntity picked = pickCrosshair(mc.player, candidates);
        if (picked == null) picked = pickNearest(mc.player, candidates);
        lockedTarget = picked;
    }

    /** 每 tick 调用：每 5 tick 刷新候选；锁定目标失效/超距/玩家死亡时自动解锁。 */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = mc.level.getGameTime();
        if (now - lastScanTick >= 5) {
            lastScanTick = now;
            candidates = scan(mc);
        }
        if (lockedTarget != null) {
            Entity t = lockedTarget;
            boolean lost = !t.isAlive() || t.getRemovalReason() != null
                    || t.distanceToSqr(mc.player) > Config.lockRange * Config.lockRange * 2.25
                    || mc.player.isDeadOrDying();
            if (lost) unlock();
        }
    }

    private static List<LivingEntity> scan(Minecraft mc) {
        Player p = mc.player;
        double range = Math.max(Config.displayRange, Config.lockRange);
        AABB box = p.getBoundingBox().inflate(range);
        return mc.level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive()
                        && e != p
                        && (!(e instanceof Player) || Config.includePlayers)
                        && e.distanceToSqr(p) <= range * range
                        && (!Config.targetHostilesOnly || e instanceof Monster));
    }

    /** 准星指向锁定：取视角方向与目标瞄准点方向夹角最小者，且夹角 <= lockAngle。 */
    private static LivingEntity pickCrosshair(Player p, List<LivingEntity> list) {
        Vec3 eye = p.getEyePosition(1.0F);
        Vec3 view = p.getViewVector(1.0F);
        LivingEntity best = null;
        double bestAngle = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            if (e.distanceToSqr(p) > Config.lockRange * Config.lockRange) continue;
            Vec3 to = aimPointOf(e).subtract(eye);
            double len = to.length();
            if (len < 1.0E-4) continue;
            double dot = view.dot(to) / len;
            double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            if (angleDeg < bestAngle) { bestAngle = angleDeg; best = e; }
        }
        return bestAngle <= Config.lockAngle ? best : null;
    }

    /** 兜底：锁定范围内最近生物。 */
    private static LivingEntity pickNearest(Player p, List<LivingEntity> list) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = e.distanceToSqr(p);
            if (d <= Config.lockRange * Config.lockRange && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }
}
