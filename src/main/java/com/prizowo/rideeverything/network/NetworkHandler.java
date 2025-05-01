package com.prizowo.rideeverything.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "rideeverything", bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    public static final NetworkHandler INSTANCE = new NetworkHandler();
    private static PayloadRegistrar REGISTRAR;
    
    private NetworkHandler() {
    }
    
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        REGISTRAR = event.registrar("rideeverything").versioned("1.0.0");

        REGISTRAR.playToServer(
                RidePacket.TYPE,
                RidePacket.STREAM_CODEC,
                RidePacket::handle
        );
        
        REGISTRAR.playToServer(
                CreateBlockSeatPacket.TYPE,
                CreateBlockSeatPacket.STREAM_CODEC,
                CreateBlockSeatPacket::handle
        );
        
        REGISTRAR.playToServer(
                MountControlPacket.TYPE,
                MountControlPacket.STREAM_CODEC,
                MountControlPacket::handle
        );
        
        REGISTRAR.playToClient(
                RideConfirmPacket.TYPE, 
                RideConfirmPacket.STREAM_CODEC,
                RideConfirmPacket::handle
        );
    }
    
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
