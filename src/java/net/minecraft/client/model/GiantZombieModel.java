package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Giant;

public class GiantZombieModel extends AbstractZombieModel<Giant> {
    public GiantZombieModel(ModelPart pRoot) {
        super(pRoot);
    }

    public boolean isAggressive(Giant pEntity) {
        return false;
    }
}