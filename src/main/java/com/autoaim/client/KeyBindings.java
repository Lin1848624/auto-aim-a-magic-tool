package com.autoaim.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.autoaim";

    public static final KeyMapping TOGGLE_DISPLAY = new KeyMapping("key.autoaim.toggle_display", InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping LOCK = new KeyMapping("key.autoaim.lock", InputConstants.KEY_K, CATEGORY);
    public static final KeyMapping UNLOCK = new KeyMapping("key.autoaim.unlock", InputConstants.KEY_L, CATEGORY);
    public static final KeyMapping TOGGLE_LOCK_PART = new KeyMapping("key.autoaim.toggle_lock_part", InputConstants.KEY_J, CATEGORY);
    public static final KeyMapping OPEN_CONFIG = new KeyMapping("key.autoaim.open_config", InputConstants.KEY_O, CATEGORY);

    private KeyBindings() {}

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DISPLAY);
        event.register(LOCK);
        event.register(UNLOCK);
        event.register(TOGGLE_LOCK_PART);
        event.register(OPEN_CONFIG);
    }
}
