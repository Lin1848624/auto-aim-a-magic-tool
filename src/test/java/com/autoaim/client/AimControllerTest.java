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
}
