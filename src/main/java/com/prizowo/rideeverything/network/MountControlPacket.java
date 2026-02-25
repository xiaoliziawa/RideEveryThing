package com.prizowo.rideeverything.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MountControlPacket(byte flags, float forward, float strafe) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "mount_control");
    public static final Type<MountControlPacket> TYPE = new Type<>(ID);

    private static final byte FLAG_ASCENDING  = 1;
    private static final byte FLAG_DESCENDING = 1 << 1;
    private static final byte FLAG_SPRINTING  = 1 << 2;

    public MountControlPacket(boolean ascending, boolean descending, boolean sprinting, float forward, float strafe) {
        this(packFlags(ascending, descending, sprinting), forward, strafe);
    }

    private static byte packFlags(boolean ascending, boolean descending, boolean sprinting) {
        byte b = 0;
        if (ascending)  b |= FLAG_ASCENDING;
        if (descending) b |= FLAG_DESCENDING;
        if (sprinting)  b |= FLAG_SPRINTING;
        return b;
    }

    public boolean ascending()  { return (flags & FLAG_ASCENDING)  != 0; }
    public boolean descending() { return (flags & FLAG_DESCENDING) != 0; }
    public boolean sprinting()  { return (flags & FLAG_SPRINTING)  != 0; }

    public static final StreamCodec<FriendlyByteBuf, MountControlPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeByte(packet.flags());
                buf.writeFloat(packet.forward());
                buf.writeFloat(packet.strafe());
            },
            buf -> new MountControlPacket(
                buf.readByte(),
                buf.readFloat(),
                buf.readFloat()
            )
    );

    public static void handle(MountControlPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.isPassenger()) {
                var vehicle = player.getVehicle();
                if (vehicle instanceof Mob mob) {
                    if (mob instanceof AbstractHorse) return;
                    if (mob.getControllingPassenger() != null && mob.getControllingPassenger() != player) return;

                    player.getPersistentData().putBoolean("mounting_ascending", packet.ascending());
                    player.getPersistentData().putBoolean("mounting_descending", packet.descending());
                    player.getPersistentData().putFloat("mounting_forward", packet.forward());
                    player.getPersistentData().putFloat("mounting_strafe", packet.strafe());
                    player.getPersistentData().putBoolean("mounting_sprinting", packet.sprinting());
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
