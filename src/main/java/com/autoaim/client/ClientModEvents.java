package com.autoaim.client;

import com.autoaim.AutoAimMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** 按键映射注册（RegisterKeyMappingsEvent 实现 IModBusEvent，会自动注册到 mod 总线）。 */
@EventBusSubscriber(modid = AutoAimMod.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBindings.registerAll(event);
    }
}
