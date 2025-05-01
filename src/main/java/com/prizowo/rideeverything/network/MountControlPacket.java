package com.prizowo.rideeverything.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MountControlPacket(boolean jumping, boolean descending, float forward, float strafe, boolean sprinting) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "mount_control");
    public static final Type<MountControlPacket> TYPE = new Type<>(ID);
    
    public static final StreamCodec<FriendlyByteBuf, MountControlPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.jumping());
                buf.writeBoolean(packet.descending());
                buf.writeFloat(packet.forward());
                buf.writeFloat(packet.strafe());
                buf.writeBoolean(packet.sprinting());
            },
            buf -> new MountControlPacket(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean()
            )
    );
    
    public static void handle(MountControlPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                player.getPersistentData().putBoolean("mounting_jumping", packet.jumping());
                player.getPersistentData().putBoolean("mounting_descending", packet.descending());
                player.getPersistentData().putFloat("mounting_forward", packet.forward());
                player.getPersistentData().putFloat("mounting_strafe", packet.strafe());
                player.getPersistentData().putBoolean("mounting_sprinting", packet.sprinting());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
} 