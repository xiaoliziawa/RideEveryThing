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

public record MountJumpPacket(float jumpPower) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("rideeverything", "mount_jump");
    public static final Type<MountJumpPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, MountJumpPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeFloat(packet.jumpPower()),
            buf -> new MountJumpPacket(buf.readFloat())
    );

    public static void handle(MountJumpPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.isPassenger()) {
                var vehicle = player.getVehicle();
                if (vehicle instanceof Mob mob) {
                    if (mob instanceof AbstractHorse) return;
                    if (mob.getControllingPassenger() != null && mob.getControllingPassenger() != player) return;

                    player.getPersistentData().putFloat("mounting_jumpPower", packet.jumpPower());
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
