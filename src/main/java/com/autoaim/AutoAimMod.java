package com.autoaim;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

/**
 * 模组主类（NeoForge 1.21.1）。
 * <p>
 * 模组为纯客户端：{@code dist = Dist.CLIENT} 表示入口点只在物理客户端构造，
 * 即使 jar 被放进专用服务器也不会因为引用客户端类而崩溃。
 */
@Mod(value = AutoAimMod.MODID, dist = Dist.CLIENT)
public class AutoAimMod {
    public static final String MODID = "autoaim";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoAimMod(ModContainer modContainer) {
        // CLIENT 类型配置：生成在 config/autoaim-client.toml
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        // 模组列表里的「配置」按钮也打开同一配置界面
        // 1.21.1 的扩展点方法签名为 IConfigScreenFactory#createScreen(ModContainer, Screen)
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new com.autoaim.client.gui.ConfigScreen(parent));
    }
}
