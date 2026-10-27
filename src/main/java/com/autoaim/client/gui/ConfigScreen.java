package com.autoaim.client.gui;

import com.autoaim.Config;
import com.autoaim.client.LockPart;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** 简易配置界面：数值行用输入框直接填写，布尔行用 开/关 按钮；改动即时生效，关闭写盘。 */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Component.literal("Auto-Aim 设置"));
        this.parent = parent;
        rows.add(new NumRow(Config.DISPLAY_RANGE, "显示范围（格）", 8, 128, 24));
        rows.add(new NumRow(Config.LOCK_RANGE, "锁定范围（格）", 8, 128, 24));
        rows.add(new NumRow(Config.LOCK_ANGLE, "准星容差角（度）", 1, 60, 15.0));
        rows.add(new NumRow(Config.AIM_SPEED, "瞄准速度（1=立即）", 0.02, 1.0, 1.0));
        rows.add(new LockPartRow(Config.LOCK_PART, "锁定部位", LockPart.CENTER));
        rows.add(new NumRow(Config.HEAD_OFFSET, "头部微调（正=上，格）", -0.3, 0.3, 0.0));
        rows.add(new BoolRow(Config.TARGET_HOSTILES_ONLY, "仅锁定敌对生物", false));
        rows.add(new BoolRow(Config.INCLUDE_PLAYERS, "包含玩家", false));
        rows.add(new BoolRow(Config.SHOW_LOCKED_TARGET, "锁定目标高亮", true));
        rows.add(new BoolRow(Config.SHOW_TRAJECTORY, "显示弹道轨迹", true));
    }

    public ConfigScreen() { this(null); }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        int right = this.width / 2 + 150;
        int yTop = 18;
        int bottomY = this.height - 26;
        // 动态行距：保证所有设置行 + 底部按钮在任何 GUI 缩放下都落在屏幕内
        // （rows.size()-1 个行间距；再留 22 像素给底部按钮行）
        int spacing = Math.max(14, Math.min(24, (bottomY - yTop - 22) / Math.max(1, rows.size() - 1)));
        int y = yTop;
        for (Row row : rows) {
            row.y = y;
            row.build(this, left, right);
            y += spacing;
        }
        // 自动保存：无「保存」按钮，改动即时写盘；仅保留 恢复默认 与 关闭
        this.addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            for (Row r : rows) r.reset();
            Config.refresh();
            Config.save();
        }).bounds(left, bottomY, 96, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose())
                .bounds(right - 96, bottomY, 96, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        for (Row r : rows) r.render(g, this.font, this.width / 2 - 138);
        super.render(g, mx, my, partialTick);
    }

    @Override
    public void onClose() {
        // 关闭时兜底：把输入框中尚未按 Enter 的内容应用并写盘
        for (Row r : rows) r.apply();
        Config.refresh();
        Config.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private abstract static class Row {
        final ForgeConfigSpec.ConfigValue<?> value;
        final String label;
        final Object defaultValue;
        int y;

        Row(ForgeConfigSpec.ConfigValue<?> value, String label, Object defaultValue) {
            this.value = value;
            this.label = label;
            this.defaultValue = defaultValue;
        }

        /** 构建本行控件。 */
        abstract void build(ConfigScreen screen, int left, int right);
        /** 把输入/选择应用到配置（未改动时可为空实现）。 */
        void apply() {}
        /** 恢复默认值并刷新控件显示。 */
        abstract void reset();

        void render(GuiGraphics g, Font font, int x) {
            g.drawString(font, label, x, y, 0xFFFFFF);
        }
    }

    /** 数值行：直接输入数字。 */
    private static class NumRow extends Row {
        final double min, max;
        EditBox box;

        NumRow(ForgeConfigSpec.ConfigValue<?> value, String label, double min, double max, Object defaultValue) {
            super(value, label, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void build(ConfigScreen screen, int left, int right) {
            int w = 88;
            int bx = (left + right) / 2 - w / 2;
            box = new EditBox(screen.font, bx, y, w, 20, Component.literal(label)) {
                @Override
                public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                        apply();
                        return true;
                    }
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            };
            box.setValue(textOfCurrent());
            // 允许负号：头部微调的范围是 -0.3 ~ +0.3
            box.setFilter(s -> s.matches("-?[0-9.]*"));
            screen.addRenderableWidget(box);
        }

        double currentValue() {
            return value instanceof ForgeConfigSpec.IntValue iv ? iv.get()
                    : ((ForgeConfigSpec.DoubleValue) value).get();
        }

        String textOfCurrent() {
            return value instanceof ForgeConfigSpec.IntValue iv ? String.valueOf(iv.get())
                    : String.valueOf(Math.round(((ForgeConfigSpec.DoubleValue) value).get() * 100.0) / 100.0);
        }

        /** 解析输入框内容并写入配置，非法输入则回显当前值。 */
        @Override
        void apply() {
            String s = box.getValue().trim();
            if (s.isEmpty()) { box.setValue(textOfCurrent()); return; }
            try {
                double nv = Math.max(min, Math.min(max, Double.parseDouble(s)));
                if (value instanceof ForgeConfigSpec.IntValue iv) iv.set((int) Math.round(nv));
                else if (value instanceof ForgeConfigSpec.DoubleValue dv) dv.set(Math.round(nv * 100.0) / 100.0);
                box.setValue(textOfCurrent());
                Config.refresh();
                Config.save();
            } catch (NumberFormatException ex) {
                box.setValue(textOfCurrent());
            }
        }

        @Override
        void reset() {
            if (value instanceof ForgeConfigSpec.IntValue iv) iv.set((Integer) defaultValue);
            else if (value instanceof ForgeConfigSpec.DoubleValue dv) dv.set(((Number) defaultValue).doubleValue());
            box.setValue(textOfCurrent());
        }
    }

    /** 枚举行：点按钮循环切换（如 身体中心 → 头部）。 */
    private static class LockPartRow extends Row {
        private final ForgeConfigSpec.EnumValue<LockPart> enumValue;
        private Button toggle;

        LockPartRow(ForgeConfigSpec.EnumValue<LockPart> value, String label, LockPart defaultValue) {
            super(value, label, defaultValue);
            this.enumValue = value;
        }

        @Override
        void build(ConfigScreen screen, int left, int right) {
            toggle = Button.builder(Component.translatable(current().translationKey()), b -> {
                enumValue.set(current().next());
                b.setMessage(Component.translatable(current().translationKey()));
                Config.refresh();
                Config.save();
            }).bounds(right - 72, y, 72, 20).build();
            screen.addRenderableWidget(toggle);
        }

        LockPart current() {
            return enumValue.get();
        }

        @Override
        void reset() {
            enumValue.set((LockPart) defaultValue);
            toggle.setMessage(Component.translatable(current().translationKey()));
        }
    }

    /** 布尔行：开/关 切换按钮。 */
    private static class BoolRow extends Row {
        Button toggle;

        BoolRow(ForgeConfigSpec.ConfigValue<?> value, String label, boolean defaultValue) {
            super(value, label, defaultValue);
        }

        @Override
        void build(ConfigScreen screen, int left, int right) {
            toggle = Button.builder(Component.literal(currentText()), b -> {
                ((ForgeConfigSpec.BooleanValue) value).set(!((ForgeConfigSpec.BooleanValue) value).get());
                b.setMessage(Component.literal(currentText()));
                Config.refresh();
                Config.save();
            }).bounds(right - 60, y, 60, 20).build();
            screen.addRenderableWidget(toggle);
        }

        @Override
        void reset() {
            ((ForgeConfigSpec.BooleanValue) value).set((Boolean) defaultValue);
            toggle.setMessage(Component.literal(currentText()));
        }

        String currentText() {
            return ((ForgeConfigSpec.BooleanValue) value).get() ? "开" : "关";
        }
    }
}
