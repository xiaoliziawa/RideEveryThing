package com.prizowo.rideeverything.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ModRideTracker {
    private static final String VEHICLE_ID_TAG = "rideeverything_vehicle_id";

    public static void markRide(Player player, Entity vehicle) {
        player.getPersistentData().putInt(VEHICLE_ID_TAG, vehicle.getId());
    }

    public static void clearRide(Player player) {
        player.getPersistentData().remove(VEHICLE_ID_TAG);
    }

    public static boolean isModRide(Player player, Entity vehicle) {
        CompoundTag data = player.getPersistentData();
        return data.contains(VEHICLE_ID_TAG) && data.getInt(VEHICLE_ID_TAG) == vehicle.getId();
    }
}
