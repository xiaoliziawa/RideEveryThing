package com.prizowo.rideeverything.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "rideeverything")
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("rideeverything").versioned("3.0.0");

        registrar.playToServer(
                RidePacket.TYPE,
                RidePacket.STREAM_CODEC,
                RidePacket::handle
        );

        registrar.playToServer(
                CreateBlockSeatPacket.TYPE,
                CreateBlockSeatPacket.STREAM_CODEC,
                CreateBlockSeatPacket::handle
        );

        registrar.playToServer(
                MountControlPacket.TYPE,
                MountControlPacket.STREAM_CODEC,
                MountControlPacket::handle
        );

        registrar.playToServer(
                MountJumpPacket.TYPE,
                MountJumpPacket.STREAM_CODEC,
                MountJumpPacket::handle
        );

        registrar.playToClient(
                RideConfirmPacket.TYPE,
                RideConfirmPacket.STREAM_CODEC,
                RideConfirmPacket::handle
        );
    }
}
