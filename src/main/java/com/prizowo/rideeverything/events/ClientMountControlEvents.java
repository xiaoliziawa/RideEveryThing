package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.client.KeyBindings;
import com.prizowo.rideeverything.network.MountControlPacket;
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

    private static float lastJumpPower = 0;
    private static boolean wasAscending = false;
    private static boolean wasDescending = false;
    private static float lastForward = 0;
    private static float lastStrafe = 0;
    private static boolean wasSprinting = false;

    private static long lastWPressTime = 0;
    private static final long DOUBLE_PRESS_TIME = 300;
    private static boolean isSprinting = false;

    private static int jumpChargeTicks = 0;
    private static boolean wasJumpDown = false;

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

            // Sprint detection via double-tap W
            if (forward > 0) {
                long currentTime = System.currentTimeMillis();
                if (lastForward <= 0) {
                    if (currentTime - lastWPressTime < DOUBLE_PRESS_TIME) {
                        isSprinting = true;
                    }
                    lastWPressTime = currentTime;
                }
            } else {
                isSprinting = false;
            }

            // Jump charge logic (vanilla horse style)
            float jumpPower = 0;
            if (ascending && !wasJumpDown) {
                jumpChargeTicks = 0;
            }
            if (ascending) {
                jumpChargeTicks++;
                // Play experience orb sound with rising pitch while charging
                if (jumpChargeTicks % 3 == 1) {
                    float chargeProgress = Math.min(1.0F, jumpChargeTicks / 12.0F);
                    float pitch = 0.5F + chargeProgress * 1.5F;
                    player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F, pitch);
                }
            } else if (wasJumpDown) {
                jumpPower = calculateJumpPower(jumpChargeTicks);
                jumpChargeTicks = 0;
            }
            wasJumpDown = ascending;

            if (jumpPower != lastJumpPower || ascending != wasAscending ||
                descending != wasDescending || forward != lastForward ||
                strafe != lastStrafe || isSprinting != wasSprinting) {

                PacketDistributor.sendToServer(
                    new MountControlPacket(jumpPower, ascending, descending, forward, strafe, isSprinting)
                );

                lastJumpPower = jumpPower;
                wasAscending = ascending;
                wasDescending = descending;
                lastForward = forward;
                lastStrafe = strafe;
                wasSprinting = isSprinting;
            }

            if (jumpPower > 0) {
                lastJumpPower = 0;
            }
        } else {
            resetState();
        }
    }

    /**
     * Vanilla horse jump charge scaling (from LocalPlayer.aiStep):
     * ticks < 10: scale = ticks * 0.1F
     * ticks >= 10: scale = 0.8F + 2.0F / (ticks - 9) * 0.1F
     * Then: scale >= 0.9 → power = 1.0; else power = 0.4 + 0.4 * scale / 0.9
     */
    private static float calculateJumpPower(int ticks) {
        if (ticks <= 0) return 0;

        float jumpScale;
        if (ticks < 10) {
            jumpScale = ticks * 0.1F;
        } else {
            jumpScale = 0.8F + 2.0F / (ticks - 9) * 0.1F;
        }
        jumpScale = Math.min(1.0F, jumpScale);

        if (jumpScale >= 0.9F) {
            return 1.0F;
        }
        return 0.4F + 0.4F * jumpScale / 0.9F;
    }

    private static void resetState() {
        lastJumpPower = 0;
        wasAscending = false;
        wasDescending = false;
        lastForward = 0;
        lastStrafe = 0;
        wasSprinting = false;
        isSprinting = false;
        jumpChargeTicks = 0;
        wasJumpDown = false;
    }
}
