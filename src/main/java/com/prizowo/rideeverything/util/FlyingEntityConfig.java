package com.prizowo.rideeverything.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
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

@EventBusSubscriber(modid = "rideeverything")
public class FlyingEntityConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_FLYING_ENTITIES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_FLYING_ENTITIES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST;
    private static final ModConfigSpec.BooleanValue PLAYER_RIDING_ALLOWED_CONFIG;
    private static final ModConfigSpec.BooleanValue BLOCK_RIDING_ALLOWED_CONFIG;

    private static final Set<EntityType<?>> additionalFlyingEntities = new HashSet<>();
    private static final Set<EntityType<?>> excludedFlyingEntities = new HashSet<>();
    private static final Set<EntityType<?>> entityBlacklist = new HashSet<>();
    private static final Set<Identifier> blockBlacklist = new HashSet<>();

    private static boolean playerRidingAllowed = true;
    private static boolean blockRidingAllowed = true;

    static {
        BUILDER.comment("RideEverything Flying Entity Configuration").push("flying_entities");

        ADDITIONAL_FLYING_ENTITIES = BUILDER
                .comment("Additional flying entity types, format: 'modid:entity_name'")
                .worldRestart()
                .defineListAllowEmpty("additional_flying_entities", new ArrayList<>(), () -> "", obj -> obj instanceof String);

        EXCLUDED_FLYING_ENTITIES = BUILDER
                .comment("Excluded entity types, format: 'modid:entity_name' (can override default behavior)")
                .worldRestart()
                .defineListAllowEmpty("excluded_flying_entities", new ArrayList<>(), () -> "", obj -> obj instanceof String);

        BUILDER.pop();

        BUILDER.comment("RideEverything General Configuration").push("general");

        PLAYER_RIDING_ALLOWED_CONFIG = BUILDER
                .comment("Allow riding other players")
                .define("player_riding_allowed", true);

        BLOCK_RIDING_ALLOWED_CONFIG = BUILDER
                .comment("Allow sitting on blocks")
                .define("block_riding_allowed", true);

        ENTITY_BLACKLIST = BUILDER
                .comment("Entity types that cannot be ridden, format: 'modid:entity_name'")
                .defineListAllowEmpty("entity_blacklist", new ArrayList<>(), () -> "", obj -> obj instanceof String);

        BLOCK_BLACKLIST = BUILDER
                .comment("Block types that cannot be sat on, format: 'modid:block_name'")
                .defineListAllowEmpty("block_blacklist", new ArrayList<>(), () -> "", obj -> obj instanceof String);

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
        entityBlacklist.clear();
        blockBlacklist.clear();

        for (String entityStr : ADDITIONAL_FLYING_ENTITIES.get()) {
            try {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityStr));
                if (entityType != null) {
                    additionalFlyingEntities.add(entityType);
                }
            } catch (Exception e) {
            }
        }

        for (String entityStr : EXCLUDED_FLYING_ENTITIES.get()) {
            try {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityStr));
                if (entityType != null) {
                    excludedFlyingEntities.add(entityType);
                }
            } catch (Exception e) {
            }
        }

        for (String entityStr : ENTITY_BLACKLIST.get()) {
            try {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityStr));
                if (entityType != null) {
                    entityBlacklist.add(entityType);
                }
            } catch (Exception e) {
            }
        }

        for (String blockStr : BLOCK_BLACKLIST.get()) {
            try {
                blockBlacklist.add(Identifier.parse(blockStr));
            } catch (Exception e) {
            }
        }

        playerRidingAllowed = PLAYER_RIDING_ALLOWED_CONFIG.get();
        blockRidingAllowed = BLOCK_RIDING_ALLOWED_CONFIG.get();
    }

    public static boolean isConfiguredAsFlying(EntityType<?> entityType) {
        return additionalFlyingEntities.contains(entityType);
    }

    public static boolean isExcludedFromFlying(EntityType<?> entityType) {
        return excludedFlyingEntities.contains(entityType);
    }

    public static boolean isEntityBlacklisted(EntityType<?> entityType) {
        return entityBlacklist.contains(entityType);
    }

    public static boolean isBlockBlacklisted(BlockState state) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockBlacklist.contains(blockId);
    }

    public static boolean isPlayerRidingAllowed() {
        return playerRidingAllowed;
    }

    public static boolean isBlockRidingAllowed() {
        return blockRidingAllowed;
    }
}
