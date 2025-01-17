package net.minecraft.client.particle;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TerrainParticle extends TextureSheetParticle {
    private final BlockPos pos;
    private final float uo;
    private final float vo;

    public TerrainParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, BlockState pState
    ) {
        this(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pState, BlockPos.containing(pX, pY, pZ));
    }

    public TerrainParticle(
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed,
        BlockState pState,
        BlockPos pPos
    ) {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        this.pos = pPos;
        this.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(pState));
        this.gravity = 1.0F;
        this.rCol = 0.6F;
        this.gCol = 0.6F;
        this.bCol = 0.6F;
        if (!pState.is(Blocks.GRASS_BLOCK)) {
            int i = Minecraft.getInstance().getBlockColors().getColor(pState, pLevel, pPos, 0);
            this.rCol *= (float)(i >> 16 & 0xFF) / 255.0F;
            this.gCol *= (float)(i >> 8 & 0xFF) / 255.0F;
            this.bCol *= (float)(i & 0xFF) / 255.0F;
        }

        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    @Override
    public int getLightColor(float pPartialTick) {
        int i = super.getLightColor(pPartialTick);
        return i == 0 && this.level.hasChunkAt(this.pos) ? LevelRenderer.getLightColor(this.level, this.pos) : i;
    }

    @Nullable
    static TerrainParticle createTerrainParticle(
        BlockParticleOption pType,
        ClientLevel pLevel,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        BlockState blockstate = pType.getState();
        return !blockstate.isAir() && !blockstate.is(Blocks.MOVING_PISTON) && blockstate.shouldSpawnTerrainParticles()
            ? new TerrainParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, blockstate)
            : null;
    }

    public static class DustPillarProvider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption p_331644_,
            ClientLevel p_335147_,
            double p_334048_,
            double p_329502_,
            double p_331778_,
            double p_332962_,
            double p_334493_,
            double p_329453_
        ) {
            Particle particle = TerrainParticle.createTerrainParticle(p_331644_, p_335147_, p_334048_, p_329502_, p_331778_, p_332962_, p_334493_, p_329453_);
            if (particle != null) {
                particle.setParticleSpeed(
                    p_335147_.random.nextGaussian() / 30.0, p_334493_ + p_335147_.random.nextGaussian() / 2.0, p_335147_.random.nextGaussian() / 30.0
                );
                particle.setLifetime(p_335147_.random.nextInt(20) + 20);
            }

            return particle;
        }
    }

    public static class Provider implements ParticleProvider<BlockParticleOption> {
        @Nullable
        public Particle createParticle(
            BlockParticleOption pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return TerrainParticle.createTerrainParticle(pType, pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        }
    }
}