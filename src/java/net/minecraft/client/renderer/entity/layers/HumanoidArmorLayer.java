package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.util.TextureUtils;

public class HumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final Map<String, ResourceLocation> ARMOR_LOCATION_CACHE = Maps.newHashMap();
    private final A innerModel;
    private final A outerModel;
    private final TextureAtlas armorTrimAtlas;

    public HumanoidArmorLayer(RenderLayerParent<T, M> pRenderer, A pInnerModel, A pOuterModel, ModelManager pModelManager) {
        super(pRenderer);
        this.innerModel = pInnerModel;
        this.outerModel = pOuterModel;
        this.armorTrimAtlas = pModelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET);
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
        this.renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.CHEST, pPackedLight, this.getArmorModel(EquipmentSlot.CHEST));
        this.renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.LEGS, pPackedLight, this.getArmorModel(EquipmentSlot.LEGS));
        this.renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.FEET, pPackedLight, this.getArmorModel(EquipmentSlot.FEET));
        this.renderArmorPiece(pPoseStack, pBuffer, pLivingEntity, EquipmentSlot.HEAD, pPackedLight, this.getArmorModel(EquipmentSlot.HEAD));
    }

    private void renderArmorPiece(PoseStack pPoseStack, MultiBufferSource pBufferSource, T pLivingEntity, EquipmentSlot pSlot, int pPackedLight, A pModel) {
        ItemStack itemstack = pLivingEntity.getItemBySlot(pSlot);
        if (itemstack.getItem() instanceof ArmorItem armoritem && armoritem.getEquipmentSlot() == pSlot) {
            this.getParentModel().copyPropertiesTo(pModel);
            this.setPartVisibility(pModel, pSlot);
            Model model = this.getArmorModelHook(pLivingEntity, itemstack, pSlot, pModel);
            boolean flag = this.usesInnerModel(pSlot);
            ArmorMaterial armormaterial = armoritem.getMaterial().value();
            int i = itemstack.is(ItemTags.DYEABLE) ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(itemstack, -6265536)) : -1;

            for (ArmorMaterial.Layer armormaterial$layer : armormaterial.layers()) {
                int j = armormaterial$layer.dyeable() ? i : -1;
                ResourceLocation resourcelocation = armormaterial$layer.texture(flag);
                if (Reflector.ForgeHooksClient_getArmorTexture.exists()) {
                    resourcelocation = (ResourceLocation)Reflector.ForgeHooksClient_getArmorTexture
                        .call(pLivingEntity, itemstack, pSlot, armormaterial$layer, flag);
                }

                if (Config.isCustomItems()) {
                    resourcelocation = CustomItems.getCustomArmorTexture(itemstack, pSlot, armormaterial$layer.getSuffix(), resourcelocation);
                }

                this.renderModel(pPoseStack, pBufferSource, pPackedLight, model, j, resourcelocation);
            }

            ArmorTrim armortrim = itemstack.get(DataComponents.TRIM);
            if (armortrim != null) {
                this.renderTrim(armoritem.getMaterial(), pPoseStack, pBufferSource, pPackedLight, armortrim, model, flag);
            }

            if (itemstack.hasFoil()) {
                this.renderGlint(pPoseStack, pBufferSource, pPackedLight, model);
            }
        }
    }

    protected void setPartVisibility(A pModel, EquipmentSlot pSlot) {
        pModel.setAllVisible(false);
        switch (pSlot) {
            case HEAD:
                pModel.head.visible = true;
                pModel.hat.visible = true;
                break;
            case CHEST:
                pModel.body.visible = true;
                pModel.rightArm.visible = true;
                pModel.leftArm.visible = true;
                break;
            case LEGS:
                pModel.body.visible = true;
                pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
                break;
            case FEET:
                pModel.rightLeg.visible = true;
                pModel.leftLeg.visible = true;
        }
    }

    private void renderModel(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, A pModel, int pDyeColor, ResourceLocation pTextureLocation) {
        this.renderModel(pPoseStack, pBufferSource, pPackedLight, pModel, pDyeColor, pTextureLocation);
    }

    private void renderModel(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Model bipedModelIn, int colorIn, ResourceLocation suffixIn) {
        VertexConsumer vertexconsumer = bufferIn.getBuffer(RenderType.armorCutoutNoCull(suffixIn));
        bipedModelIn.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, OverlayTexture.NO_OVERLAY, colorIn);
    }

    private void renderTrim(
        Holder<ArmorMaterial> pArmorMaterial, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, ArmorTrim pTrim, A pModel, boolean pInnerTexture
    ) {
        this.renderTrim(pArmorMaterial, pPoseStack, pBufferSource, pPackedLight, pTrim, pModel, pInnerTexture);
    }

    private void renderTrim(
        Holder<ArmorMaterial> armorMaterialIn,
        PoseStack matrixStackIn,
        MultiBufferSource bufferIn,
        int packedLightIn,
        ArmorTrim trimIn,
        Model bipedModelIn,
        boolean isLegSlot
    ) {
        TextureAtlasSprite textureatlassprite = this.armorTrimAtlas.getSprite(isLegSlot ? trimIn.innerTexture(armorMaterialIn) : trimIn.outerTexture(armorMaterialIn));
        textureatlassprite = TextureUtils.getCustomSprite(textureatlassprite);
        VertexConsumer vertexconsumer = textureatlassprite.wrap(bufferIn.getBuffer(Sheets.armorTrimsSheet(trimIn.pattern().value().decal())));
        bipedModelIn.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, OverlayTexture.NO_OVERLAY);
    }

    private void renderGlint(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, A pModel) {
        this.renderGlint(pPoseStack, pBufferSource, pPackedLight, pModel);
    }

    private void renderGlint(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Model bipedModelIn) {
        bipedModelIn.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.armorEntityGlint()), packedLightIn, OverlayTexture.NO_OVERLAY);
    }

    private A getArmorModel(EquipmentSlot pSlot) {
        return this.usesInnerModel(pSlot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot pSlot) {
        return pSlot == EquipmentSlot.LEGS;
    }

    protected Model getArmorModelHook(T entity, ItemStack itemStack, EquipmentSlot slot, A model) {
        return (Model)(Reflector.ForgeHooksClient_getArmorModel.exists()
            ? (Model)Reflector.ForgeHooksClient_getArmorModel.call(entity, itemStack, slot, model)
            : model);
    }
}