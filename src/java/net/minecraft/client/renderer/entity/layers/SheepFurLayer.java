package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.optifine.Config;
import net.optifine.CustomColors;

public class SheepFurLayer extends RenderLayer<Sheep, SheepModel<Sheep>> {
    private static final ResourceLocation SHEEP_FUR_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep_fur.png");
    public SheepFurModel<Sheep> model;

    public SheepFurLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new SheepFurModel<>(pModelSet.bakeLayer(ModelLayers.SHEEP_FUR));
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        Sheep pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTicks,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        if (!pLivingEntity.isSheared()) {
            if (pLivingEntity.isInvisible()) {
                Minecraft minecraft = Minecraft.getInstance();
                boolean flag = minecraft.shouldEntityAppearGlowing(pLivingEntity);
                if (flag) {
                    this.getParentModel().copyPropertiesTo(this.model);
                    this.model.prepareMobModel(pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTicks);
                    this.model.setupAnim(pLivingEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
                    VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.outline(SHEEP_FUR_LOCATION));
                    this.model.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F), -16777216);
                }
            } else {
                int j1;
                if (pLivingEntity.hasCustomName() && "jeb_".equals(pLivingEntity.getName().getString())) {
                    int k1 = 25;
                    int l1 = pLivingEntity.tickCount / 25 + pLivingEntity.getId();
                    int i = DyeColor.values().length;
                    int j = l1 % i;
                    int k = (l1 + 1) % i;
                    float f = ((float)(pLivingEntity.tickCount % 25) + pPartialTicks) / 25.0F;
                    int l = Sheep.getColor(DyeColor.byId(j));
                    int i1 = Sheep.getColor(DyeColor.byId(k));
                    if (Config.isCustomColors()) {
                        l = CustomColors.getSheepColors(DyeColor.byId(j), l);
                        i1 = CustomColors.getSheepColors(DyeColor.byId(k), i1);
                    }

                    j1 = FastColor.ARGB32.lerp(f, l, i1);
                } else {
                    j1 = Sheep.getColor(pLivingEntity.getColor());
                    if (Config.isCustomColors()) {
                        j1 = CustomColors.getSheepColors(pLivingEntity.getColor(), j1);
                    }
                }

                coloredCutoutModelCopyLayerRender(
                    this.getParentModel(),
                    this.model,
                    SHEEP_FUR_LOCATION,
                    pPoseStack,
                    pBuffer,
                    pPackedLight,
                    pLivingEntity,
                    pLimbSwing,
                    pLimbSwingAmount,
                    pAgeInTicks,
                    pNetHeadYaw,
                    pHeadPitch,
                    pPartialTicks,
                    j1
                );
            }
        }
    }
}