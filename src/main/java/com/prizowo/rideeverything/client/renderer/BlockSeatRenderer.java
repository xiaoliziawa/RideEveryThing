package com.prizowo.rideeverything.client.renderer;

import com.prizowo.rideeverything.entity.BlockSeatEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class BlockSeatRenderer extends EntityRenderer<BlockSeatEntity> {
    public BlockSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BlockSeatEntity entity) {
        return new ResourceLocation("textures/block/air.png");
    }
}
