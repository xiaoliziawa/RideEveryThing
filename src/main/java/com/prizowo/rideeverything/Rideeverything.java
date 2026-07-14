package com.prizowo.rideeverything;

import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("rideeverything")
public class Rideeverything {
    public Rideeverything(IEventBus eventBus, ModContainer modContainer) {
        ModEntities.ENTITIES.register(eventBus);

        FlyingEntityConfig.register(modContainer);
    }
}
