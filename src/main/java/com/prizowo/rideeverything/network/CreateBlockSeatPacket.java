package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record CreateBlockSeatPacket(BlockPos pos) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "create_block_seat");
    public static final Type<CreateBlockSeatPacket> TYPE = new Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, CreateBlockSeatPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBlockPos(packet.pos()),
            buf -> new CreateBlockSeatPacket(buf.readBlockPos())
    );
    
    public static void handle(CreateBlockSeatPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.level() != null) {
                if (player.level().getBlockState(packet.pos()).isAir()) {
                    return;
                }

                BlockSeatEntity seat = new BlockSeatEntity(ModEntities.BLOCK_SEAT.get(), player.level());
                seat.setPos(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5);
                seat.setAttachedBlock(packet.pos());

                player.level().addFreshEntity(seat);
                player.startRiding(seat, true);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
