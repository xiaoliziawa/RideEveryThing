package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.client.KeyBindings;
import com.prizowo.rideeverything.network.MountControlPacket;
import com.prizowo.rideeverything.network.MountJumpPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "rideeverything", value = Dist.CLIENT)
public class ClientMountControlEvents {

    private static boolean lastAscending = false;
    private static boolean lastDescending = false;
    private static float lastForward = 0;
    private static float lastStrafe = 0;
    private static boolean lastSprinting = false;

    private static long lastWPressTime = 0;
    private static final long DOUBLE_PRESS_TIME = 300;
    private static boolean isSprinting = false;

    private static int jumpChargeTicks = 0;
    private static boolean wasJumpDown = false;

    private static final float JUMP_SCALE_THRESHOLD = 0.9F;
    private static final float JUMP_POWER_BASE = 0.4F;
    private static final float JUMP_POWER_SCALE = 0.4F / JUMP_SCALE_THRESHOLD;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player != null && player.isPassenger() && player.getVehicle() instanceof Mob mob && !(mob instanceof AbstractHorse)) {
            if (mob.getControllingPassenger() != null && mob.getControllingPassenger() != player) {
                resetState();
                return;
            }

            boolean ascending = minecraft.options.keyJump.isDown();
            boolean descending = KeyBindings.KEY_DESCEND.isDown();
            float forward = player.zza;
            float strafe = player.xxa;

            if (forward > 0) {
                if (lastForward <= 0) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastWPressTime < DOUBLE_PRESS_TIME) {
                        isSprinting = true;
                    }
                    lastWPressTime = currentTime;
                }
            } else {
                isSprinting = false;
            }

            if (ascending && !wasJumpDown) {
                jumpChargeTicks = 0;
            }
            if (ascending) {
                jumpChargeTicks++;
                if (jumpChargeTicks % 3 == 1) {
                    float chargeProgress = Math.min(1.0F, jumpChargeTicks / 12.0F);
                    float pitch = 0.5F + chargeProgress * 1.5F;
                    player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F, pitch);
                }
            } else if (wasJumpDown) {
                float jumpPower = calculateJumpPower(jumpChargeTicks);
                if (jumpPower > 0) {
                    PacketDistributor.sendToServer(new MountJumpPacket(jumpPower));
                }
                jumpChargeTicks = 0;
            }
            wasJumpDown = ascending;

            if (ascending != lastAscending || descending != lastDescending ||
                forward != lastForward || strafe != lastStrafe ||
                isSprinting != lastSprinting) {

                PacketDistributor.sendToServer(
                    new MountControlPacket(ascending, descending, isSprinting, forward, strafe)
                );

                lastAscending = ascending;
                lastDescending = descending;
                lastForward = forward;
                lastStrafe = strafe;
                lastSprinting = isSprinting;
            }
        } else {
            resetState();
        }
    }

    private static float calculateJumpPower(int ticks) {
        if (ticks <= 0) return 0;

        float jumpScale;
        if (ticks < 10) {
            jumpScale = ticks * 0.1F;
        } else {
            jumpScale = 0.8F + 2.0F / (ticks - 9) * 0.1F;
        }
        jumpScale = Math.min(1.0F, jumpScale);

        if (jumpScale >= JUMP_SCALE_THRESHOLD) {
            return 1.0F;
        }
        return JUMP_POWER_BASE + JUMP_POWER_SCALE * jumpScale;
    }

    private static void resetState() {
        lastAscending = false;
        lastDescending = false;
        lastForward = 0;
        lastStrafe = 0;
        lastSprinting = false;
        isSprinting = false;
        jumpChargeTicks = 0;
        wasJumpDown = false;
    }
}
