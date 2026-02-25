package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.util.FlyingEntityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "rideeverything")
public class MountControlEvents {
    private static final Map<Class<?>, Boolean> flyingEntityCache = new HashMap<>();

    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;

    private static boolean shouldSkipEntity(Mob mob, Player rider) {
        if (mob instanceof AbstractHorse) return true;
        Entity controller = mob.getControllingPassenger();
        return controller != null && controller != rider;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        Entity passenger = mob.getFirstPassenger();
        if (!(passenger instanceof Player player)) return;
        if (shouldSkipEntity(mob, player)) return;
        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.xxa = 0;
        mob.zza = 0;
        mob.yya = 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        if (!player.isPassenger()) return;
        Entity et = player.getVehicle();
        if (!(et instanceof Mob mob)) return;
        if (shouldSkipEntity(mob, player)) return;
        float jumpPower = player.getPersistentData().getFloat("mounting_jumpPower");
        boolean ascending = player.getPersistentData().getBoolean("mounting_ascending");
        boolean descending = player.getPersistentData().getBoolean("mounting_descending");
        float forward = player.getPersistentData().getFloat("mounting_forward");
        float strafe = player.getPersistentData().getFloat("mounting_strafe");
        boolean sprinting = player.getPersistentData().getBoolean("mounting_sprinting");
        if (jumpPower > 0) {
            player.getPersistentData().putFloat("mounting_jumpPower", 0);
        }
        boolean isFlying = isFlying(mob);
        if (isFlying) {
            handleFlyingMobControl(mob, player, ascending, descending, forward, strafe, sprinting);
        } else {
            handleGroundMobControl(mob, player, jumpPower, forward, strafe, sprinting);
        }
    }

    private static void handleGroundMobControl(Mob mob, Player player, float jumpPower, float forward, float strafe, boolean isSprinting) {
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setXRot(player.getXRot() * 0.5F);
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        float moveStrafe = strafe * 0.5F;
        float moveForward = forward;
        if (moveForward <= 0.0F) {
            moveForward *= 0.25F;
        }
        float speed = (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (isSprinting && moveForward > 0) {
            speed *= SPRINT_SPEED_MULTIPLIER;
        }
        if (jumpPower > 0 && mob.onGround()) {
            double baseJumpStrength = 0.7;
            BlockPos jumpBelowPos = mob.blockPosition();
            float blockJumpFactor = mob.level().getBlockState(jumpBelowPos).getBlock() instanceof net.minecraft.world.level.block.HoneyBlock ? 0.5F : 1.0F;
            double jumpY = baseJumpStrength * (double) jumpPower * (double) blockJumpFactor;
            if (mob.hasEffect(MobEffects.JUMP)) {
                jumpY += (double) ((float) (mob.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1F);
            }
            Vec3 currentVel = mob.getDeltaMovement();
            double boostX = 0;
            double boostZ = 0;
            if (moveForward > 0) {
                float yawRad = mob.getYRot() * ((float) Math.PI / 180F);
                boostX += -Math.sin(yawRad) * 0.4F * jumpPower;
                boostZ += Math.cos(yawRad) * 0.4F * jumpPower;
            }

            mob.setDeltaMovement(currentVel.x + boostX, jumpY, currentVel.z + boostZ);
            mob.hasImpulse = true;
        }
        mob.setNoGravity(false);
        if (moveForward != 0 || moveStrafe != 0) {
            Level level = mob.level();
            BlockPos belowPos = BlockPos.containing(mob.getX(), mob.getBoundingBox().minY - 0.5000001, mob.getZ());
            float blockFriction = level.getBlockState(belowPos).getFriction(level, belowPos, mob);
            float accelFactor;
            if (mob.onGround()) {
                accelFactor = speed * (0.21600002F / (blockFriction * blockFriction * blockFriction));
            } else {
                accelFactor = 0.02F;
            }
            Vec3 inputVec = getInputVector(new Vec3(moveStrafe, 0, moveForward), accelFactor, mob.getYRot());
            Vec3 currentVel = mob.getDeltaMovement();
            mob.setDeltaMovement(currentVel.x + inputVec.x, currentVel.y, currentVel.z + inputVec.z);
            mob.hasImpulse = true;
        }
    }

    private static Vec3 getInputVector(Vec3 relative, float motionScaler, float facing) {
        double d0 = relative.lengthSqr();
        if (d0 < 1.0E-7) {
            return Vec3.ZERO;
        }
        Vec3 vec3 = (d0 > 1.0 ? relative.normalize() : relative).scale(motionScaler);
        float sinYaw = (float) Math.sin(facing * ((float) Math.PI / 180F));
        float cosYaw = (float) Math.cos(facing * ((float) Math.PI / 180F));
        return new Vec3(vec3.x * (double) cosYaw - vec3.z * (double) sinYaw, vec3.y, vec3.z * (double) cosYaw + vec3.x * (double) sinYaw);
    }

    private static void handleFlyingMobControl(Mob mob, Player player, boolean ascending, boolean descending, float forward, float strafe, boolean isSprinting) {
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        Vec3 currentVelocity = mob.getDeltaMovement();
        double verticalMotion;
        if (ascending) {
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
        double motionX;
        double motionZ;
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
        } else if (type == EntityType.BAT || type == EntityType.BEE || type == EntityType.BLAZE || type == EntityType.PHANTOM || type == EntityType.GHAST || type == EntityType.ALLAY || type == EntityType.VEX || type == EntityType.WITHER) {
            result = true;
        }
        flyingEntityCache.put(mobClass, result);
        return result;
    }
}
