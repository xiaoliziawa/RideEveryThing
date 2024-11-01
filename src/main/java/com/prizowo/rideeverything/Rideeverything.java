package com.prizowo.rideeverything;

import com.mojang.blaze3d.platform.InputConstants;
import com.prizowo.rideeverything.init.KeyBindings;
import com.prizowo.rideeverything.init.ModEntities;
import com.prizowo.rideeverything.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("rideeverything")
public class Rideeverything {
    // 定义按键绑定
    public static final KeyMapping RIDE_KEY = new KeyMapping(
            "key.rideeverything.ride",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.rideeverything"
    );
    
    public static final KeyMapping DISMOUNT_KEY = new KeyMapping(
            "key.rideeverything.dismount",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.categories.rideeverything"
    );

    public Rideeverything() {
        MinecraftForge.EVENT_BUS.register(this);
        
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modBus.addListener(KeyBindings::registerKeys);
        
        ModEntities.ENTITIES.register(modBus);
        
        NetworkHandler.init();
    }

    @SubscribeEvent
    public void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RIDE_KEY);
        event.register(DISMOUNT_KEY);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "rideeverything")
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                if (RIDE_KEY.consumeClick()) {
                    handleRideKey();
                }
                
                if (DISMOUNT_KEY.consumeClick()) {
                    handleDismountKey();
                }
            }
        }
    }

    private static void handleRideKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Entity target = mc.crosshairPickEntity;
        
        if (target != null && !player.isPassenger()) {
            net.minecraft.network.protocol.game.ServerboundInteractPacket packet =
                net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(
                    target, 
                    false,
                    net.minecraft.world.InteractionHand.MAIN_HAND
                );
            
            if (player instanceof LocalPlayer) {
                player.connection.send(packet);
            }
            
            player.startRiding(target, true);
        }
    }
    
    private static void handleDismountKey() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        if (player.isPassenger()) {
            player.stopRiding();
        }
    }
}
