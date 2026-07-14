package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CreateBlockSeatPacket(BlockPos pos) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "create_block_seat");
    public static final Type<CreateBlockSeatPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, CreateBlockSeatPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBlockPos(packet.pos()),
            buf -> new CreateBlockSeatPacket(buf.readBlockPos())
    );

    public static void handle(CreateBlockSeatPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                BlockPos pos = packet.pos();

                if (!FlyingEntityConfig.isBlockRidingAllowed()) {
                    return;
                }

                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    return;
                }

                if (FlyingEntityConfig.isBlockBlacklisted(state)) {
                    return;
                }

                if (hasSeatEntityAt(level, pos)) {
                    return;
                }

                if (!isEnoughSpaceAbove(level, pos)) {
                    return;
                }

                BlockSeatEntity seat = new BlockSeatEntity(ModEntities.BLOCK_SEAT.get(), level);

                double entityY = pos.getY() + 1.0;
                if (state.getBlock() instanceof StairBlock) {
                    entityY = state.getValue(StairBlock.HALF) == Half.TOP ?
                            pos.getY() + 1.0 : pos.getY() + 0.5;
                } else if (state.getBlock() instanceof SlabBlock) {
                    SlabType slabType = state.getValue(SlabBlock.TYPE);
                    entityY = switch (slabType) {
                        case TOP, DOUBLE -> pos.getY() + 1.0;
                        case BOTTOM -> pos.getY() + 0.5;
                    };
                }

                seat.setPos(pos.getX() + 0.5, entityY, pos.getZ() + 0.5);
                seat.setAttachedBlock(pos);

                if (level.addFreshEntity(seat)) {
                    player.startRiding(seat, true);
                }
            }
        });
    }

    private static boolean hasSeatEntityAt(ServerLevel level, BlockPos pos) {
        List<Entity> entities = level.getEntities(null, new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
        ));

        for (Entity entity : entities) {
            if (entity instanceof BlockSeatEntity) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEnoughSpaceAbove(ServerLevel level, BlockPos pos) {
        BlockPos abovePos = pos.above();

        List<Entity> entities = level.getEntities(null, new AABB(
                abovePos.getX(), abovePos.getY(), abovePos.getZ(),
                abovePos.getX() + 1, abovePos.getY() + 1, abovePos.getZ() + 1
        ));

        if (!entities.isEmpty()) {
            return false;
        }

        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.isAir() || !aboveState.canOcclude();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
