package com.autoaim.client;

import com.autoaim.client.AimController.LookAngles;
import com.autoaim.client.AimController.ProjectileSpecs;
import com.autoaim.client.AimController.V3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AimControllerTest {
    private static final double TOL = 0.2;

    /** 用与 AimController 相同物理（无阻力模型）模拟，返回弹体与（移动）目标的最短距离。单位全部为 tick。 */
    private static double simulateClosest(V3 eye, V3 targetPos, V3 targetVel, ProjectileSpecs s, LookAngles angles) {
        final double sub = 0.02; // 子步长（tick）
        V3 pos = eye;
        V3 vel = AimController.directionFromRotation(angles.yaw(), angles.pitch()).mul(s.speed());
        double best = Double.MAX_VALUE;
        for (double t = 0; t < 200; t += sub) {
            pos = pos.add(vel.mul(sub));
            vel = new V3(vel.x() * Math.pow(s.drag(), sub),
                    (vel.y() - s.gravity() * sub) * Math.pow(s.drag(), sub),
                    vel.z() * Math.pow(s.drag(), sub));
            V3 target = targetPos.add(targetVel.mul(t + sub));
            best = Math.min(best, pos.sub(target).length());
        }
        return best;
    }

    @Test
    void wrapDegrees_normalizesToSigned180() {
        assertEquals(-10.0F, AimController.wrapDegrees(350.0F), 0.001F);
        assertEquals(170.0F, AimController.wrapDegrees(-190.0F), 0.001F);
        assertEquals(45.0F, AimController.wrapDegrees(405.0F), 0.001F);
    }

    @Test
    void lookAt_southIsYawZero() {
        LookAngles a = AimController.lookAt(new V3(0, 0, 0), new V3(0, 0, 10));
        assertEquals(0.0F, a.yaw(), 0.01F);
        assertEquals(0.0F, a.pitch(), 0.01F);
    }

    @Test
    void lookAt_eastIsYawNegativeNinety() {
        LookAngles a = AimController.lookAt(new V3(0, 0, 0), new V3(10, 0, 0));
        assertEquals(-90.0F, a.yaw(), 0.01F);
    }

    @Test
    void lookAt_upIsNegativePitch() {
        LookAngles a = AimController.lookAt(new V3(0, 0, 0), new V3(0, 10, 0));
        assertEquals(-90.0F, a.pitch(), 0.01F);
    }

    @Test
    void solveLaunch_staticTargetIsHit() {
        ProjectileSpecs s = new ProjectileSpecs(3.0, 0.05, 1.0); // 求解按无阻力模型验证
        V3 eye = new V3(0, 1, 0);
        V3 target = new V3(0, 1, 10);
        Optional<LookAngles> opt = AimController.solveLaunch(eye, target, new V3(0, 0, 0), s);
        assertTrue(opt.isPresent(), "10 格平射目标应可解");
        double dist = simulateClosest(eye, target, new V3(0, 0, 0), s, opt.get());
        assertTrue(dist < TOL, "静态目标实际命中距离应 < " + TOL + "，实际 " + dist);
    }

    @Test
    void solveLaunch_leadsMovingTarget() {
        ProjectileSpecs s = new ProjectileSpecs(3.0, 0.05, 1.0);
        V3 eye = new V3(0, 1, 0);
        V3 target = new V3(0, 1, 10);
        V3 vel = new V3(0.4, 0, 0); // 目标向右横移（格/tick）
        Optional<LookAngles> opt = AimController.solveLaunch(eye, target, vel, s);
        assertTrue(opt.isPresent());
        // 有提前量：应朝 +X（东，yaw 更负）偏移
        assertTrue(opt.get().yaw() < 0, "移动目标应向左提前，yaw 为负");
        double dist = simulateClosest(eye, target, vel, s, opt.get());
        assertTrue(dist < TOL, "移动目标实际命中距离应 < " + TOL + "，实际 " + dist);
    }

    @Test
    void solveLaunch_returnsEmptyWhenTooFar() {
        ProjectileSpecs s = new ProjectileSpecs(3.0, 0.05, 1.0);
        V3 eye = new V3(0, 1, 0);
        V3 target = new V3(0, 1, 300); // 远超 60 tick × 3.0 格/tick = 180 格最大射程
        assertTrue(AimController.solveLaunch(eye, target, new V3(0, 0, 0), s).isEmpty());
    }

    @Test
    void computeTrajectory_hasExpectedLengthAndFalls() {
        ProjectileSpecs s = new ProjectileSpecs(3.0, 0.05, 0.99);
        List<V3> pts = AimController.computeTrajectory(new V3(0, 2, 0), 0.0F, 0.0F, s, 48);
        assertEquals(49, pts.size());
        assertTrue(pts.get(48).y() < pts.get(0).y(), "平射轨迹应随重力下降");
        assertTrue(pts.get(48).z() > 0, "平射轨迹应向前延伸");
    }

    // ---------- 锁定部位：身体中心 / 头部 ----------

    /** 以僵尸为例：碰撞箱高 1.95、眼睛高 1.74。 */
    private static final double ZOMBIE_HEIGHT = 1.95;
    private static final double ZOMBIE_EYE = 1.74;

    @Test
    void aimHeightOffset_centerIsHalfHitboxHeight() {
        assertEquals(ZOMBIE_HEIGHT * 0.5,
                AimController.aimHeightOffset(ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.CENTER, 0.0), 1.0E-9);
    }

    @Test
    void aimHeightOffset_centerIgnoresHeadOffset() {
        assertEquals(ZOMBIE_HEIGHT * 0.5,
                AimController.aimHeightOffset(ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.CENTER, 0.3), 1.0E-9);
    }

    @Test
    void aimHeightOffset_headIsEyeHeightPlusOffset() {
        assertEquals(ZOMBIE_EYE,
                AimController.aimHeightOffset(ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.HEAD, 0.0), 1.0E-9);
        assertEquals(ZOMBIE_EYE + 0.15,
                AimController.aimHeightOffset(ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.HEAD, 0.15), 1.0E-9);
        assertEquals(ZOMBIE_EYE - 0.15,
                AimController.aimHeightOffset(ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.HEAD, -0.15), 1.0E-9);
    }

    @Test
    void aimHeightOffset_headIsClampedInsideHitbox() {
        // 玩家：高 1.8、眼睛 1.62 → +0.3 会顶到箱外，收回到箱顶下方 0.05
        assertEquals(1.75, AimController.aimHeightOffset(1.8, 1.62, LockPart.HEAD, 0.3), 1.0E-9);
        // 负向微调也不能掉到脚底以下
        assertEquals(0.05, AimController.aimHeightOffset(1.8, 0.1, LockPart.HEAD, -0.3), 1.0E-9);
    }

    @Test
    void aimPoint_keepsHitboxCenterXZAndUsesHeadHeight() {
        V3 feet = new V3(10.5, 64.0, -3.5);
        V3 head = AimController.aimPoint(feet, ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.HEAD, 0.0);
        assertEquals(10.5, head.x(), 1.0E-9);
        assertEquals(-3.5, head.z(), 1.0E-9);
        assertEquals(65.74, head.y(), 1.0E-9);

        V3 center = AimController.aimPoint(feet, ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.CENTER, 0.0);
        assertEquals(64.975, center.y(), 1.0E-9);
    }

    // ---------- 端到端核对：命中点是否落在 TaCZ 的爆头判定带内 ----------

    /** TaCZ 的爆头判定半高（命中点离脚底高度相对眼睛高度的容差）。 */
    private static final double TACZ_HEAD_BAND = 0.25;

    /** 射线与 AABB 的首个交点（slab 法）；不相交返回 null。 */
    private static V3 clipRay(V3 start, V3 dir, double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ) {
        double[] s = {start.x(), start.y(), start.z()};
        double[] d = {dir.x(), dir.y(), dir.z()};
        double[] lo = {minX, minY, minZ};
        double[] hi = {maxX, maxY, maxZ};
        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-9) {
                if (s[i] < lo[i] || s[i] > hi[i]) return null;
                continue;
            }
            double t1 = (lo[i] - s[i]) / d[i];
            double t2 = (hi[i] - s[i]) / d[i];
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return null;
        }
        return new V3(s[0] + d[0] * tMin, s[1] + d[1] * tMin, s[2] + d[2] * tMin);
    }

    /** 20 格外的僵尸（碰撞箱 0.6 × 1.95），玩家眼睛高度 1.62。 */
    private static final V3 ZOMBIE_FEET = new V3(0.0, 64.0, 20.0);
    private static final V3 PLAYER_EYE = new V3(0.0, 65.62, 0.0);

    /** 以给定瞄准点向僵尸开枪，返回命中点相对脚底的高度。 */
    private static double hitHeightForAim(V3 aim, double muzzleDrop) {
        LookAngles a = AimController.lookAt(PLAYER_EYE, aim);
        V3 dir = AimController.directionFromRotation(a.yaw(), a.pitch());
        // 半径 0.3、高 1.95 的碰撞箱；从眼睛下方 muzzleDrop 处射出（TaCZ 为 0.1）
        V3 hit = clipRay(PLAYER_EYE.add(new V3(0.0, -muzzleDrop, 0.0)), dir,
                ZOMBIE_FEET.x() - 0.3, ZOMBIE_FEET.y(), ZOMBIE_FEET.z() - 0.3,
                ZOMBIE_FEET.x() + 0.3, ZOMBIE_FEET.y() + ZOMBIE_HEIGHT, ZOMBIE_FEET.z() + 0.3);
        assertNotNull(hit, "射线应命中碰撞箱");
        return hit.y() - ZOMBIE_FEET.y();
    }

    /** 头部模式：无论是否计入 TaCZ 子弹的 0.1 格下移，命中点都应落在眼睛高度 ±0.25 内。 */
    @Test
    void headAim_landsInsideTaczHeadshotBand() {
        V3 aim = AimController.aimPoint(ZOMBIE_FEET, ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.HEAD, 0.0);

        double ideal = hitHeightForAim(aim, 0.0);
        assertTrue(Math.abs(ideal - ZOMBIE_EYE) <= TACZ_HEAD_BAND,
                "理想射线命中高度 " + ideal + " 应落在眼睛高度 ±" + TACZ_HEAD_BAND + " 内");

        double withMuzzleOffset = hitHeightForAim(aim, 0.1);
        assertTrue(Math.abs(withMuzzleOffset - ZOMBIE_EYE) <= TACZ_HEAD_BAND,
                "计入子弹 0.1 格下移后命中高度 " + withMuzzleOffset + " 仍应落在判定带内");
    }

    /** 身体中心模式：命中点落在爆头判定带之外（说明这个模式本来就不是为爆头准备的）。 */
    @Test
    void centerAim_isOutsideTaczHeadshotBand() {
        V3 aim = AimController.aimPoint(ZOMBIE_FEET, ZOMBIE_HEIGHT, ZOMBIE_EYE, LockPart.CENTER, 0.0);
        double h = hitHeightForAim(aim, 0.0);
        assertTrue(Math.abs(h - ZOMBIE_EYE) > TACZ_HEAD_BAND,
                "身体中心命中高度 " + h + " 应落在眼睛高度 ±" + TACZ_HEAD_BAND + " 之外");
    }
}
