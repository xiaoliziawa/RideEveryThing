package com.prizowo.rideeverything.network;

import com.prizowo.rideeverything.util.FlyingEntityConfig;
import com.prizowo.rideeverything.util.ModRideTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RidePacket {
    private final int entityId;
    private final boolean isMount;

    public RidePacket(int entityId, boolean isMount) {
        this.entityId = entityId;
        this.isMount = isMount;
    }

    public static void encode(RidePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.isMount);
    }

    public static RidePacket decode(FriendlyByteBuf buf) {
        return new RidePacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(RidePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();

                Entity target = level.getEntity(msg.entityId);
                if (target != null && !(target instanceof AbstractHorse)) {
                    // Skip entities that already have their own riding system (saddled pigs, striders, etc.)
                    if (target instanceof LivingEntity le && le.getControllingPassenger() != null) {
                        return;
                    }
                    if (target instanceof Player && !FlyingEntityConfig.isPlayerRidingAllowed()) {
                        return;
                    }
                    if (FlyingEntityConfig.isEntityBlacklisted(target.getType())) {
                        return;
                    }
                    if (msg.isMount) {
                        boolean success = player.startRiding(target, true);
                        if (success) {
                            ModRideTracker.markRide(player, target);
                            NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new RideConfirmPacket(player.getId(), msg.entityId, true));
                        }
                    } else {
                        if (player.isPassenger()) {
                            player.stopRiding();
                            ModRideTracker.clearRide(player);
                            NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new RideConfirmPacket(player.getId(), msg.entityId, false));
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
