package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BlockGetter, BiomeManager.NoiseBiomeSource, LightChunk, StructureAccess {
    public static final int NO_FILLED_SECTION = -1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
    protected final ShortList[] postProcessing;
    protected volatile boolean unsaved;
    private volatile boolean isLightCorrect;
    protected final ChunkPos chunkPos;
    private long inhabitedTime;
    @Nullable
    @Deprecated
    private BiomeGenerationSettings carverBiomeSettings;
    @Nullable
    protected NoiseChunk noiseChunk;
    protected final UpgradeData upgradeData;
    @Nullable
    protected BlendingData blendingData;
    protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
    protected ChunkSkyLightSources skyLightSources;
    private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
    private final Map<Structure, LongSet> structuresRefences = Maps.newHashMap();
    protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
    protected final Map<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap<>();
    protected final LevelHeightAccessor levelHeightAccessor;
    protected final LevelChunkSection[] sections;

    public ChunkAccess(
        ChunkPos pChunkPos,
        UpgradeData pUpgradeData,
        LevelHeightAccessor pLevelHeightAccessor,
        Registry<Biome> pBiomeRegistry,
        long pInhabitedTime,
        @Nullable LevelChunkSection[] pSections,
        @Nullable BlendingData pBlendingData
    ) {
        this.chunkPos = pChunkPos;
        this.upgradeData = pUpgradeData;
        this.levelHeightAccessor = pLevelHeightAccessor;
        this.sections = new LevelChunkSection[pLevelHeightAccessor.getSectionsCount()];
        this.inhabitedTime = pInhabitedTime;
        this.postProcessing = new ShortList[pLevelHeightAccessor.getSectionsCount()];
        this.blendingData = pBlendingData;
        this.skyLightSources = new ChunkSkyLightSources(pLevelHeightAccessor);
        if (pSections != null) {
            if (this.sections.length == pSections.length) {
                System.arraycopy(pSections, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", pSections.length, this.sections.length);
            }
        }

        replaceMissingSections(pBiomeRegistry, this.sections);
    }

    private static void replaceMissingSections(Registry<Biome> pBiomeRegistry, LevelChunkSection[] pSections) {
        for (int i = 0; i < pSections.length; i++) {
            if (pSections[i] == null) {
                pSections[i] = new LevelChunkSection(pBiomeRegistry);
            }
        }
    }

    public GameEventListenerRegistry getListenerRegistry(int pSectionY) {
        return GameEventListenerRegistry.NOOP;
    }

    @Nullable
    public abstract BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving);

    public abstract void setBlockEntity(BlockEntity pBlockEntity);

    public abstract void addEntity(Entity pEntity);

    public int getHighestFilledSectionIndex() {
        LevelChunkSection[] alevelchunksection = this.getSections();

        for (int i = alevelchunksection.length - 1; i >= 0; i--) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            if (!levelchunksection.hasOnlyAir()) {
                return i;
            }
        }

        return -1;
    }

    @Deprecated(
        forRemoval = true
    )
    public int getHighestSectionPosition() {
        int i = this.getHighestFilledSectionIndex();
        return i == -1 ? this.getMinBuildHeight() : SectionPos.sectionToBlockCoord(this.getSectionYFromSectionIndex(i));
    }

    public Set<BlockPos> getBlockEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    public LevelChunkSection[] getSections() {
        return this.sections;
    }

    public LevelChunkSection getSection(int pIndex) {
        return this.getSections()[pIndex];
    }

    public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public void setHeightmap(Heightmap.Types pType, long[] pData) {
        this.getOrCreateHeightmapUnprimed(pType).setRawData(this, pType, pData);
    }

    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types pType) {
        return this.heightmaps.computeIfAbsent(pType, p_187665_ -> new Heightmap(this, p_187665_));
    }

    public boolean hasPrimedHeightmap(Heightmap.Types pType) {
        return this.heightmaps.get(pType) != null;
    }

    public int getHeight(Heightmap.Types pType, int pX, int pZ) {
        Heightmap heightmap = this.heightmaps.get(pType);
        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
                LOGGER.error("Unprimed heightmap: " + pType + " " + pX + " " + pZ);
            }

            Heightmap.primeHeightmaps(this, EnumSet.of(pType));
            heightmap = this.heightmaps.get(pType);
        }

        return heightmap.getFirstAvailable(pX & 15, pZ & 15) - 1;
    }

    public ChunkPos getPos() {
        return this.chunkPos;
    }

    @Nullable
    @Override
    public StructureStart getStartForStructure(Structure pStructure) {
        return this.structureStarts.get(pStructure);
    }

    @Override
    public void setStartForStructure(Structure pStructure, StructureStart pStructureStart) {
        this.structureStarts.put(pStructure, pStructureStart);
        this.unsaved = true;
    }

    public Map<Structure, StructureStart> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    public void setAllStarts(Map<Structure, StructureStart> pStructureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(pStructureStarts);
        this.unsaved = true;
    }

    @Override
    public LongSet getReferencesForStructure(Structure pStructure) {
        return this.structuresRefences.getOrDefault(pStructure, EMPTY_REFERENCE_SET);
    }

    @Override
    public void addReferenceForStructure(Structure pStructure, long pReference) {
        this.structuresRefences.computeIfAbsent(pStructure, p_223019_ -> new LongOpenHashSet()).add(pReference);
        this.unsaved = true;
    }

    @Override
    public Map<Structure, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(Map<Structure, LongSet> pStructureReferencesMap) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(pStructureReferencesMap);
        this.unsaved = true;
    }

    public boolean isYSpaceEmpty(int pStartY, int pEndY) {
        if (pStartY < this.getMinBuildHeight()) {
            pStartY = this.getMinBuildHeight();
        }

        if (pEndY >= this.getMaxBuildHeight()) {
            pEndY = this.getMaxBuildHeight() - 1;
        }

        for (int i = pStartY; i <= pEndY; i += 16) {
            if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public boolean isSectionEmpty(int pY) {
        return this.getSection(this.getSectionIndexFromSectionY(pY)).hasOnlyAir();
    }

    public void setUnsaved(boolean pUnsaved) {
        this.unsaved = pUnsaved;
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public abstract ChunkStatus getPersistedStatus();

    public ChunkStatus getHighestGeneratedStatus() {
        ChunkStatus chunkstatus = this.getPersistedStatus();
        BelowZeroRetrogen belowzeroretrogen = this.getBelowZeroRetrogen();
        if (belowzeroretrogen != null) {
            ChunkStatus chunkstatus1 = belowzeroretrogen.targetStatus();
            return ChunkStatus.max(chunkstatus1, chunkstatus);
        } else {
            return chunkstatus;
        }
    }

    public abstract void removeBlockEntity(BlockPos pPos);

    public void markPosForPostprocessing(BlockPos pPos) {
        LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", pPos);
    }

    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(short pPackedPosition, int pIndex) {
        getOrCreateOffsetList(this.getPostProcessing(), pIndex).add(pPackedPosition);
    }

    public void setBlockEntityNbt(CompoundTag pTag) {
        this.pendingBlockEntities.put(BlockEntity.getPosFromTag(pTag), pTag);
    }

    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos pPos) {
        return this.pendingBlockEntities.get(pPos);
    }

    @Nullable
    public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos pPos, HolderLookup.Provider pRegistries);

    @Override
    public final void findBlockLightSources(BiConsumer<BlockPos, BlockState> pOutput) {
        this.findBlocks(p_284897_ -> p_284897_.getLightEmission() != 0, pOutput);
    }

    public void findBlocks(Predicate<BlockState> pPredicate, BiConsumer<BlockPos, BlockState> pOutput) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = this.getMinSection(); i < this.getMaxSection(); i++) {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndexFromSectionY(i));
            if (levelchunksection.maybeHas(pPredicate)) {
                BlockPos blockpos = SectionPos.of(this.chunkPos, i).origin();

                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 16; k++) {
                        for (int l = 0; l < 16; l++) {
                            BlockState blockstate = levelchunksection.getBlockState(l, j, k);
                            if (pPredicate.test(blockstate)) {
                                pOutput.accept(blockpos$mutableblockpos.setWithOffset(blockpos, l, j, k), blockstate);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract TickContainerAccess<Block> getBlockTicks();

    public abstract TickContainerAccess<Fluid> getFluidTicks();

    public abstract ChunkAccess.TicksToSave getTicksForSerialization();

    public UpgradeData getUpgradeData() {
        return this.upgradeData;
    }

    public boolean isOldNoiseGeneration() {
        return this.blendingData != null;
    }

    @Nullable
    public BlendingData getBlendingData() {
        return this.blendingData;
    }

    public void setBlendingData(BlendingData pBlendingData) {
        this.blendingData = pBlendingData;
    }

    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    public void incrementInhabitedTime(long pAmount) {
        this.inhabitedTime += pAmount;
    }

    public void setInhabitedTime(long pInhabitedTime) {
        this.inhabitedTime = pInhabitedTime;
    }

    public static ShortList getOrCreateOffsetList(ShortList[] pPackedPositions, int pIndex) {
        if (pPackedPositions[pIndex] == null) {
            pPackedPositions[pIndex] = new ShortArrayList();
        }

        return pPackedPositions[pIndex];
    }

    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    public void setLightCorrect(boolean pLightCorrect) {
        this.isLightCorrect = pLightCorrect;
        this.setUnsaved(true);
    }

    @Override
    public int getMinBuildHeight() {
        return this.levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.levelHeightAccessor.getHeight();
    }

    public NoiseChunk getOrCreateNoiseChunk(Function<ChunkAccess, NoiseChunk> pNoiseChunkCreator) {
        if (this.noiseChunk == null) {
            this.noiseChunk = pNoiseChunkCreator.apply(this);
        }

        return this.noiseChunk;
    }

    @Deprecated
    public BiomeGenerationSettings carverBiome(Supplier<BiomeGenerationSettings> pCaverBiomeSettingsSupplier) {
        if (this.carverBiomeSettings == null) {
            this.carverBiomeSettings = pCaverBiomeSettingsSupplier.get();
        }

        return this.carverBiomeSettings;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
        try {
            int i = QuartPos.fromBlock(this.getMinBuildHeight());
            int k = i + QuartPos.fromBlock(this.getHeight()) - 1;
            int l = Mth.clamp(pY, i, k);
            int j = this.getSectionIndex(QuartPos.toBlock(l));
            return this.sections[j].getNoiseBiome(pX & 3, l & 3, pZ & 3);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting biome");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Biome being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    public void fillBiomesFromNoise(BiomeResolver pResolver, Climate.Sampler pSampler) {
        ChunkPos chunkpos = this.getPos();
        int i = QuartPos.fromBlock(chunkpos.getMinBlockX());
        int j = QuartPos.fromBlock(chunkpos.getMinBlockZ());
        LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();

        for (int k = levelheightaccessor.getMinSection(); k < levelheightaccessor.getMaxSection(); k++) {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndexFromSectionY(k));
            int l = QuartPos.fromSection(k);
            levelchunksection.fillBiomesFromNoise(pResolver, pSampler, i, l, j);
        }
    }

    public boolean hasAnyStructureReferences() {
        return !this.getAllReferences().isEmpty();
    }

    @Nullable
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return null;
    }

    public boolean isUpgrading() {
        return this.getBelowZeroRetrogen() != null;
    }

    public LevelHeightAccessor getHeightAccessorForGeneration() {
        return this;
    }

    public void initializeLightSources() {
        this.skyLightSources.fillFrom(this);
    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return this.skyLightSources;
    }

    public static record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {
    }
}