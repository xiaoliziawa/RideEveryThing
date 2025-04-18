package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.client.KeyBindings;
import com.prizowo.rideeverything.network.MountControlPacket;
import com.prizowo.rideeverything.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "rideeverything", value = Dist.CLIENT)
public class ClientMountControlEvents {

    private static boolean wasJumping = false;
    private static boolean wasDescending = false;
    private static float lastForward = 0;
    private static float lastStrafe = 0;
    private static boolean wasSprinting = false;
    
    private static long lastWPressTime = 0;
    private static final long DOUBLE_PRESS_TIME = 300;
    private static boolean isSprinting = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            
            if (player != null && player.isPassenger() && player.getVehicle() instanceof Mob) {
                boolean jumping = minecraft.options.keyJump.isDown();
                boolean descending = KeyBindings.KEY_DESCEND.isDown();
                float forward = player.zza;
                float strafe = player.xxa;
                
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
                
                if (jumping != wasJumping || descending != wasDescending ||
                    forward != lastForward || strafe != lastStrafe ||
                    isSprinting != wasSprinting) {
                    
                    NetworkHandler.INSTANCE.sendToServer(
                        new MountControlPacket(jumping, descending, forward, strafe, isSprinting)
                    );
                    
                    wasJumping = jumping;
                    wasDescending = descending;
                    lastForward = forward;
                    lastStrafe = strafe;
                    wasSprinting = isSprinting;
                }
            } else {
                wasJumping = false;
                wasDescending = false;
                lastForward = 0;
                lastStrafe = 0;
                wasSprinting = false;
                isSprinting = false;
            }
        }
    }
} 