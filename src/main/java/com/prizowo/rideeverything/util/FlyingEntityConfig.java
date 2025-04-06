package com.prizowo.rideeverything.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "rideeverything", bus = Mod.EventBusSubscriber.Bus.MOD)
public class FlyingEntityConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec CONFIG;
    
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_FLYING_ENTITIES;
    
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_FLYING_ENTITIES;
    
    private static final Set<EntityType<?>> additionalFlyingEntities = new HashSet<>();
    private static final Set<EntityType<?>> excludedFlyingEntities = new HashSet<>();
    
    static {
        BUILDER.comment("RideEverything Flying Entity Configuration").push("flying_entities");
        
        ADDITIONAL_FLYING_ENTITIES = BUILDER
                .comment("Additional flying entity types, format: 'modid:entity_name'")
                .worldRestart()
                .defineList("additional_flying_entities", new ArrayList<>(), obj -> obj instanceof String);
        
        EXCLUDED_FLYING_ENTITIES = BUILDER
                .comment("Excluded entity types, format: 'modid:entity_name' (can override default behavior)")
                .worldRestart()
                .defineList("excluded_flying_entities", new ArrayList<>(), obj -> obj instanceof String);
        
        BUILDER.pop();
        CONFIG = BUILDER.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG);
    }
    
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        parseConfig();
    }
    
    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        parseConfig();
    }
    
    private static void parseConfig() {
        additionalFlyingEntities.clear();
        excludedFlyingEntities.clear();
        
        for (String entityStr : ADDITIONAL_FLYING_ENTITIES.get()) {
            try {
                ResourceLocation entityId = new ResourceLocation(entityStr);
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                if (entityType != null) {
                    additionalFlyingEntities.add(entityType);
                }
            } catch (Exception e) {
            }
        }
        
        for (String entityStr : EXCLUDED_FLYING_ENTITIES.get()) {
            try {
                ResourceLocation entityId = new ResourceLocation(entityStr);
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                if (entityType != null) {
                    excludedFlyingEntities.add(entityType);
                }
            } catch (Exception e) {
            }
        }
    }
    
    public static boolean isConfiguredAsFlying(EntityType<?> entityType) {
        return additionalFlyingEntities.contains(entityType);
    }
    
    public static boolean isExcludedFromFlying(EntityType<?> entityType) {
        return excludedFlyingEntities.contains(entityType);
    }
} 