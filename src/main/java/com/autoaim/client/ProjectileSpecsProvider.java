package com.autoaim.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;

/** 主手物品 → 弹道参数；非弹道武器返回 null（走普通命中角模式）。 */
public final class ProjectileSpecsProvider {
    private ProjectileSpecsProvider() {}

    public static AimController.ProjectileSpecs specsFor(ItemStack stack, Player player) {
        Item item = stack.getItem();
        if (item instanceof BowItem) {
            // 满弓 3.0；拉弓中按当前进度实时换算（与原版 BowItem.getPowerForTime 一致）
            double power = 1.0;
            if (player.isUsingItem() && player.getUseItem() == stack) {
                int useTicks = stack.getUseDuration() - player.getUseItemRemainingTicks();
                power = BowItem.getPowerForTime(useTicks);
            }
            return new AimController.ProjectileSpecs(power * 3.0, 0.05, 0.99);
        }
        if (item instanceof CrossbowItem) {
            return new AimController.ProjectileSpecs(3.15, 0.05, 0.99);
        }
        if (item instanceof TridentItem) {
            return new AimController.ProjectileSpecs(2.5, 0.05, 0.99);
        }
        if (item instanceof SnowballItem || item instanceof EggItem
                || item instanceof EnderpearlItem || item instanceof ExperienceBottleItem) {
            return new AimController.ProjectileSpecs(1.5, 0.03, 1.0);
        }
        if (item instanceof PotionItem) {
            return new AimController.ProjectileSpecs(0.5, 0.05, 1.0);
        }
        return null;
    }
}
