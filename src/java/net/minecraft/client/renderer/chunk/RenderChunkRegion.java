package net.minecraft.client.renderer.chunk;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.model.data.ModelDataManager;
import net.optifine.override.ChunkCacheOF;

public class RenderChunkRegion implements BlockAndTintGetter {
    public static final int RADIUS = 1;
    public static final int SIZE = 3;
    private final int minChunkX;
    private final int minChunkZ;
    protected final RenderChunk[] chunks;
    protected final Level level;
    private SectionPos sectionPos;

    RenderChunkRegion(Level pLevel, int pMinChunkX, int pMinChunkZ, RenderChunk[] pChunks) {
        this(pLevel, pMinChunkX, pMinChunkZ, pChunks, null);
    }

    RenderChunkRegion(Level worldIn, int chunkStartXIn, int chunkStartYIn, RenderChunk[] chunksIn, SectionPos sectionPosIn) {
        this.level = worldIn;
        this.minChunkX = chunkStartXIn;
        this.minChunkZ = chunkStartYIn;
        this.chunks = chunksIn;
        this.sectionPos = sectionPosIn;
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ())).getBlockState(pPos);
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ())).getBlockState(pPos).getFluidState();
    }

    @Override
    public float getShade(Direction pDirection, boolean pShade) {
        return this.level.getShade(pDirection, pShade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ())).getBlockEntity(pPos);
    }

    public RenderChunk getChunk(int pX, int pZ) {
        return this.chunks[index(this.minChunkX, this.minChunkZ, pX, pZ)];
    }

    @Override
    public int getBlockTint(BlockPos pPos, ColorResolver pColorResolver) {
        return this.level.getBlockTint(pPos, pColorResolver);
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    public static int index(int pMinX, int pMinZ, int pX, int pZ) {
        return pX - pMinX + (pZ - pMinZ) * 3;
    }

    public Biome getBiome(BlockPos pos) {
        return this.level.getBiome(pos).value();
    }

    public LevelChunk getLevelChunk(int cx, int cz) {
        return this.getChunk(cx, cz).getChunk();
    }

    public ChunkCacheOF makeChunkCacheOF() {
        return this.sectionPos == null ? null : new ChunkCacheOF(this, this.sectionPos);
    }

    public int getMinChunkX() {
        return this.minChunkX;
    }

    public int getMinChunkZ() {
        return this.minChunkZ;
    }

    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level instanceof ClientLevel clientlevel ? clientlevel.getShade(normalX, normalY, normalZ, shade) : 1.0F;
    }

    public ModelDataManager getModelDataManager() {
        return this.level instanceof ClientLevel clientlevel ? clientlevel.getModelDataManager() : null;
    }
}