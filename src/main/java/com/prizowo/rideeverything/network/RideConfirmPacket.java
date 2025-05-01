package com.prizowo.rideeverything.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RideConfirmPacket(boolean isBlockSeat, int riderId, int targetId, boolean mounting, BlockPos blockPos) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "ride_confirm");
    public static final Type<RideConfirmPacket> TYPE = new Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, RideConfirmPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.isBlockSeat());
                
                if (packet.isBlockSeat()) {
                    buf.writeBlockPos(packet.blockPos());
                } else {
                    buf.writeInt(packet.riderId());
                    buf.writeInt(packet.targetId());
                    buf.writeBoolean(packet.mounting());
                }
            },
            buf -> {
                boolean isBlockSeat = buf.readBoolean();
                
                if (isBlockSeat) {
                    BlockPos pos = buf.readBlockPos();
                    return new RideConfirmPacket(true, -1, -1, true, pos);
                } else {
                    int riderId = buf.readInt();
                    int targetId = buf.readInt();
                    boolean mounting = buf.readBoolean();
                    return new RideConfirmPacket(false, riderId, targetId, mounting, null);
                }
            }
    );
    
    public RideConfirmPacket(int riderId, int targetId, boolean mounting) {
        this(false, riderId, targetId, mounting, null);
    }
    
    public RideConfirmPacket(BlockPos blockPos) {
        this(true, -1, -1, true, blockPos);
    }
    
    public static void handle(RideConfirmPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 客户端处理乘坐确认
            if (Minecraft.getInstance().level != null) {
                if (packet.isBlockSeat()) {
                    // 方块座位情况的处理，如有必要可添加特效或音效
                } else {
                    // 实体乘坐情况的处理
                    int riderId = packet.riderId();
                    int targetId = packet.targetId();
                    boolean mounting = packet.mounting();
                    
                    Entity rider = Minecraft.getInstance().level.getEntity(riderId);
                    Entity target = Minecraft.getInstance().level.getEntity(targetId);
                    
                    if (rider != null && target != null) {
                        if (mounting) {
                            if (!rider.isPassenger() || rider.getVehicle() != target) {
                                rider.startRiding(target, true);
                            }
                        } else {
                            if (rider.isPassenger() && rider.getVehicle() == target) {
                                rider.stopRiding();
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
