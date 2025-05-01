package com.prizowo.rideeverything;

import com.prizowo.rideeverything.events.MountControlEvents;
import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.network.NetworkHandler;
import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("rideeverything")
public class Rideeverything {
    public Rideeverything(IEventBus eventBus, ModContainer modContainer) {
        ModEntities.ENTITIES.register(eventBus);
        
        eventBus.register(NetworkHandler.class);
        
        NeoForge.EVENT_BUS.register(MountControlEvents.class);
        
        FlyingEntityConfig.register(modContainer);
    }
}
