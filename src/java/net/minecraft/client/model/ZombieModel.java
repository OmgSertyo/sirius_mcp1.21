package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieModel<T extends Zombie> extends AbstractZombieModel<T> {
    public ZombieModel(ModelPart pRoot) {
        super(pRoot);
    }

    public boolean isAggressive(T pEntity) {
        return pEntity.isAggressive();
    }
}