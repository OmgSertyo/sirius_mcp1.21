package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

public class PartDefinition {
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();
    private String name;

    PartDefinition(List<CubeDefinition> pCubes, PartPose pPartPose) {
        this.cubes = pCubes;
        this.partPose = pPartPose;
    }

    public PartDefinition addOrReplaceChild(String pName, CubeListBuilder pCubes, PartPose pPartPose) {
        PartDefinition partdefinition = new PartDefinition(pCubes.getCubes(), pPartPose);
        partdefinition.setName(pName);
        PartDefinition partdefinition1 = this.children.put(pName, partdefinition);
        if (partdefinition1 != null) {
            partdefinition.children.putAll(partdefinition1.children);
        }

        return partdefinition;
    }

    public ModelPart bake(int pTexWidth, int pTexHeight) {
        Object2ObjectArrayMap<String, ModelPart> object2objectarraymap = this.children
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    p_171590_2_ -> p_171590_2_.getValue().bake(pTexWidth, pTexHeight),
                    (p_171594_0_, p_171594_1_) -> p_171594_0_,
                    Object2ObjectArrayMap::new
                )
            );
        List<ModelPart.Cube> list = this.cubes
            .stream()
            .map(p_171586_2_ -> p_171586_2_.bake(pTexWidth, pTexHeight))
            .collect(ImmutableList.toImmutableList());
        ModelPart modelpart = new ModelPart(list, object2objectarraymap);
        modelpart.setInitialPose(this.partPose);
        modelpart.loadPose(this.partPose);
        modelpart.setName(this.name);
        return modelpart;
    }

    public PartDefinition getChild(String pName) {
        return this.children.get(pName);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}