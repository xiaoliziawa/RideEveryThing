package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.init.KeyBindings;
import com.prizowo.rideeverything.util.RideHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "rideeverything")
public class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (KeyBindings.RIDE_KEY.consumeClick()) {
                RideHandler.handleRideKey();
            }
            
            if (KeyBindings.DISMOUNT_KEY.consumeClick()) {
                RideHandler.handleDismountKey();
            }
        }
    }
}
