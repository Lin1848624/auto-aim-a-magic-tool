package com.autoaim;

import com.mojang.logging.LogUtils;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AutoAimMod.MODID)
public class AutoAimMod {
    public static final String MODID = "autoaim";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AutoAimMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modEventBus.register(Config.class);
        // 让模组列表里的「配置」按钮也打开同一配置界面（1.20.1：ModLoadingContext + ConfigScreenFactory）
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new com.autoaim.client.gui.ConfigScreen(parent)));
    }
}
