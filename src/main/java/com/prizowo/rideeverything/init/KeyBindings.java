package com.prizowo.rideeverything.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
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
