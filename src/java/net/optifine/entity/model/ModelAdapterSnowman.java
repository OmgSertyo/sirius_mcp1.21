package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.SnowGolemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.SnowGolem;

public class ModelAdapterSnowman extends ModelAdapter {
    public ModelAdapterSnowman() {
        super(EntityType.SNOW_GOLEM, "snow_golem", 0.5F);
    }

    @Override
    public Model makeModel() {
        return new SnowGolemModel(bakeModelLayer(ModelLayers.SNOW_GOLEM));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        if (!(model instanceof SnowGolemModel snowgolemmodel)) {
            return null;
        } else if (modelPart.equals("body")) {
            return snowgolemmodel.root().getChildModelDeep("upper_body");
        } else if (modelPart.equals("body_bottom")) {
            return snowgolemmodel.root().getChildModelDeep("lower_body");
        } else if (modelPart.equals("head")) {
            return snowgolemmodel.root().getChildModelDeep("head");
        } else if (modelPart.equals("right_hand")) {
            return snowgolemmodel.root().getChildModelDeep("right_arm");
        } else if (modelPart.equals("left_hand")) {
            return snowgolemmodel.root().getChildModelDeep("left_arm");
        } else {
            return modelPart.equals("root") ? snowgolemmodel.root() : null;
        }
    }

    @Override
    public String[] getModelRendererNames() {
        return new String[]{"body", "body_bottom", "head", "right_hand", "left_hand", "root"};
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index) {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        SnowGolemRenderer snowgolemrenderer = new SnowGolemRenderer(entityrenderdispatcher.getContext());
        snowgolemrenderer.model = (SnowGolemModel<SnowGolem>)modelBase;
        snowgolemrenderer.shadowRadius = shadowSize;
        return snowgolemrenderer;
    }
}