package com.prizowo.rideeverything.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RidePacket {
    private final int targetId;
    private final boolean mounting;

    public RidePacket(int targetId, boolean mounting) {
        this.targetId = targetId;
        this.mounting = mounting;
    }

    public static void encode(RidePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetId);
        buf.writeBoolean(msg.mounting);
    }

    public static RidePacket decode(FriendlyByteBuf buf) {
        return new RidePacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(RidePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() != null) {
                Entity target = player.level().getEntity(msg.targetId);
                if (target != null) {
                    if (msg.mounting) {
                        boolean success = player.startRiding(target, true);
                        if (success) {
                            NetworkHandler.INSTANCE.send(
                                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new RideConfirmPacket(player.getId(), msg.targetId, true)
                            );
                        }
                    } else {
                        if (player.isPassenger()) {
                            player.stopRiding();
                            NetworkHandler.INSTANCE.send(
                                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                new RideConfirmPacket(player.getId(), msg.targetId, false)
                            );
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
