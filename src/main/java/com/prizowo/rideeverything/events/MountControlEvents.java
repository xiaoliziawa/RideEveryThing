package com.prizowo.rideeverything.events;

import com.prizowo.rideeverything.util.FlyingEntityConfig;
import com.prizowo.rideeverything.util.ModRideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "rideeverything")
public class MountControlEvents {
    private static final Map<EntityType<?>, Boolean> flyingEntityCache = new HashMap<>();
    private static final Map<Integer, CachedMobData> mobDataCache = new HashMap<>();

    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;
    private static final double FRICTION_RECALC_THRESHOLD = 0.5;

    private static class CachedMobData {
        float lastFriction = 0.91F;
        BlockPos lastFrictionPos = BlockPos.ZERO;
        float lastYaw = 0;
        float cachedSinYaw = 0;
        float cachedCosYaw = 0;
    }

    private static boolean shouldSkipEntity(Mob mob, Player rider) {
        return !ModRideTracker.isControlledModRide(rider, mob);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        Entity passenger = entity.getFirstPassenger();
        if (!(passenger instanceof Player player)) return;
        if (!(entity instanceof Mob mob)) return;
        if (shouldSkipEntity(mob, player)) return;

        mob.getNavigation().stop();
        mob.setTarget(null);
        mob.xxa = 0;
        mob.zza = 0;
        mob.yya = 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.isPassenger()) {
            ModRideTracker.clearRide(player);
            return;
        }

        Entity et = player.getVehicle();
        if (!(et instanceof Mob mob)) return;
        if (shouldSkipEntity(mob, player)) return;

        var nbt = player.getPersistentData();
        float jumpPower = nbt.getFloat("mounting_jumpPower");
        boolean ascending = nbt.getBoolean("mounting_ascending");
        boolean descending = nbt.getBoolean("mounting_descending");
        float forward = nbt.getFloat("mounting_forward");
        float strafe = nbt.getFloat("mounting_strafe");
        boolean sprinting = nbt.getBoolean("mounting_sprinting");

        if (jumpPower > 0) {
            nbt.putFloat("mounting_jumpPower", 0);
        }

        boolean isFlying = isFlying(mob);

        if (isFlying) {
            handleFlyingMobControl(mob, player, ascending, descending, forward, strafe, sprinting);
        } else {
            handleGroundMobControl(mob, player, jumpPower, forward, strafe, sprinting);
        }
    }

    private static void handleGroundMobControl(Mob mob, Player player, float jumpPower, float forward, float strafe, boolean isSprinting) {
        int mobId = mob.getId();
        CachedMobData cache = mobDataCache.computeIfAbsent(mobId, k -> new CachedMobData());

        float yaw = player.getYRot();
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setXRot(player.getXRot() * 0.5F);
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);

        if (Math.abs(yaw - cache.lastYaw) > 1.0F) {
            float yawRad = yaw * DEG_TO_RAD;
            cache.cachedSinYaw = net.minecraft.util.Mth.sin(yawRad);
            cache.cachedCosYaw = net.minecraft.util.Mth.cos(yawRad);
            cache.lastYaw = yaw;
        }

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
            float blockJumpFactor = getBlockJumpFactor(mob);
            double jumpY = baseJumpStrength * (double) jumpPower * (double) blockJumpFactor;

            var jumpEffect = mob.getEffect(MobEffects.JUMP);
            if (jumpEffect != null) {
                jumpY += (double) ((float) (jumpEffect.getAmplifier() + 1) * 0.1F);
            }

            Vec3 currentVel = mob.getDeltaMovement();
            mob.setDeltaMovement(currentVel.x, jumpY, currentVel.z);
            mob.hasImpulse = true;

            if (moveForward > 0) {
                mob.setDeltaMovement(
                    mob.getDeltaMovement().add(
                        (double) (-0.4F * cache.cachedSinYaw * jumpPower),
                        0.0,
                        (double) (0.4F * cache.cachedCosYaw * jumpPower)
                    )
                );
            }
        }

        mob.setNoGravity(false);

        if (moveForward != 0 || moveStrafe != 0) {
            Level level = mob.level();
            BlockPos belowPos = mob.getBlockPosBelowThatAffectsMyMovement();

            float blockFriction;
            if (cache.lastFrictionPos.distSqr(belowPos) > FRICTION_RECALC_THRESHOLD) {
                blockFriction = level.getBlockState(belowPos).getFriction(level, belowPos, mob);
                cache.lastFriction = blockFriction;
                cache.lastFrictionPos = belowPos;
            } else {
                blockFriction = cache.lastFriction;
            }

            float accelFactor;
            if (mob.onGround()) {
                accelFactor = speed * (0.21600002F / (blockFriction * blockFriction * blockFriction));
            } else {
                accelFactor = 0.02F;
            }

            Vec3 inputVec = getInputVector(moveStrafe, moveForward, accelFactor, cache.cachedSinYaw, cache.cachedCosYaw);
            Vec3 currentVel = mob.getDeltaMovement();

            mob.setDeltaMovement(currentVel.x + inputVec.x, currentVel.y, currentVel.z + inputVec.z);
            mob.hasImpulse = true;
        }
    }

    private static float getBlockJumpFactor(Mob mob) {
        Level level = mob.level();
        float f = level.getBlockState(mob.blockPosition()).getBlock().getJumpFactor();
        float f1 = level.getBlockState(mob.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return (double) f == 1.0 ? f1 : f;
    }

    private static Vec3 getInputVector(float strafe, float forward, float motionScaler, float sinYaw, float cosYaw) {
        float lengthSq = strafe * strafe + forward * forward;
        if (lengthSq < 1.0E-7F) {
            return Vec3.ZERO;
        }

        if (lengthSq > 1.0F) {
            float invLength = Mth.invSqrt(lengthSq);
            strafe *= invLength;
            forward *= invLength;
        }

        float scaledStrafe = strafe * motionScaler;
        float scaledForward = forward * motionScaler;

        return new Vec3(
            scaledStrafe * (double) cosYaw - scaledForward * (double) sinYaw,
            0,
            scaledForward * (double) cosYaw + scaledStrafe * (double) sinYaw
        );
    }

    private static void handleFlyingMobControl(Mob mob, Player player, boolean ascending, boolean descending, float forward, float strafe, boolean isSprinting) {
        int mobId = mob.getId();
        CachedMobData cache = mobDataCache.computeIfAbsent(mobId, k -> new CachedMobData());

        float yaw = player.getYRot();
        mob.setYRot(yaw);
        mob.yRotO = yaw;
        mob.setYBodyRot(yaw);
        mob.setYHeadRot(yaw);

        if (Math.abs(yaw - cache.lastYaw) > 1.0F) {
            double rad = Math.toRadians(yaw);
            cache.cachedSinYaw = (float) Math.sin(rad);
            cache.cachedCosYaw = (float) Math.cos(rad);
            cache.lastYaw = yaw;
        }

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

        double motionX;
        double motionZ;

        if (forward != 0 || strafe != 0) {
            motionX = -cache.cachedSinYaw * forward * horizontalSpeed + cache.cachedCosYaw * strafe * horizontalSpeed;
            motionZ = cache.cachedCosYaw * forward * horizontalSpeed + cache.cachedSinYaw * strafe * horizontalSpeed;
        } else {
            motionX = currentVelocity.x * 0.8;
            motionZ = currentVelocity.z * 0.8;
        }

        mob.setDeltaMovement(new Vec3(motionX, verticalMotion, motionZ));
        mob.hasImpulse = true;
        mob.setNoGravity(true);
    }

    private static boolean isFlying(Mob mob) {
        EntityType<?> type = mob.getType();
        if (flyingEntityCache.containsKey(type)) {
            return flyingEntityCache.get(type);
        }

        boolean result = false;

        if (FlyingEntityConfig.isExcludedFromFlying(type)) {
            flyingEntityCache.put(type, false);
            return false;
        }

        if (FlyingEntityConfig.isConfiguredAsFlying(type)) {
            flyingEntityCache.put(type, true);
            return true;
        }

        if (mob instanceof FlyingAnimal) {
            result = true;
        } else if (type == EntityType.BAT ||
            type == EntityType.BEE ||
            type == EntityType.BLAZE ||
            type == EntityType.PHANTOM ||
            type == EntityType.GHAST ||
            type == EntityType.ALLAY ||
            type == EntityType.VEX ||
            type == EntityType.WITHER) {
            result = true;
        }

        flyingEntityCache.put(type, result);
        return result;
    }

    public static void clearCache(int mobId) {
        mobDataCache.remove(mobId);
    }
}
