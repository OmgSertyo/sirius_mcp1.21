package net.minecraft.client.sounds;

import net.minecraft.util.RandomSource;

public interface Weighted<T> {
    int getWeight();

    T getSound(RandomSource pRandomSource);

    void preloadIfRequired(SoundEngine pEngine);
}