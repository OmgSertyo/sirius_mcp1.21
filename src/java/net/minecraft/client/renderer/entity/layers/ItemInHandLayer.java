package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.model.AttachmentType;

public class ItemInHandLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> extends RenderLayer<T, M> {
    private final ItemInHandRenderer itemInHandRenderer;

    public ItemInHandLayer(RenderLayerParent<T, M> pRenderer, ItemInHandRenderer pItemInHandRenderer) {
        super(pRenderer);
        this.itemInHandRenderer = pItemInHandRenderer;
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        T pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTicks,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        boolean flag = pLivingEntity.getMainArm() == HumanoidArm.RIGHT;
        ItemStack itemstack = flag ? pLivingEntity.getOffhandItem() : pLivingEntity.getMainHandItem();
        ItemStack itemstack1 = flag ? pLivingEntity.getMainHandItem() : pLivingEntity.getOffhandItem();
        if (!itemstack.isEmpty() || !itemstack1.isEmpty()) {
            pPoseStack.pushPose();
            if (this.getParentModel().young) {
                float f = 0.5F;
                pPoseStack.translate(0.0F, 0.75F, 0.0F);
                pPoseStack.scale(0.5F, 0.5F, 0.5F);
            }

            this.renderArmWithItem(pLivingEntity, itemstack1, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, pPoseStack, pBuffer, pPackedLight);
            this.renderArmWithItem(pLivingEntity, itemstack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, pPoseStack, pBuffer, pPackedLight);
            pPoseStack.popPose();
        }
    }

    protected void renderArmWithItem(
        LivingEntity pLivingEntity,
        ItemStack pItemStack,
        ItemDisplayContext pDisplayContext,
        HumanoidArm pArm,
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight
    ) {
        if (!pItemStack.isEmpty()) {
            pPoseStack.pushPose();
            if (!this.applyAttachmentTransform(pArm, pPoseStack)) {
                this.getParentModel().translateToHand(pArm, pPoseStack);
            }

            pPoseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean flag = pArm == HumanoidArm.LEFT;
            pPoseStack.translate((float)(flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            this.itemInHandRenderer.renderItem(pLivingEntity, pItemStack, pDisplayContext, flag, pPoseStack, pBuffer, pPackedLight);
            pPoseStack.popPose();
        }
    }

    private boolean applyAttachmentTransform(HumanoidArm armIn, PoseStack matrixStackIn) {
        if (!CustomEntityModels.isActive()) {
            return false;
        } else {
            ModelPart modelpart = this.getRoot();
            if (modelpart == null) {
                return false;
            } else {
                AttachmentType attachmenttype = armIn == HumanoidArm.LEFT ? AttachmentType.LEFT_HANDHELD_ITEM : AttachmentType.RIGHT_HANDHELD_ITEM;
                return modelpart.applyAttachmentTransform(attachmenttype, matrixStackIn);
            }
        }
    }

    private ModelPart getRoot() {
        ArmedModel armedmodel = this.getParentModel();
        if (armedmodel instanceof HumanoidModel humanoidmodel) {
            return humanoidmodel.body.getParent();
        } else {
            return armedmodel instanceof HierarchicalModel hierarchicalmodel ? hierarchicalmodel.root() : null;
        }
    }
}