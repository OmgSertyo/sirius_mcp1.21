package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;

public class BreezeWindLayer extends RenderLayer<Breeze, BreezeModel<Breeze>> {
    private ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
    private BreezeModel<Breeze> model;

    public BreezeWindLayer(EntityRendererProvider.Context pContext, RenderLayerParent<Breeze, BreezeModel<Breeze>> pRenderer) {
        super(pRenderer);
        this.model = new BreezeModel<>(pContext.bakeLayer(ModelLayers.BREEZE_WIND));
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        Breeze pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTick,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        float f = (float)pLivingEntity.tickCount + pPartialTick;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.breezeWind(this.TEXTURE_LOCATION, this.xOffset(f) % 1.0F, 0.0F));
        this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        BreezeRenderer.enable(this.model, this.model.wind()).renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY);
    }

    private float xOffset(float pTickCount) {
        return pTickCount * 0.02F;
    }

    public void setModel(BreezeModel<Breeze> model) {
        this.model = model;
    }

    public void setTextureLocation(ResourceLocation textureLocation) {
        this.TEXTURE_LOCATION = textureLocation;
    }
}