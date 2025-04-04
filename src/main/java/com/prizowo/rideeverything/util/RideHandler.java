package com.prizowo.rideeverything.util;

import com.prizowo.rideeverything.network.NetworkHandler;
import com.prizowo.rideeverything.network.RidePacket;
import com.prizowo.rideeverything.network.CreateBlockSeatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RideHandler {
    public static void handleRideKey() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Entity target = mc.crosshairPickEntity;
        
        if (target != null && !player.isPassenger()) {
            if (target instanceof Player targetPlayer) {
                if (targetPlayer == player) return;
            }
            
            NetworkHandler.INSTANCE.sendToServer(new RidePacket(target.getId(), true));
        } else {
            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                
                NetworkHandler.INSTANCE.sendToServer(new CreateBlockSeatPacket(pos));
            }
        }
    }
    
    public static void handleDismountKey() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        if (player.isPassenger()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                NetworkHandler.INSTANCE.sendToServer(new RidePacket(vehicle.getId(), false));
            }
        }
    }
}
