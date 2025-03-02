package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Blaze;

public class BlazeRenderer extends MobRenderer<Blaze, BlazeModel<Blaze>> {
    private static final ResourceLocation BLAZE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/blaze.png");

    public BlazeRenderer(EntityRendererProvider.Context p_173933_) {
        super(p_173933_, new BlazeModel<>(p_173933_.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    protected int getBlockLightLevel(Blaze pEntity, BlockPos pPos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(Blaze pEntity) {
        return BLAZE_LOCATION;
    }
}