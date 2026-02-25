package com.prizowo.rideeverything.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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

    // General
    private static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_RIDING;
    private static final ForgeConfigSpec.BooleanValue ALLOW_PLAYER_RIDING;

    // Blacklists
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_BLACKLIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST;

    // Flying entities
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_FLYING_ENTITIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_FLYING_ENTITIES;

    // Parsed caches
    private static final Set<EntityType<?>> additionalFlyingEntities = new HashSet<>();
    private static final Set<EntityType<?>> excludedFlyingEntities = new HashSet<>();
    private static final Set<ResourceLocation> blacklistedBlockIds = new HashSet<>();
    private static final Set<TagKey<Block>> blacklistedBlockTags = new HashSet<>();
    private static final Set<EntityType<?>> blacklistedEntityTypes = new HashSet<>();
    private static final Set<TagKey<EntityType<?>>> blacklistedEntityTags = new HashSet<>();

    static {
        BUILDER.comment("RideEverything General Configuration").push("general");

        ALLOW_BLOCK_RIDING = BUILDER.comment("Allow players to ride on blocks. Set to false to only allow mob riding.").define("allow_block_riding", true);

        ALLOW_PLAYER_RIDING = BUILDER.comment("Allow players to ride on other players.").define("allow_player_riding", true);

        BUILDER.pop();

        BUILDER.comment("Blacklist Configuration", "Supports both IDs and tags. Use '#' prefix for tags.", "Examples: 'minecraft:chest', '#minecraft:doors', 'minecraft:villager', '#minecraft:raiders'").push("blacklist");

        BLOCK_BLACKLIST = BUILDER.comment("Blocks that cannot be ridden. Format: 'modid:block_name' or '#modid:tag_name'").defineList("block_blacklist", new ArrayList<>(), obj -> obj instanceof String);

        ENTITY_BLACKLIST = BUILDER.comment("Entities that cannot be ridden. Format: 'modid:entity_name' or '#modid:tag_name'").defineList("entity_blacklist", new ArrayList<>(), obj -> obj instanceof String);

        BUILDER.pop();

        BUILDER.comment("RideEverything Flying Entity Configuration").push("flying_entities");

        ADDITIONAL_FLYING_ENTITIES = BUILDER.comment("Additional flying entity types, format: 'modid:entity_name'").worldRestart().defineList("additional_flying_entities", new ArrayList<>(), obj -> obj instanceof String);

        EXCLUDED_FLYING_ENTITIES = BUILDER.comment("Excluded entity types, format: 'modid:entity_name' (can override default behavior)").worldRestart().defineList("excluded_flying_entities", new ArrayList<>(), obj -> obj instanceof String);

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
        blacklistedBlockIds.clear();
        blacklistedBlockTags.clear();
        blacklistedEntityTypes.clear();
        blacklistedEntityTags.clear();

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

        for (String entry : BLOCK_BLACKLIST.get()) {
            try {
                if (entry.startsWith("#")) {
                    ResourceLocation tagId = new ResourceLocation(entry.substring(1));
                    blacklistedBlockTags.add(TagKey.create(Registries.BLOCK, tagId));
                } else {
                    blacklistedBlockIds.add(new ResourceLocation(entry));
                }
            } catch (Exception e) {
            }
        }

        for (String entry : ENTITY_BLACKLIST.get()) {
            try {
                if (entry.startsWith("#")) {
                    ResourceLocation tagId = new ResourceLocation(entry.substring(1));
                    blacklistedEntityTags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                } else {
                    ResourceLocation entityId = new ResourceLocation(entry);
                    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
                    if (entityType != null) {
                        blacklistedEntityTypes.add(entityType);
                    }
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

    public static boolean isBlockRidingAllowed() {
        return ALLOW_BLOCK_RIDING.get();
    }

    public static boolean isPlayerRidingAllowed() {
        return ALLOW_PLAYER_RIDING.get();
    }

    public static boolean isBlockBlacklisted(BlockState state) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && blacklistedBlockIds.contains(blockId)) {
            return true;
        }
        for (TagKey<Block> tag : blacklistedBlockTags) {
            if (state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEntityBlacklisted(EntityType<?> entityType) {
        if (blacklistedEntityTypes.contains(entityType)) {
            return true;
        }
        for (TagKey<EntityType<?>> tag : blacklistedEntityTags) {
            if (entityType.is(tag)) {
                return true;
            }
        }
        return false;
    }
}