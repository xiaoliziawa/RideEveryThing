package com.prizowo.rideeverything.init;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "rideeverything");

    public static final RegistryObject<EntityType<BlockSeatEntity>> BLOCK_SEAT = ENTITIES.register("block_seat",
            () -> EntityType.Builder.of(BlockSeatEntity::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .build("block_seat"));
}
