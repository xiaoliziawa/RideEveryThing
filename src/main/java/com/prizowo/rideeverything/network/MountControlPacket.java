package com.prizowo.rideeverything.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MountControlPacket {
    private final boolean jumping;
    private final boolean descending;
    private final float forward;
    private final float strafe;

    public MountControlPacket(boolean jumping, boolean descending, float forward, float strafe) {
        this.jumping = jumping;
        this.descending = descending;
        this.forward = forward;
        this.strafe = strafe;
    }

    public static void encode(MountControlPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.jumping);
        buf.writeBoolean(packet.descending);
        buf.writeFloat(packet.forward);
        buf.writeFloat(packet.strafe);
    }

    public static MountControlPacket decode(FriendlyByteBuf buf) {
        return new MountControlPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(MountControlPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.isPassenger()) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof Mob) {
                    // 这些数据将在服务器端的MountControlEvents中使用
                    player.getPersistentData().putBoolean("mounting_jumping", packet.jumping);
                    player.getPersistentData().putBoolean("mounting_descending", packet.descending);
                    player.getPersistentData().putFloat("mounting_forward", packet.forward);
                    player.getPersistentData().putFloat("mounting_strafe", packet.strafe);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 