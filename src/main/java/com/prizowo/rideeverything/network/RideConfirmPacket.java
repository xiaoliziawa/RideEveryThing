package com.prizowo.rideeverything.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RideConfirmPacket {
    private final int riderId;
    private final int targetId;
    private final boolean mounting;
    private final BlockPos blockPos;
    private final boolean isBlockSeat;

    public RideConfirmPacket(int riderId, int targetId, boolean mounting) {
        this.riderId = riderId;
        this.targetId = targetId;
        this.mounting = mounting;
        this.blockPos = null;
        this.isBlockSeat = false;
    }
    
    public RideConfirmPacket(BlockPos blockPos) {
        this.riderId = -1;
        this.targetId = -1;
        this.mounting = true;
        this.blockPos = blockPos;
        this.isBlockSeat = true;
    }

    public static void encode(RideConfirmPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isBlockSeat);
        
        if (msg.isBlockSeat) {
            buf.writeBlockPos(msg.blockPos);
        } else {
            buf.writeInt(msg.riderId);
            buf.writeInt(msg.targetId);
            buf.writeBoolean(msg.mounting);
        }
    }

    public static RideConfirmPacket decode(FriendlyByteBuf buf) {
        boolean isBlockSeat = buf.readBoolean();
        
        if (isBlockSeat) {
            BlockPos pos = buf.readBlockPos();
            return new RideConfirmPacket(pos);
        } else {
            int riderId = buf.readInt();
            int targetId = buf.readInt();
            boolean mounting = buf.readBoolean();
            return new RideConfirmPacket(riderId, targetId, mounting);
        }
    }

    public static void handle(RideConfirmPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                if (msg.isBlockSeat) {
                } else {
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
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
