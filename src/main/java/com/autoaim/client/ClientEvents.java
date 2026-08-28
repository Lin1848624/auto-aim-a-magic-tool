package com.autoaim.client;

import com.autoaim.AutoAimMod;
import com.autoaim.Config;
import com.autoaim.client.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        AimController.V3 targetCenter = toV3(target.position().add(0, target.getBbHeight() * 0.5, 0));
        AimController.ProjectileSpecs specs = ProjectileSpecsProvider.specsFor(player.getMainHandItem(), player);

        AimController.LookAngles want;
        if (specs == null) {
            want = AimController.lookAt(eye, targetCenter);
        } else {
            Vec3 vel = target.getDeltaMovement();
            var opt = AimController.solveLaunch(eye, targetCenter, toV3(vel), specs);
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

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        EspRenderer.render(event);
    }

    static AimController.V3 toV3(Vec3 v) {
        return new AimController.V3(v.x, v.y, v.z);
    }
}
