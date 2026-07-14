package com.prizowo.rideeverything.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "rideeverything", value = Dist.CLIENT)
public class KeyBindings {

    public static final KeyMapping KEY_DESCEND = new KeyMapping(
            "key.rideeverything.descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            com.prizowo.rideeverything.init.KeyBindings.CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_DESCEND);
    }
}
