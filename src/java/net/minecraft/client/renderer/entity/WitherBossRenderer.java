package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.boss.wither.WitherBoss;

public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherBossModel<WitherBoss>> {
    private static final ResourceLocation WITHER_INVULNERABLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation WITHER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither.png");

    public WitherBossRenderer(EntityRendererProvider.Context p_174445_) {
        super(p_174445_, new WitherBossModel<>(p_174445_.bakeLayer(ModelLayers.WITHER)), 1.0F);
        this.addLayer(new WitherArmorLayer(this, p_174445_.getModelSet()));
    }

    protected int getBlockLightLevel(WitherBoss pEntity, BlockPos pPos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(WitherBoss pEntity) {
        int i = pEntity.getInvulnerableTicks();
        return i > 0 && (i > 80 || i / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
    }

    protected void scale(WitherBoss pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        float f = 2.0F;
        int i = pLivingEntity.getInvulnerableTicks();
        if (i > 0) {
            f -= ((float)i - pPartialTickTime) / 220.0F * 0.5F;
        }

        pPoseStack.scale(f, f, f);
    }
}