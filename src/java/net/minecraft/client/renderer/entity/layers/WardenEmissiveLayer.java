package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;
import net.optifine.Config;
import net.optifine.shaders.Shaders;

public class WardenEmissiveLayer<T extends Warden, M extends WardenModel<T>> extends RenderLayer<T, M> {
    private final ResourceLocation texture;
    private final WardenEmissiveLayer.AlphaFunction<T> alphaFunction;
    private final WardenEmissiveLayer.DrawSelector<T, M> drawSelector;

    public WardenEmissiveLayer(
        RenderLayerParent<T, M> pRenderer,
        ResourceLocation pTexture,
        WardenEmissiveLayer.AlphaFunction<T> pAlphaFunction,
        WardenEmissiveLayer.DrawSelector<T, M> pDrawSelector
    ) {
        super(pRenderer);
        this.texture = pTexture;
        this.alphaFunction = pAlphaFunction;
        this.drawSelector = pDrawSelector;
    }

    public void render(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        T pLivingEntity,
        float pLimbSwing,
        float pLimbSwingAmount,
        float pPartialTick,
        float pAgeInTicks,
        float pNetHeadYaw,
        float pHeadPitch
    ) {
        if (!pLivingEntity.isInvisible()) {
            if (Config.isShaders()) {
                Shaders.beginSpiderEyes();
            }

            Config.getRenderGlobal().renderOverlayEyes = true;
            this.onlyDrawSelectedParts();
            VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.entityTranslucentEmissive(this.texture));
            float f = this.alphaFunction.apply(pLivingEntity, pPartialTick, pAgeInTicks);
            int i = FastColor.ARGB32.color(Mth.floor(f * 255.0F), 255, 255, 255);
            this.getParentModel().renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F), i);
            this.resetDrawForAllParts();
            Config.getRenderGlobal().renderOverlayEyes = false;
            if (Config.isShaders()) {
                Shaders.endSpiderEyes();
            }
        }
    }

    private void onlyDrawSelectedParts() {
        List<ModelPart> list = this.drawSelector.getPartsToDraw(this.getParentModel());
        this.getParentModel().root().getAllParts().forEach(partIn -> partIn.skipDraw = true);
        list.forEach(partIn -> partIn.skipDraw = false);
        list.forEach(partIn -> {
            for (Entry<String, ModelPart> entry : partIn.children.entrySet()) {
                if (entry.getKey().startsWith("CEM-")) {
                    entry.getValue().skipDraw = false;
                }
            }
        });
    }

    private void resetDrawForAllParts() {
        this.getParentModel().root().getAllParts().forEach(partIn -> partIn.skipDraw = false);
    }

    public interface AlphaFunction<T extends Warden> {
        float apply(T pLivingEntity, float pPartialTick, float pAgeInTicks);
    }

    public interface DrawSelector<T extends Warden, M extends EntityModel<T>> {
        List<ModelPart> getPartsToDraw(M pParentModel);
    }
}