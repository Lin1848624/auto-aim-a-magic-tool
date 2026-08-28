package com.autoaim.client;

import com.autoaim.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 世界渲染：
 * - 候选/锁定实体用原版光灵箭式「发光轮廓」高亮（穿墙、动画描边）；
 * - 弹道武器显示预测轨迹弧线。
 */
public final class EspRenderer {
    private EspRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // ---- 发光轮廓高亮（仿光灵箭效果） ----
        OutlineBufferSource outline = mc.renderBuffers().outlineBufferSource();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        if (TargetManager.isDisplayEnabled()) {
            double range = Config.displayRange;
            for (LivingEntity e : TargetManager.getCandidates()) {
                if (!e.isAlive() || e.distanceToSqr(mc.player) > range * range) continue;
                outline.setColor(255, 255, 0, 255); // 候选：黄色发光轮廓
                renderOutline(dispatcher, poseStack, e, camPos, outline, partialTick);
            }
        }
        if (TargetManager.getLockedTarget() instanceof LivingEntity locked && Config.showLockedTarget) {
            outline.setColor(255, 0, 0, 255); // 锁定目标：红色发光轮廓
            renderOutline(dispatcher, poseStack, locked, camPos, outline, partialTick);
        }
        outline.endOutlineBatch();

        // ---- 弹道弧线 ----
        if (Config.showTrajectory) {
            AimController.ProjectileSpecs specs = ProjectileSpecsProvider.specsFor(mc.player.getMainHandItem(), mc.player);
            if (specs != null) {
                AimController.V3 eye = toV3(mc.player.getEyePosition(1.0F));
                List<AimController.V3> pts = AimController.computeTrajectory(
                        eye, mc.player.getYRot(), mc.player.getXRot(), specs, 48);
                drawPolyline(event.getPoseStack().last().pose(), pts, 0.0F, 1.0F, 1.0F, 0.9F);
            }
        }
    }

    /** 把实体模型渲染进描边缓冲，形成光灵箭式发光轮廓。 */
    private static void renderOutline(EntityRenderDispatcher dispatcher, PoseStack poseStack,
                                      LivingEntity entity, Vec3 camPos, OutlineBufferSource outline, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(entity.getX() - camPos.x, entity.getY() - camPos.y, entity.getZ() - camPos.z);
        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity> renderer = (EntityRenderer<LivingEntity>) dispatcher.getRenderer(entity);
        renderer.render(entity, entity.getYRot(), partialTick, poseStack, outline, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void drawPolyline(Matrix4f mat, List<AimController.V3> pts, float r, float g, float b, float a) {
        if (pts.size() < 2) return;
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < pts.size() - 1; i++) {
            AimController.V3 p = pts.get(i);
            AimController.V3 q = pts.get(i + 1);
            builder.vertex(mat, (float) p.x(), (float) p.y(), (float) p.z()).color(r, g, b, a).endVertex();
            builder.vertex(mat, (float) q.x(), (float) q.y(), (float) q.z()).color(r, g, b, a).endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.enableDepthTest();
    }

    private static AimController.V3 toV3(Vec3 v) {
        return new AimController.V3(v.x, v.y, v.z);
    }
}
