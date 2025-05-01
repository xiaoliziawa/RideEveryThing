package com.prizowo.rideeverything.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "rideeverything", bus = EventBusSubscriber.Bus.MOD)
public class FlyingEntityConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_FLYING_ENTITIES;
    
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_FLYING_ENTITIES;
    
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
        SPEC = BUILDER.build();
    }
    
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC);
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
                ResourceLocation entityId = ResourceLocation.parse(entityStr);
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
                additionalFlyingEntities.add(entityType);
            } catch (Exception e) {
            }
        }
        
        for (String entityStr : EXCLUDED_FLYING_ENTITIES.get()) {
            try {
                ResourceLocation entityId = ResourceLocation.parse(entityStr);
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);
                excludedFlyingEntities.add(entityType);
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