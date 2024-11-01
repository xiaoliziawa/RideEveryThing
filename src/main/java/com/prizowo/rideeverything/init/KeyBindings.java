package com.prizowo.rideeverything.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping RIDE_KEY = new KeyMapping(
            "key.rideeverything.ride",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.rideeverything"
    );
    
    public static final KeyMapping DISMOUNT_KEY = new KeyMapping(
            "key.rideeverything.dismount",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.categories.rideeverything"
    );

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RIDE_KEY);
        event.register(DISMOUNT_KEY);
    }
}
