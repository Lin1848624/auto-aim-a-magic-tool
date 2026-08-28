package com.autoaim.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 纯数学弹道计算。不依赖任何 Minecraft 类，可直接用 JUnit 测试。
 */
public final class AimController {
    /**
     * 弹道求解最大飞行 tick 数（1 tick = 1/20 秒，60 tick = 3.0 秒）。
     * 用户要求该值不暴露为配置，内部固定 3.0 秒。
     */
    public static final int MAX_FLIGHT_TICKS = 60;

    /** 弹体参数：初速（格/tick）、重力（格/tick²）、空气阻力（每 tick 速度倍率）。求解按无阻力模型（drag=1.0）进行。 */
    public record ProjectileSpecs(double speed, double gravity, double drag) {}

    /** 轻量 3D 向量。 */
    public record V3(double x, double y, double z) {
        public V3 add(V3 o) { return new V3(x + o.x, y + o.y, z + o.z); }
        public V3 sub(V3 o) { return new V3(x - o.x, y - o.y, z - o.z); }
        public V3 mul(double s) { return new V3(x * s, y * s, z * s); }
        public double lengthSqr() { return x * x + y * y + z * z; }
        public double length() { return Math.sqrt(lengthSqr()); }
    }

    /** 视角角：yaw 水平旋转、pitch 俯仰，均角度制。 */
    public record LookAngles(float yaw, float pitch) {}

    private AimController() {}

    /** 角度差规范到 [-180, 180]。 */
    public static float wrapDegrees(float deg) {
        float w = deg % 360.0F;
        if (w > 180.0F) w -= 360.0F;
        if (w < -180.0F) w += 360.0F;
        return w;
    }

    /** 由 yaw/pitch 构造方向单位向量（与 MC Vec3.directionFromRotation 等价）。 */
    public static V3 directionFromRotation(float yaw, float pitch) {
        double p = Math.toRadians(pitch);
        double y = Math.toRadians(yaw);
        double cosP = Math.cos(p);
        return new V3(-cosP * Math.sin(y), -Math.sin(p), cosP * Math.cos(y));
    }

    /** 普通命中角：视线直接指向目标点。 */
    public static LookAngles lookAt(V3 eye, V3 target) {
        V3 d = target.sub(eye);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, horiz)));
        return new LookAngles(yaw, pitch);
    }

    /**
     * 弹道求解：求发射角使弹体飞行 t tick 后命中目标（目标按自身速度外推）。
     * 求解使用无阻力模型（drag=1.0）；阻力差异对弓/三叉戟的近距离影响很小。
     * 返回空表示目标在当前弹速/重力下不可达。
     */
    public static Optional<LookAngles> solveLaunch(V3 eye, V3 targetPos, V3 targetVel, ProjectileSpecs s) {
        double t = binarySearchFlightTime(eye, targetPos, targetVel, s);
        if (t <= 0) return Optional.empty();

        V3 targetAtT = targetPos.add(targetVel.mul(t));
        V3 r = targetAtT.sub(eye);
        double horiz = Math.sqrt(r.x * r.x + r.z * r.z);
        if (horiz < 1.0E-5) return Optional.empty();

        double vyTerm = r.y + 0.5 * s.gravity() * t * t; // 需 -v0*sin(pitch)*t 提供的高度（MC 中 pitch 为正 = 低头）
        double sinPitch = -vyTerm / (s.speed() * t);
        if (Math.abs(sinPitch) > 1.0) return Optional.empty();

        float yaw = (float) (Math.toDegrees(Math.atan2(r.z, r.x)) - 90.0);
        float pitch = (float) Math.toDegrees(Math.asin(sinPitch));
        return Optional.of(new LookAngles(yaw, pitch));
    }

    /**
     * 在 [1, MAX_FLIGHT_TICKS] 内求解使 q(t)=0 的最小正根。
     * q(t) = (v0·t)² − |r_h(t)|² − (r_y(t)+½g·t²)²；q>=0 表示 t 时刻弹体可及。
     * 先大步扫描找首次跨零区间，再二分精化。
     */
    private static double binarySearchFlightTime(V3 eye, V3 targetPos, V3 targetVel, ProjectileSpecs s) {
        double tMin = 1.0;
        double tMax = MAX_FLIGHT_TICKS;
        if (q(eye, targetPos, targetVel, s, tMax) < 0) return -1;

        final int SCAN = 128;
        double prevT = tMin;
        double prevQ = q(eye, targetPos, targetVel, s, prevT);
        for (int i = 1; i <= SCAN; i++) {
            double t = tMin + (tMax - tMin) * i / SCAN;
            double q = q(eye, targetPos, targetVel, s, t);
            if (q >= 0) {
                double lo = prevQ >= 0 ? tMin : prevT;
                double hi = t;
                for (int k = 0; k < 48; k++) {
                    double mid = (lo + hi) / 2.0;
                    if (q(eye, targetPos, targetVel, s, mid) >= 0) hi = mid; else lo = mid;
                }
                return (lo + hi) / 2.0;
            }
            prevT = t;
            prevQ = q;
        }
        return -1;
    }

    private static double q(V3 eye, V3 targetPos, V3 targetVel, ProjectileSpecs s, double t) {
        V3 r = targetPos.add(targetVel.mul(t)).sub(eye);
        double horizSqr = r.x * r.x + r.z * r.z;
        double vy = r.y + 0.5 * s.gravity() * t * t;
        return s.speed() * s.speed() * t * t - horizSqr - vy * vy;
    }

    /** 计算弹道轨迹点列（世界坐标）：从 eye 沿 (yaw,pitch) 以弹速逐 tick 模拟，含重力与阻力。 */
    public static List<V3> computeTrajectory(V3 eye, float yaw, float pitch, ProjectileSpecs s, int steps) {
        List<V3> points = new ArrayList<>();
        V3 vel = directionFromRotation(yaw, pitch).mul(s.speed());
        V3 pos = eye;
        points.add(pos);
        for (int i = 0; i < steps; i++) {
            pos = pos.add(vel);
            vel = new V3(vel.x * s.drag(), (vel.y - s.gravity()) * s.drag(), vel.z * s.drag());
            points.add(pos);
        }
        return points;
    }
}
