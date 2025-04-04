package com.prizowo.rideeverything;

import com.prizowo.rideeverything.events.MountControlEvents;
import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("rideeverything")
public class Rideeverything {
    public Rideeverything() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ModEntities.ENTITIES.register(eventBus);
        NetworkHandler.init();
        
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MountControlEvents());
    }
}
