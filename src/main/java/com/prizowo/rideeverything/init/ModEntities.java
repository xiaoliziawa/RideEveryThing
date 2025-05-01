package com.prizowo.rideeverything.init;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "rideeverything");

    public static final Supplier<EntityType<BlockSeatEntity>> BLOCK_SEAT = ENTITIES.register("block_seat",
            () -> EntityType.Builder.of(BlockSeatEntity::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .build("block_seat"));
}
