package com.prizowo.rideeverything.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MountControlPacket {
    private final float jumpPower;
    private final boolean ascending;
    private final boolean descending;
    private final float forward;
    private final float strafe;
    private final boolean sprinting;

    public MountControlPacket(float jumpPower, boolean ascending, boolean descending, float forward, float strafe, boolean sprinting) {
        this.jumpPower = jumpPower;
        this.ascending = ascending;
        this.descending = descending;
        this.forward = forward;
        this.strafe = strafe;
        this.sprinting = sprinting;
    }

    public static void encode(MountControlPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.jumpPower);
        buf.writeBoolean(packet.ascending);
        buf.writeBoolean(packet.descending);
        buf.writeFloat(packet.forward);
        buf.writeFloat(packet.strafe);
        buf.writeBoolean(packet.sprinting);
    }

    public static MountControlPacket decode(FriendlyByteBuf buf) {
        return new MountControlPacket(buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
    }

    public static void handle(MountControlPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.isPassenger()) {
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof Mob mob) {
                    if (mob instanceof AbstractHorse) return;
                    if (mob.getControllingPassenger() != null && mob.getControllingPassenger() != player) return;
                    player.getPersistentData().putFloat("mounting_jumpPower", packet.jumpPower);
                    player.getPersistentData().putBoolean("mounting_ascending", packet.ascending);
                    player.getPersistentData().putBoolean("mounting_descending", packet.descending);
                    player.getPersistentData().putFloat("mounting_forward", packet.forward);
                    player.getPersistentData().putFloat("mounting_strafe", packet.strafe);
                    player.getPersistentData().putBoolean("mounting_sprinting", packet.sprinting);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
