package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
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
                
                BlockState state = player.level().getBlockState(packet.pos());
                double entityY = packet.pos().getY() + 1.0;
                
                if (state.getBlock() instanceof StairBlock) {
                    entityY = state.getValue(StairBlock.HALF) == Half.TOP ?
                        packet.pos().getY() + 1.0 : packet.pos().getY() + 0.5;
                } else if (state.getBlock() instanceof SlabBlock) {
                    SlabType slabType = state.getValue(SlabBlock.TYPE);
                    entityY = switch (slabType) {
                        case TOP -> packet.pos().getY() + 1.0;
                        case BOTTOM -> packet.pos().getY() + 0.5;
                        case DOUBLE -> packet.pos().getY() + 1.0;
                    };
                }
                
                seat.setPos(packet.pos().getX() + 0.5, entityY, packet.pos().getZ() + 0.5);
                seat.setAttachedBlock(packet.pos());

                if (player.level().addFreshEntity(seat)) {
                    player.startRiding(seat, true);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
