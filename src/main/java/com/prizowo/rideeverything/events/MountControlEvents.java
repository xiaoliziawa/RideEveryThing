package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "rideeverything")
public class MountControlEvents {
    private static final Map<Class<?>, Boolean> flyingEntityCache = new HashMap<>();
    
    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;

            if (player.isPassenger()) {
                Entity et = player.getVehicle();
                if (et instanceof Mob mt) {
                    boolean jumping = player.getPersistentData().getBoolean("mounting_jumping");
                    boolean descending = player.getPersistentData().getBoolean("mounting_descending");
                    float forward = player.getPersistentData().getFloat("mounting_forward");
                    float strafe = player.getPersistentData().getFloat("mounting_strafe");
                    boolean sprinting = player.getPersistentData().getBoolean("mounting_sprinting");

                    boolean isFlying = isFlying(mt);
                    
                    if (isFlying) {
                        handleFlyingMobControl(mt, player, jumping, descending, forward, strafe, sprinting);
                    } else {
                        handleGroundMobControl(mt, player, jumping, forward, strafe, sprinting);
                    }
                }
            }
        }
    }
    
    private static void handleFlyingMobControl(Mob mob, Player player, boolean jumping, boolean descending, float forward, float strafe, boolean isSprinting) {
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        
        Vec3 currentVelocity = mob.getDeltaMovement();
        
        double verticalMotion;
        if (jumping) {
            verticalMotion = 0.4;
        } else if (descending) {
            verticalMotion = -0.4;
        } else {
            verticalMotion = Math.max(-0.1, currentVelocity.y * 0.8);
        }
        
        double horizontalSpeed = 0.35;
        
        if (isSprinting && forward > 0) {
            horizontalSpeed *= SPRINT_SPEED_MULTIPLIER;
        }
        
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        
        double motionX = 0;
        double motionZ = 0;
        
        if (forward != 0 || strafe != 0) {
            motionX = -Math.sin(rad) * forward * horizontalSpeed;
            motionZ = Math.cos(rad) * forward * horizontalSpeed;
            
            motionX += Math.cos(rad) * strafe * horizontalSpeed;
            motionZ += Math.sin(rad) * strafe * horizontalSpeed;
        } else {
            motionX = currentVelocity.x * 0.8;
            motionZ = currentVelocity.z * 0.8;
        }
        
        Vec3 newVelocity = new Vec3(motionX, verticalMotion, motionZ);
        mob.setDeltaMovement(newVelocity);
        
        mob.hasImpulse = true;
        
        mob.setNoGravity(true);
    }
    
    private static void handleGroundMobControl(Mob mob, Player player, boolean jumping, float forward, float strafe, boolean isSprinting) {
        if (jumping && mob.onGround()) {
            double jumpPower = 0.5;
            
            if (forward != 0 || strafe != 0) {
                jumpPower *= 1.2;
            }
            
            Vec3 currentVelocity = mob.getDeltaMovement();
            mob.setDeltaMovement(currentVelocity.x, jumpPower, currentVelocity.z);
            mob.hasImpulse = true;
        }
        
        mob.setNoGravity(false);
        
        if (forward != 0 || strafe != 0) {
            mob.setYRot(player.getYRot());
            mob.yRotO = mob.getYRot();
            mob.setYBodyRot(mob.getYRot());
            mob.setYHeadRot(mob.getYRot());
            
            Vec3 currentVelocity = mob.getDeltaMovement();
            
            float yaw = player.getYRot();
            double rad = Math.toRadians(yaw);
            double speed = mob.getSpeed() * 0.5;
            
            if (isSprinting && forward > 0) {
                speed *= SPRINT_SPEED_MULTIPLIER;
            }
            
            double motionX = -Math.sin(rad) * forward * speed;
            double motionZ = Math.cos(rad) * forward * speed;
            
            motionX += Math.cos(rad) * strafe * speed;
            motionZ += Math.sin(rad) * strafe * speed;
            
            Vec3 newVelocity = new Vec3(
                currentVelocity.x + (motionX - currentVelocity.x) * 0.5,
                currentVelocity.y,
                currentVelocity.z + (motionZ - currentVelocity.z) * 0.5
            );
            
            mob.setDeltaMovement(newVelocity);
            mob.hasImpulse = true;
        }
    }
    

    private static boolean isFlying(Mob mob) {
        Class<?> mobClass = mob.getClass();
        if (flyingEntityCache.containsKey(mobClass)) {
            return flyingEntityCache.get(mobClass);
        }
        
        boolean result = false;
        EntityType<?> type = mob.getType();
        
        if (FlyingEntityConfig.isExcludedFromFlying(type)) {
            flyingEntityCache.put(mobClass, false);
            return false;
        }
        
        if (FlyingEntityConfig.isConfiguredAsFlying(type)) {
            flyingEntityCache.put(mobClass, true);
            return true;
        }
        
        if (mob instanceof FlyingAnimal) {
            result = true;
        } 
        else if (type == EntityType.BAT ||
            type == EntityType.BEE || 
            type == EntityType.BLAZE || 
            type == EntityType.PHANTOM || 
            type == EntityType.GHAST || 
            type == EntityType.ALLAY || 
            type == EntityType.VEX || 
            type == EntityType.WITHER) {
            result = true;
        }
        
        flyingEntityCache.put(mobClass, result);
        return result;
    }
} 