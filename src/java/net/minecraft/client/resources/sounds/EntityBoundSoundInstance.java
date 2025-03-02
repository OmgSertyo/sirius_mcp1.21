package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class EntityBoundSoundInstance extends AbstractTickableSoundInstance {
    private final Entity entity;

    public EntityBoundSoundInstance(SoundEvent pSoundEvent, SoundSource pSource, float pVolume, float pPitch, Entity pEntity, long pSeed) {
        super(pSoundEvent, pSource, RandomSource.create(pSeed));
        this.volume = pVolume;
        this.pitch = pPitch;
        this.entity = pEntity;
        this.x = (double)((float)this.entity.getX());
        this.y = (double)((float)this.entity.getY());
        this.z = (double)((float)this.entity.getZ());
    }

    @Override
    public boolean canPlaySound() {
        return !this.entity.isSilent();
    }

    @Override
    public void tick() {
        if (this.entity.isRemoved()) {
            this.stop();
        } else {
            this.x = (double)((float)this.entity.getX());
            this.y = (double)((float)this.entity.getY());
            this.z = (double)((float)this.entity.getZ());
        }
    }
}