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
        this.noCulling = true;
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D; // 增加渲染距离
    }
    
    @Override
    public boolean isAlwaysTicking() {
        return true; // 确保实体始终 tick
    }

    @Override
    public void tick() {
        super.tick();
        
        // 只有在方块无效时才移除实体，不检查是否有乘客（避免时序问题）
        if (attachedBlock != null && !isBlockValid()) {
            this.ejectPassengers();
            this.discard();
        }
        
        // 如果没有乘客且实体存在时间超过 100 ticks（5秒），则移除（避免孤儿实体）
        if (!this.isVehicle() && this.tickCount > 100) {
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
            double yOffset = 0.8D;
            
            if (state.getBlock() instanceof StairBlock) {
                yOffset = state.getValue(StairBlock.HALF) == Half.TOP ? 0.8D : 0.3D;
            }
            
            if (state.getBlock() instanceof SlabBlock) {
                SlabType slabType = state.getValue(SlabBlock.TYPE);
                yOffset = switch (slabType) {
                    case TOP -> 0.8D;
                    case BOTTOM -> 0.3D;
                    case DOUBLE -> 0.8D;
                };
            }
            
            return new Vec3(0, yOffset, 0);
        }
        return super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
    }

    @Override
    protected void addPassenger(@NotNull Entity passenger) {
        super.addPassenger(passenger);
        if (attachedBlock != null) {
            BlockState state = level().getBlockState(attachedBlock);
            double yOffset = attachedBlock.getY() + 1.8; // 默认高度
            
            // 处理楼梯
            if (state.getBlock() instanceof StairBlock) {
                yOffset = state.getValue(StairBlock.HALF) == Half.TOP ? 
                         attachedBlock.getY() + 1.8 : // TOP 楼梯
                         attachedBlock.getY() + 0.8;  // BOTTOM 楼梯，坐在一半高度
                         
                double horizontalOffset = 0.25;
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
                    case TOP -> attachedBlock.getY() + 1.8;    // 上台阶
                    case BOTTOM -> attachedBlock.getY() + 0.8; // 下台阶，坐在一半高度
                    case DOUBLE -> attachedBlock.getY() + 1.8; // 双台阶
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
            Vec3 safePos = findSafeDismountPosition(passenger);
            passenger.setPos(safePos.x, safePos.y, safePos.z);
        }
        super.removePassenger(passenger);
    }
    
    @Override
    public void ejectPassengers() {
        for (Entity passenger : getPassengers()) {
            if (attachedBlock != null) {
                Vec3 safePos = findSafeDismountPosition(passenger);
                passenger.setPos(safePos.x, safePos.y, safePos.z);
            }
        }
        super.ejectPassengers();
    }
    
    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull net.minecraft.world.entity.LivingEntity passenger) {
        if (attachedBlock != null) {
            return findSafeDismountPosition(passenger);
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    private Vec3 findSafeDismountPosition(Entity passenger) {
        BlockState state = level().getBlockState(attachedBlock);
        double baseY = attachedBlock.getY() + 1.0;
        
        if (state.getBlock() instanceof StairBlock) {
            Half half = state.getValue(StairBlock.HALF);
            baseY = half == Half.TOP ? attachedBlock.getY() + 1.0 : attachedBlock.getY() + 0.5;
        } else if (state.getBlock() instanceof SlabBlock) {
            SlabType slabType = state.getValue(SlabBlock.TYPE);
            baseY = switch (slabType) {
                case TOP -> attachedBlock.getY() + 1.0;
                case BOTTOM -> attachedBlock.getY() + 0.5;
                case DOUBLE -> attachedBlock.getY() + 1.0;
            };
        }
        float yRot = passenger.getYRot();
        double radians = Math.toRadians(yRot);
        double frontX = -Math.sin(radians) * 2.0;
        double frontZ = Math.cos(radians) * 2.0;
        Vec3 frontPos = new Vec3(
                attachedBlock.getX() + 0.5 + frontX,
                baseY,
                attachedBlock.getZ() + 0.5 + frontZ
        );
        if (isSafePosition(frontPos, passenger)) {
            return frontPos;
        }
        
        double[] distances = {2.0, 2.5, 3.0};
        double[] angles = {0, 45, 90, 135, 180, 225, 270, 315};
        for (double distance : distances) {
            for (double angle : angles) {
                double angleRad = Math.toRadians(angle);
                double xOffset = Math.sin(angleRad) * distance;
                double zOffset = Math.cos(angleRad) * distance;
                Vec3 testPos = new Vec3(
                        attachedBlock.getX() + 0.5 + xOffset,
                        baseY,
                        attachedBlock.getZ() + 0.5 + zOffset
                );

                if (isSafePosition(testPos, passenger)) {
                    return testPos;
                }
            }
        }
        
        for (int i = 1; i <= 5; i++) {
            Vec3 highPos = new Vec3(
                attachedBlock.getX() + 0.5, 
                baseY + i, 
                attachedBlock.getZ() + 0.5
            );
            if (isSafePosition(highPos, passenger)) {
                return highPos;
            }
        }
        
        return new Vec3(attachedBlock.getX() + 0.5, baseY, attachedBlock.getZ() + 0.5);
    }

    private boolean isSafePosition(Vec3 pos, Entity passenger) {
        BlockPos feetPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        BlockPos headPos = feetPos.above();
        BlockPos belowPos = feetPos.below();
        
        BlockState belowState = level().getBlockState(belowPos);
        if (belowState.isAir()) {
            return false;
        }
        
        BlockState feetState = level().getBlockState(feetPos);
        if (!feetState.isAir() && feetState.canOcclude()) {
            return false;
        }
        
        BlockState headState = level().getBlockState(headPos);
        if (!headState.isAir() && headState.canOcclude()) {
            return false;
        }
        
        double minX = feetPos.getX();
        double maxX = feetPos.getX() + 1.0;
        double minZ = feetPos.getZ();
        double maxZ = feetPos.getZ() + 1.0;
        
        if (pos.x <= minX + 0.3 || pos.x >= maxX - 0.3 ||
            pos.z <= minZ + 0.3 || pos.z >= maxZ - 0.3) {
            return !feetState.canOcclude();
        }
        
        return true;
    }
}
