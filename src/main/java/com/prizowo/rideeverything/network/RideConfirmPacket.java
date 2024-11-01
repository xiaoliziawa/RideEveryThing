package com.prizowo.rideeverything.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RideConfirmPacket {
    private final int riderId;
    private final int targetId;
    private final boolean mounting;

    public RideConfirmPacket(int riderId, int targetId, boolean mounting) {
        this.riderId = riderId;
        this.targetId = targetId;
        this.mounting = mounting;
    }

    public static void encode(RideConfirmPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.riderId);
        buf.writeInt(msg.targetId);
        buf.writeBoolean(msg.mounting);
    }

    public static RideConfirmPacket decode(FriendlyByteBuf buf) {
        return new RideConfirmPacket(buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(RideConfirmPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity rider = mc.level.getEntity(msg.riderId);
                Entity target = mc.level.getEntity(msg.targetId);
                if (rider != null && target != null) {
                    if (msg.mounting) {
                        rider.startRiding(target, true);
                    } else {
                        rider.stopRiding();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
