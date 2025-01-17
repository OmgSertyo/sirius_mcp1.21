package net.minecraft.client.renderer.entity.layers;

import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperPowerLayer extends EnergySwirlLayer<Creeper, CreeperModel<Creeper>> {
    private static final ResourceLocation POWER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    public CreeperModel<Creeper> model;
    public ResourceLocation customTextureLocation;

    public CreeperPowerLayer(RenderLayerParent<Creeper, CreeperModel<Creeper>> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new CreeperModel<>(pModelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
    }

    @Override
    protected float xOffset(float pTickCount) {
        return pTickCount * 0.01F;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return this.customTextureLocation != null ? this.customTextureLocation : POWER_LOCATION;
    }

    @Override
    protected EntityModel<Creeper> model() {
        return this.model;
    }
}