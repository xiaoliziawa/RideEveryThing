package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateBlockSeatPacket {
    private final BlockPos pos;

    public CreateBlockSeatPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CreateBlockSeatPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static CreateBlockSeatPacket decode(FriendlyByteBuf buf) {
        return new CreateBlockSeatPacket(buf.readBlockPos());
    }

    public static void handle(CreateBlockSeatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() != null) {
                if (player.level().getBlockState(msg.pos).isAir()) {
                    return;
                }

                BlockSeatEntity seat = new BlockSeatEntity(ModEntities.BLOCK_SEAT.get(), player.level());
                seat.setPos(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5);
                seat.setAttachedBlock(msg.pos);
                
                player.level().addFreshEntity(seat);
                
                player.startRiding(seat, true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
