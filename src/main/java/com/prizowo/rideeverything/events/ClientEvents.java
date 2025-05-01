package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.init.KeyBindings;
import com.prizowo.rideeverything.util.RideHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = "rideeverything")
public class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (KeyBindings.RIDE_KEY.consumeClick()) {
            RideHandler.handleRideKey();
        }
        
        if (KeyBindings.DISMOUNT_KEY.consumeClick()) {
            RideHandler.handleDismountKey();
        }
    }
}
