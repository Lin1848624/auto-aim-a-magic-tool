package com.autoaim.client;

import com.autoaim.AutoAimMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** MOD 总线订阅：按键映射注册（RegisterKeyMappingsEvent 走 mod 总线）。 */
@Mod.EventBusSubscriber(modid = AutoAimMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBindings.registerAll(event);
    }
}
