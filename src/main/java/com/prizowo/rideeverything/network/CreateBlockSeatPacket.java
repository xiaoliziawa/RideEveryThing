package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
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
            if (player != null) {
                if (!FlyingEntityConfig.isBlockRidingAllowed()) {
                    return;
                }

                Level level = player.level();
                BlockState state = level.getBlockState(msg.pos);
                if (state.isAir()) {
                    return;
                }

                if (FlyingEntityConfig.isBlockBlacklisted(state)) {
                    return;
                }

                if (hasSeatEntityAt(level, msg.pos)) {
                    return;
                }

                if (!isEnoughSpaceAbove(level, msg.pos)) {
                    return;
                }

                BlockSeatEntity seat = new BlockSeatEntity(ModEntities.BLOCK_SEAT.get(), level);
                seat.setPos(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5);
                seat.setAttachedBlock(msg.pos);

                level.addFreshEntity(seat);

                player.startRiding(seat, true);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean hasSeatEntityAt(Level level, BlockPos pos) {
        List<Entity> entities = level.getEntities(null, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));

        for (Entity entity : entities) {
            if (entity instanceof BlockSeatEntity) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEnoughSpaceAbove(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();

        List<Entity> entities = level.getEntities(null, new AABB(abovePos.getX(), abovePos.getY(), abovePos.getZ(), abovePos.getX() + 1, abovePos.getY() + 1, abovePos.getZ() + 1));

        if (!entities.isEmpty()) {
            return false;
        }

        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.isAir() || !aboveState.canOcclude();
    }
}
