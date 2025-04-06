package com.prizowo.rideeverything.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("rideeverything", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(
            id++,
            RidePacket.class,
            RidePacket::encode,
            RidePacket::decode,
            RidePacket::handle
        );
        INSTANCE.registerMessage(
            id++,
            RideConfirmPacket.class,
            RideConfirmPacket::encode,
            RideConfirmPacket::decode,
            RideConfirmPacket::handle
        );
        INSTANCE.registerMessage(
            id++,
            CreateBlockSeatPacket.class,
            CreateBlockSeatPacket::encode,
            CreateBlockSeatPacket::decode,
            CreateBlockSeatPacket::handle
        );
        INSTANCE.registerMessage(
            id++,
            MountControlPacket.class,
            MountControlPacket::encode,
            MountControlPacket::decode,
            MountControlPacket::handle
        );
    }
}
