package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import com.prizowo.rideeverything.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class RidePacket {
    private final BlockPos pos;
    private final int entityId;
    private final boolean isMount;
    private final boolean isEntityInteraction;

    public RidePacket(BlockPos pos) {
        this.pos = pos;
        this.entityId = -1;
        this.isMount = false;
        this.isEntityInteraction = false;
    }

    public RidePacket(int entityId, boolean isMount) {
        this.entityId = entityId;
        this.isMount = isMount;
        this.pos = null;
        this.isEntityInteraction = true;
    }

    public static void encode(RidePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isEntityInteraction);
        
        if (msg.isEntityInteraction) {
            buf.writeInt(msg.entityId);
            buf.writeBoolean(msg.isMount);
        } else {
            buf.writeBlockPos(msg.pos);
        }
    }

    public static RidePacket decode(FriendlyByteBuf buf) {
        boolean isEntityInteraction = buf.readBoolean();
        
        if (isEntityInteraction) {
            int entityId = buf.readInt();
            boolean isMount = buf.readBoolean();
            return new RidePacket(entityId, isMount);
        } else {
            BlockPos pos = buf.readBlockPos();
            return new RidePacket(pos);
        }
    }

    public static void handle(RidePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();
                
                if (msg.isEntityInteraction) {
                    Entity target = level.getEntity(msg.entityId);
                    if (target != null) {
                        if (msg.isMount) {
                            boolean success = player.startRiding(target, true);
                            if (success) {
                                NetworkHandler.INSTANCE.send(
                                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new RideConfirmPacket(player.getId(), msg.entityId, true)
                                );
                            }
                        } else {
                            if (player.isPassenger()) {
                                player.stopRiding();
                                NetworkHandler.INSTANCE.send(
                                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                                    new RideConfirmPacket(player.getId(), msg.entityId, false)
                                );
                            }
                        }
                    }
                } else {
                    if (hasSeatEntityAt(level, msg.pos)) {
                        return;
                    }
                    
                    if (!isEnoughSpaceAbove(level, msg.pos)) {
                        return;
                    }
                    
                    BlockState state = level.getBlockState(msg.pos);
                    createSeatAndRide(player, level, msg.pos, state);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean hasSeatEntityAt(Level level, BlockPos pos) {
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
    
    private static boolean isEnoughSpaceAbove(Level level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        
        List<Entity> entities = level.getEntities(null, new AABB(
                abovePos.getX(), abovePos.getY(), abovePos.getZ(),
                abovePos.getX() + 1, abovePos.getY() + 1, abovePos.getZ() + 1
        ));
        
        if (!entities.isEmpty()) {
            return false;
        }
        
        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.isAir() || aboveState.canOcclude() == false;
    }

    private static void createSeatAndRide(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        BlockSeatEntity seat = new BlockSeatEntity(ModEntities.BLOCK_SEAT.get(), level);
        seat.setPos(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        
        level.addFreshEntity(seat);
        player.startRiding(seat, true);
        
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RideConfirmPacket(pos));
    }
}
