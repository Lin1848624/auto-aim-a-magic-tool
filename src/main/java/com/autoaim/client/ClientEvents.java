package com.autoaim.client;

import com.autoaim.AutoAimMod;
import com.autoaim.Config;
import com.autoaim.client.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AutoAimMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        while (KeyBindings.TOGGLE_DISPLAY.consumeClick()) TargetManager.toggleDisplay();
        while (KeyBindings.LOCK.consumeClick()) TargetManager.toggleLock();
        while (KeyBindings.UNLOCK.consumeClick()) TargetManager.unlock();
        while (KeyBindings.TOGGLE_LOCK_PART.consumeClick()) toggleLockPart(mc);
        while (KeyBindings.OPEN_CONFIG.consumeClick()) mc.setScreen(new ConfigScreen());

        TargetManager.tick();

        if (mc.screen == null && !mc.isPaused() && !mc.player.isSpectator() && !mc.player.isDeadOrDying()) {
            aim(mc.player);
        }
    }

    private static void aim(LocalPlayer player) {
        Entity target = TargetManager.getLockedTarget();
        if (target == null || !target.isAlive()) return;

        AimController.V3 eye = toV3(player.getEyePosition(1.0F));
        // 瞄准点由「锁定部位」决定：身体中心（碰撞箱中部）或头部（眼睛高度 + 微调）
        AimController.V3 aimPoint = toV3(TargetManager.aimPointOf(target));
        AimController.ProjectileSpecs specs = ProjectileSpecsProvider.specsFor(player.getMainHandItem(), player);

        AimController.LookAngles want;
        if (specs == null) {
            want = AimController.lookAt(eye, aimPoint);
        } else {
            Vec3 vel = target.getDeltaMovement();
            var opt = AimController.solveLaunch(eye, aimPoint, toV3(vel), specs);
            if (opt.isEmpty()) return;
            want = opt.get();
        }

        float yawDiff = AimController.wrapDegrees(want.yaw() - player.getYRot());
        float pitchDiff = AimController.wrapDegrees(want.pitch() - player.getXRot());
        // 直接设置旋转，避免 Entity.turn() 内置的 0.15 系数导致瞄准过慢
        float speed = (float) Config.aimSpeed;
        player.setYRot(player.getYRot() + yawDiff * speed);
        player.setXRot(Mth.clamp(player.getXRot() + pitchDiff * speed, -90.0F, 90.0F));
        player.setYHeadRot(player.getYRot());
        player.yBodyRot = player.getYRot();
    }

    /**
     * J 键：在「身体中心 / 头部」之间切换锁定部位并立刻写盘，同时在准星下方提示当前模式。
     * 战斗中不必打开配置界面即可切换，打完爆头再切回身体中心。
     */
    private static void toggleLockPart(Minecraft mc) {
        // 配置尚未加载时缓存字段可能为 null，兜底按 CENTER 处理
        LockPart current = Config.lockPart == null ? LockPart.CENTER : Config.lockPart;
        LockPart next = current.next();
        Config.LOCK_PART.set(next);
        Config.refresh();
        Config.save();
        if (mc.player != null) {
            // true = actionbar（准星下方），不刷聊天栏
            mc.player.displayClientMessage(
                    Component.translatable("autoaim.lock_part.toast", Component.translatable(next.translationKey())), true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        EspRenderer.render(event);
    }

    static AimController.V3 toV3(Vec3 v) {
        return new AimController.V3(v.x, v.y, v.z);
    }
}
