package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;

public interface ParticleProvider<T extends ParticleOptions> {
    @Nullable
    Particle createParticle(
        T pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    );

    public interface Sprite<T extends ParticleOptions> {
        @Nullable
        TextureSheetParticle createParticle(
            T pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
        );
    }
}