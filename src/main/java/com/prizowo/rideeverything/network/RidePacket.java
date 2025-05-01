package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record RidePacket(boolean isEntityInteraction, int entityId, boolean isMount, BlockPos pos) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.parse("rideeverything:ride_packet");
    public static final Type<RidePacket> TYPE = new Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, RidePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.isEntityInteraction());
                
                if (packet.isEntityInteraction()) {
                    buf.writeInt(packet.entityId());
                    buf.writeBoolean(packet.isMount());
                } else {
                    buf.writeBlockPos(packet.pos());
                }
            },
            buf -> {
                boolean isEntityInteraction = buf.readBoolean();
                
                if (isEntityInteraction) {
                    int entityId = buf.readInt();
                    boolean isMount = buf.readBoolean();
                    return new RidePacket(true, entityId, isMount, null);
                } else {
                    BlockPos pos = buf.readBlockPos();
                    return new RidePacket(false, -1, false, pos);
                }
            }
    );
    
    public RidePacket(int entityId, boolean isMount) {
        this(true, entityId, isMount, null);
    }
    
    public RidePacket(BlockPos pos) {
        this(false, -1, false, pos);
    }
    
    public static void handle(RidePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                
                if (packet.isEntityInteraction()) {
                    Entity target = level.getEntity(packet.entityId());
                    if (target != null) {
                        if (packet.isMount()) {
                            boolean success = player.startRiding(target, true);
                            if (success) {
                                RideConfirmPacket confirmPacket = new RideConfirmPacket(player.getId(), packet.entityId(), true);
                                
                                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, confirmPacket);
                                
                                if (target instanceof ServerPlayer targetPlayer) {
                                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(targetPlayer, confirmPacket);
                                }
                            }
                        } else {
                            if (player.isPassenger()) {
                                Entity vehicle = player.getVehicle();
                                player.stopRiding();
                                
                                if (vehicle != null) {
                                    RideConfirmPacket confirmPacket = new RideConfirmPacket(player.getId(), vehicle.getId(), false);
                                    
                                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, confirmPacket);
                                    
                                    if (vehicle instanceof ServerPlayer vehiclePlayer) {
                                        PacketDistributor.sendToPlayersTrackingEntityAndSelf(vehiclePlayer, confirmPacket);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (hasSeatEntityAt(level, packet.pos())) {
                        return;
                    }
                    
                    if (!isEnoughSpaceAbove(level, packet.pos())) {
                        return;
                    }
                    
                    BlockState state = level.getBlockState(packet.pos());
                    if (state.isAir()) {
                        return;
                    }
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
