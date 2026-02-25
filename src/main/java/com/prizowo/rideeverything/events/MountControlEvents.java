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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "rideeverything")
public class MountControlEvents {
    private static final Map<Class<?>, Boolean> flyingEntityCache = new HashMap<>();

    private static final double SPRINT_SPEED_MULTIPLIER = 1.3;

    private static boolean shouldSkipEntity(Mob mob, Player rider) {
        if (mob instanceof AbstractHorse) return true;
        Entity controller = mob.getControllingPassenger();
        return controller != null && controller != rider;
    }

    /**
     * Suppress AI navigation for force-ridden mobs before their tick.
     * Prevents the mob's pathfinding from fighting player controls.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
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

        // Consume jumpPower (one-shot event from space release)
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

    /**
     * Ground movement using vanilla horse physics:
     * - AbstractHorse.tickRidden() for rotation sync
     * - AbstractHorse.getRiddenInput() for input processing (50% strafe, 25% backward)
     * - AbstractHorse.getRiddenSpeed() for speed
     * - AbstractHorse.executeRidersJump() for charged jump
     * - LivingEntity.travel() for acceleration/friction
     */
    private static void handleGroundMobControl(Mob mob, Player player, float jumpPower, float forward, float strafe, boolean isSprinting) {
        // 1. Rotation sync (AbstractHorse.tickRidden + getRiddenRotation)
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setXRot(player.getXRot() * 0.5F);
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());

        // 2. Input processing (AbstractHorse.getRiddenInput)
        float moveStrafe = strafe * 0.5F;
        float moveForward = forward;
        if (moveForward <= 0.0F) {
            moveForward *= 0.25F;
        }

        // 3. Speed (AbstractHorse.getRiddenSpeed)
        float speed = (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (isSprinting && moveForward > 0) {
            speed *= SPRINT_SPEED_MULTIPLIER;
        }

        // 4. Charged jump (AbstractHorse.executeRidersJump)
        if (jumpPower > 0 && mob.onGround()) {
            // getJumpPower(multiplier) = JUMP_STRENGTH * multiplier * blockJumpFactor + jumpBoostPower
            // Generic mobs don't have JUMP_STRENGTH attribute, use a fixed base
            double baseJumpStrength = 0.7;

            // getBlockJumpFactor is protected, replicate logic
            float blockJumpFactor = getBlockJumpFactor(mob);
            double jumpY = baseJumpStrength * (double) jumpPower * (double) blockJumpFactor;

            // Jump Boost potion effect
            var jumpEffect = mob.getEffect(MobEffects.JUMP);
            if (jumpEffect != null) {
                jumpY += (double) ((float) (jumpEffect.getAmplifier() + 1) * 0.1F);
            }

            Vec3 currentVel = mob.getDeltaMovement();
            mob.setDeltaMovement(currentVel.x, jumpY, currentVel.z);
            mob.hasImpulse = true;

            // Forward boost on jump (vanilla horse: -0.4F * sin/cos * jumpPower)
            if (moveForward > 0) {
                float yawRad = mob.getYRot() * ((float) Math.PI / 180F);
                mob.setDeltaMovement(
                    mob.getDeltaMovement().add(
                        (double) (-0.4F * net.minecraft.util.Mth.sin(yawRad) * jumpPower),
                        0.0,
                        (double) (0.4F * net.minecraft.util.Mth.cos(yawRad) * jumpPower)
                    )
                );
            }
        }

        mob.setNoGravity(false);

        // 5. Acceleration-based movement (LivingEntity.travel math)
        if (moveForward != 0 || moveStrafe != 0) {
            Level level = mob.level();
            BlockPos belowPos = mob.getBlockPosBelowThatAffectsMyMovement();
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

    /**
     * Replicate Entity.getBlockJumpFactor() which is protected.
     */
    private static float getBlockJumpFactor(Mob mob) {
        float f = mob.level().getBlockState(mob.blockPosition()).getBlock().getJumpFactor();
        float f1 = mob.level().getBlockState(mob.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return (double) f == 1.0 ? f1 : f;
    }

    /**
     * Entity.getInputVector: converts (strafe, up, forward) + acceleration + yaw into world-space velocity.
     */
    private static Vec3 getInputVector(Vec3 relative, float motionScaler, float facing) {
        double d0 = relative.lengthSqr();
        if (d0 < 1.0E-7) {
            return Vec3.ZERO;
        }
        Vec3 vec3 = (d0 > 1.0 ? relative.normalize() : relative).scale(motionScaler);
        float sinYaw = net.minecraft.util.Mth.sin(facing * ((float) Math.PI / 180F));
        float cosYaw = net.minecraft.util.Mth.cos(facing * ((float) Math.PI / 180F));
        return new Vec3(
            vec3.x * (double) cosYaw - vec3.z * (double) sinYaw,
            vec3.y,
            vec3.z * (double) cosYaw + vec3.x * (double) sinYaw
        );
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

        mob.setDeltaMovement(new Vec3(motionX, verticalMotion, motionZ));
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

        flyingEntityCache.put(mobClass, result);
        return result;
    }
}
