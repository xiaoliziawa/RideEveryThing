package com.prizowo.rideeverything;

import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("rideeverything")
public class Rideeverything {
    public Rideeverything() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(modBus);
        NetworkHandler.init();
        
        MinecraftForge.EVENT_BUS.register(this);
    }
}
