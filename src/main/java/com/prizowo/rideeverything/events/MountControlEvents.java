package com.prizowo.rideeverything.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "rideeverything")
public class MountControlEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;

            if (player.isPassenger()) {
                Entity et = player.getVehicle();
                if (et instanceof Mob mt) {

                    float forward = player.zza;
                    float strafe = player.xxa;

                    if (player.getPose() == Pose.CROUCHING && mt.onGround()) {
                        double jumpPower = 0.5;

                        if (forward != 0 || strafe != 0) {
                            jumpPower *= 1.2;
                        }
                        
                        Vec3 currentVelocity = mt.getDeltaMovement();
                        mt.setDeltaMovement(currentVelocity.x, jumpPower, currentVelocity.z);
                        mt.hasImpulse = true;
                    }
                    
                    if (forward != 0 || strafe != 0) {
                        mt.setYRot(player.getYRot());
                        mt.yRotO = mt.getYRot();
                        mt.setYBodyRot(mt.getYRot());
                        mt.setYHeadRot(mt.getYRot());
                        
                        Vec3 currentVelocity = mt.getDeltaMovement();
                        
                        float yaw = player.getYRot();
                        double rad = Math.toRadians(yaw);
                        double speed = mt.getSpeed() * 0.5;
                        
                        double motionX = -Math.sin(rad) * forward * speed;
                        double motionZ = Math.cos(rad) * forward * speed;
                        
                        motionX += Math.cos(rad) * strafe * speed;
                        motionZ += Math.sin(rad) * strafe * speed;
                        
                        double motionY = currentVelocity.y;
                        
                        Vec3 newVelocity = new Vec3(
                            currentVelocity.x + (motionX - currentVelocity.x) * 0.5,
                            motionY,
                            currentVelocity.z + (motionZ - currentVelocity.z) * 0.5
                        );
                        
                        mt.setDeltaMovement(newVelocity);
                        mt.hasImpulse = true;
                    }
                }
            }
        }
    }
} 