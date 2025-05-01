package com.prizowo.rideeverything.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.network.syncher.SynchedEntityData;

public class BlockSeatEntity extends Entity {
    private BlockPos attachedBlock;

    public BlockSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
    }

    public void setAttachedBlock(BlockPos pos) {
        this.attachedBlock = pos;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains("AttachedBlockX")) {
            int x = tag.getInt("AttachedBlockX");
            int y = tag.getInt("AttachedBlockY");
            int z = tag.getInt("AttachedBlockZ");
            this.attachedBlock = new BlockPos(x, y, z);
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (attachedBlock != null) {
            tag.putInt("AttachedBlockX", attachedBlock.getX());
            tag.putInt("AttachedBlockY", attachedBlock.getY());
            tag.putInt("AttachedBlockZ", attachedBlock.getZ());
        }
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.isVehicle() || (attachedBlock != null && !isBlockValid())) {
            this.ejectPassengers();
            this.discard();
        }
    }

    private boolean isBlockValid() {
        if (attachedBlock == null) return false;
        BlockState state = level().getBlockState(attachedBlock);
        return !state.isAir();
    }

    @Override
    public void move(@NotNull MoverType type, @NotNull Vec3 pos) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity entity, @NotNull EntityDimensions dimensions, float partialTick) {
        if (attachedBlock != null) {
            BlockState state = level().getBlockState(attachedBlock);
            double yOffset = 0.4D;
            
            // 处理楼梯
            if (state.getBlock() instanceof StairBlock) {
                yOffset = state.getValue(StairBlock.HALF) == Half.TOP ? 1.2D : 0.5D;
            }
            
            // 处理台阶
            if (state.getBlock() instanceof SlabBlock) {
                SlabType slabType = state.getValue(SlabBlock.TYPE);
                yOffset = switch (slabType) {
                    case TOP, DOUBLE -> 1.2D;
                    case BOTTOM -> 0.5D;
                };
            }
            
            return new Vec3(0, yOffset - dimensions.height(), 0);
        }
        return super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger) {
        super.addPassenger(passenger);
        if (attachedBlock != null) {
            BlockState state = level().getBlockState(attachedBlock);
            double yOffset = attachedBlock.getY() + 0.4;
            
            // 处理楼梯
            if (state.getBlock() instanceof StairBlock) {
                yOffset = state.getValue(StairBlock.HALF) == Half.TOP ? 
                         attachedBlock.getY() + 1.1 :
                         attachedBlock.getY() + 0.5;
                         
                double horizontalOffset = 0.25; // 减小水平偏移量
                switch(state.getValue(StairBlock.FACING)) {
                    case NORTH -> passenger.setPos(
                        attachedBlock.getX() + 0.5,
                        yOffset,
                        attachedBlock.getZ() + 0.5 + horizontalOffset
                    );
                    case SOUTH -> passenger.setPos(
                        attachedBlock.getX() + 0.5,
                        yOffset,
                        attachedBlock.getZ() + 0.5 - horizontalOffset
                    );
                    case EAST -> passenger.setPos(
                        attachedBlock.getX() + 0.5 - horizontalOffset,
                        yOffset,
                        attachedBlock.getZ() + 0.5
                    );
                    case WEST -> passenger.setPos(
                        attachedBlock.getX() + 0.5 + horizontalOffset,
                        yOffset,
                        attachedBlock.getZ() + 0.5
                    );
                }
                return;
            }
            
            // 处理台阶
            if (state.getBlock() instanceof SlabBlock) {
                SlabType slabType = state.getValue(SlabBlock.TYPE);
                yOffset = switch (slabType) {
                    case TOP, DOUBLE -> attachedBlock.getY() + 1.1;
                    case BOTTOM -> attachedBlock.getY() + 0.5;
                };
            }
            
            passenger.setPos(
                attachedBlock.getX() + 0.5,
                yOffset,
                attachedBlock.getZ() + 0.5
            );
        }
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        if (attachedBlock != null) {
            BlockState state = level().getBlockState(attachedBlock);
            double yOffset = attachedBlock.getY() + 1.0;
            
            // 根据方块类型调整下车位置
            if (state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock) {
                yOffset = attachedBlock.getY() + 1.0;
            }
            
            // 计算水平偏移
            double offsetX;
            double offsetZ;
            float passengerYRot = passenger.getYRot();
            float angle = (float) Math.toRadians(passengerYRot);
            double horizontalOffset = 1.0;
            offsetX = -Math.sin(angle) * horizontalOffset;
            offsetZ = Math.cos(angle) * horizontalOffset;
            
            passenger.setPos(
                attachedBlock.getX() + 0.5 + offsetX,
                yOffset,
                attachedBlock.getZ() + 0.5 + offsetZ
            );
        }
        super.removePassenger(passenger);
    }
}
