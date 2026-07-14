package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.util.FlyingEntityConfig;
import com.prizowo.rideeverything.util.ModRideTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public record RidePacket(int entityId, boolean isMount) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.parse("rideeverything:ride_packet");
    public static final Type<RidePacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RidePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.entityId());
                buf.writeBoolean(packet.isMount());
            },
            buf -> new RidePacket(buf.readInt(), buf.readBoolean())
    );

    public static void handle(RidePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();

                Entity target = level.getEntity(packet.entityId());
                if (target != null && !(target instanceof AbstractHorse)) {
                    if (target instanceof LivingEntity le && le.getControllingPassenger() != null) {
                        return;
                    }
                    if (target instanceof Player && !FlyingEntityConfig.isPlayerRidingAllowed()) {
                        return;
                    }
                    if (FlyingEntityConfig.isEntityBlacklisted(target.getType())) {
                        return;
                    }
                    if (packet.isMount()) {
                        boolean success = player.startRiding(target, true);
                        if (success) {
                            ModRideTracker.markRide(player, target);
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
                            ModRideTracker.clearRide(player);

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
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
