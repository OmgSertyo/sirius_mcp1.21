package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import net.optifine.reflect.Reflector;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider, GeneratingChunkMap {
    private static final ChunkResult<List<ChunkAccess>> UNLOADED_CHUNK_LIST_RESULT = ChunkResult.error("Unloaded chunks found in range");
    private static final CompletableFuture<ChunkResult<List<ChunkAccess>>> UNLOADED_CHUNK_LIST_FUTURE = CompletableFuture.completedFuture(UNLOADED_CHUNK_LIST_RESULT);
    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHUNK_SAVED_PER_TICK = 200;
    private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
    private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
    public static final int MIN_VIEW_DISTANCE = 2;
    public static final int MAX_VIEW_DISTANCE = 32;
    public static final int FORCED_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap<>();
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap = this.updatingChunkMap.clone();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads = new Long2ObjectLinkedOpenHashMap<>();
    private final List<ChunkGenerationTask> pendingGenerationTasks = new ArrayList<>();
    final ServerLevel level;
    private final ThreadedLevelLightEngine lightEngine;
    private final BlockableEventLoop<Runnable> mainThreadExecutor;
    private final RandomState randomState;
    private final ChunkGeneratorStructureState chunkGeneratorState;
    private final Supplier<DimensionDataStorage> overworldDataStorage;
    private final PoiManager poiManager;
    final LongSet toDrop = new LongOpenHashSet();
    private boolean modified;
    private final ChunkTaskPriorityQueueSorter queueSorter;
    private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
    private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
    private final ChunkProgressListener progressListener;
    private final ChunkStatusUpdateListener chunkStatusListener;
    private final ChunkMap.DistanceManager distanceManager;
    private final AtomicInteger tickingGenerated = new AtomicInteger();
    private final String storageName;
    private final PlayerMap playerMap = new PlayerMap();
    private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap = new Int2ObjectOpenHashMap<>();
    private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
    private final Long2LongMap chunkSaveCooldowns = new Long2LongOpenHashMap();
    private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
    private int serverViewDistance;
    private final WorldGenContext worldGenContext;

    public ChunkMap(
        ServerLevel pLevel,
        LevelStorageSource.LevelStorageAccess pLevelStorageAccess,
        DataFixer pFixerUpper,
        StructureTemplateManager pStructureManager,
        Executor pDispatcher,
        BlockableEventLoop<Runnable> pMainThreadExecutor,
        LightChunkGetter pLightChunk,
        ChunkGenerator pGenerator,
        ChunkProgressListener pProgressListener,
        ChunkStatusUpdateListener pChunkStatusListener,
        Supplier<DimensionDataStorage> pOverworldDataStorage,
        int pViewDistance,
        boolean pSync
    ) {
        super(
            new RegionStorageInfo(pLevelStorageAccess.getLevelId(), pLevel.dimension(), "chunk"),
            pLevelStorageAccess.getDimensionPath(pLevel.dimension()).resolve("region"),
            pFixerUpper,
            pSync
        );
        Path path = pLevelStorageAccess.getDimensionPath(pLevel.dimension());
        this.storageName = path.getFileName().toString();
        this.level = pLevel;
        RegistryAccess registryaccess = pLevel.registryAccess();
        long i = pLevel.getSeed();
        if (pGenerator instanceof NoiseBasedChunkGenerator noisebasedchunkgenerator) {
            this.randomState = RandomState.create(noisebasedchunkgenerator.generatorSettings().value(), registryaccess.lookupOrThrow(Registries.NOISE), i);
        } else {
            this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), registryaccess.lookupOrThrow(Registries.NOISE), i);
        }

        this.chunkGeneratorState = pGenerator.createState(registryaccess.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, i);
        this.mainThreadExecutor = pMainThreadExecutor;
        ProcessorMailbox<Runnable> processormailbox1 = ProcessorMailbox.create(pDispatcher, "worldgen");
        ProcessorHandle<Runnable> processorhandle = ProcessorHandle.of("main", pMainThreadExecutor::tell);
        this.progressListener = pProgressListener;
        this.chunkStatusListener = pChunkStatusListener;
        ProcessorMailbox<Runnable> processormailbox = ProcessorMailbox.create(pDispatcher, "light");
        this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(processormailbox1, processorhandle, processormailbox), pDispatcher, Integer.MAX_VALUE);
        this.worldgenMailbox = this.queueSorter.getProcessor(processormailbox1, false);
        this.mainThreadMailbox = this.queueSorter.getProcessor(processorhandle, false);
        this.lightEngine = new ThreadedLevelLightEngine(
            pLightChunk, this, this.level.dimensionType().hasSkyLight(), processormailbox, this.queueSorter.getProcessor(processormailbox, false)
        );
        this.distanceManager = new ChunkMap.DistanceManager(pDispatcher, pMainThreadExecutor);
        this.overworldDataStorage = pOverworldDataStorage;
        this.poiManager = new PoiManager(
            new RegionStorageInfo(pLevelStorageAccess.getLevelId(), pLevel.dimension(), "poi"),
            path.resolve("poi"),
            pFixerUpper,
            pSync,
            registryaccess,
            pLevel.getServer(),
            pLevel
        );
        this.setServerViewDistance(pViewDistance);
        this.worldGenContext = new WorldGenContext(pLevel, pGenerator, pStructureManager, this.lightEngine, this.mainThreadMailbox);
    }

    protected ChunkGenerator generator() {
        return this.worldGenContext.generator();
    }

    protected ChunkGeneratorStructureState generatorState() {
        return this.chunkGeneratorState;
    }

    protected RandomState randomState() {
        return this.randomState;
    }

    private static double euclideanDistanceSquared(ChunkPos pChunkPos, Entity pEntity) {
        double d0 = (double)SectionPos.sectionToBlockCoord(pChunkPos.x, 8);
        double d1 = (double)SectionPos.sectionToBlockCoord(pChunkPos.z, 8);
        double d2 = d0 - pEntity.getX();
        double d3 = d1 - pEntity.getZ();
        return d2 * d2 + d3 * d3;
    }

    boolean isChunkTracked(ServerPlayer pPlayer, int pX, int pZ) {
        return pPlayer.getChunkTrackingView().contains(pX, pZ) && !pPlayer.connection.chunkSender.isPending(ChunkPos.asLong(pX, pZ));
    }

    private boolean isChunkOnTrackedBorder(ServerPlayer pPlayer, int pX, int pZ) {
        if (!this.isChunkTracked(pPlayer, pX, pZ)) {
            return false;
        } else {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if ((i != 0 || j != 0) && !this.isChunkTracked(pPlayer, pX + i, pZ + j)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    protected ThreadedLevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    protected ChunkHolder getUpdatingChunkIfPresent(long pChunkPos) {
        return this.updatingChunkMap.get(pChunkPos);
    }

    @Nullable
    protected ChunkHolder getVisibleChunkIfPresent(long pChunkPos) {
        return this.visibleChunkMap.get(pChunkPos);
    }

    protected IntSupplier getChunkQueueLevel(long pChunkPos) {
        return () -> {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
            return chunkholder == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(chunkholder.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
        };
    }

    public String getChunkDebugData(ChunkPos pPos) {
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pPos.toLong());
        if (chunkholder == null) {
            return "null";
        } else {
            String s = chunkholder.getTicketLevel() + "\n";
            ChunkStatus chunkstatus = chunkholder.getLatestStatus();
            ChunkAccess chunkaccess = chunkholder.getLatestChunk();
            if (chunkstatus != null) {
                s = s + "St: \u00a7" + chunkstatus.getIndex() + chunkstatus + "\u00a7r\n";
            }

            if (chunkaccess != null) {
                s = s + "Ch: \u00a7" + chunkaccess.getPersistedStatus().getIndex() + chunkaccess.getPersistedStatus() + "\u00a7r\n";
            }

            FullChunkStatus fullchunkstatus = chunkholder.getFullStatus();
            s = s + "\u00a7" + fullchunkstatus.ordinal() + fullchunkstatus;
            return s + "\u00a7r";
        }
    }

    private CompletableFuture<ChunkResult<List<ChunkAccess>>> getChunkRangeFuture(ChunkHolder pChunkHolder, int pRange, IntFunction<ChunkStatus> pStatusGetter) {
        if (pRange == 0) {
            ChunkStatus chunkstatus1 = pStatusGetter.apply(0);
            return pChunkHolder.scheduleChunkGenerationTask(chunkstatus1, this).thenApply(resultIn -> resultIn.map(List::of));
        } else {
            List<CompletableFuture<ChunkResult<ChunkAccess>>> list = new ArrayList<>();
            ChunkPos chunkpos = pChunkHolder.getPos();

            for (int i = -pRange; i <= pRange; i++) {
                for (int j = -pRange; j <= pRange; j++) {
                    int k = Math.max(Math.abs(j), Math.abs(i));
                    long l = ChunkPos.asLong(chunkpos.x + j, chunkpos.z + i);
                    ChunkHolder chunkholder = this.getUpdatingChunkIfPresent(l);
                    if (chunkholder == null) {
                        return UNLOADED_CHUNK_LIST_FUTURE;
                    }

                    ChunkStatus chunkstatus = pStatusGetter.apply(k);
                    list.add(chunkholder.scheduleChunkGenerationTask(chunkstatus, this));
                }
            }

            return Util.sequence(list).thenApply(chunkResultsIn -> {
                List<ChunkAccess> list1 = Lists.newArrayList();

                for (ChunkResult<ChunkAccess> chunkresult : chunkResultsIn) {
                    if (chunkresult == null) {
                        throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
                    }

                    ChunkAccess chunkaccess = chunkresult.orElse(null);
                    if (chunkaccess == null) {
                        return UNLOADED_CHUNK_LIST_RESULT;
                    }

                    list1.add(chunkaccess);
                }

                return ChunkResult.of(list1);
            });
        }
    }

    public ReportedException debugFuturesAndCreateReportedException(IllegalStateException pException, String pDetails) {
        StringBuilder stringbuilder = new StringBuilder();
        Consumer<ChunkHolder> consumer = holderIn -> holderIn.getAllFutures()
                .forEach(
                    pairIn -> {
                        ChunkStatus chunkstatus = pairIn.getFirst();
                        CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = pairIn.getSecond();
                        if (completablefuture != null && completablefuture.isDone() && completablefuture.join() == null) {
                            stringbuilder.append(holderIn.getPos())
                                .append(" - status: ")
                                .append(chunkstatus)
                                .append(" future: ")
                                .append(completablefuture)
                                .append(System.lineSeparator());
                        }
                    }
                );
        stringbuilder.append("Updating:").append(System.lineSeparator());
        this.updatingChunkMap.values().forEach(consumer);
        stringbuilder.append("Visible:").append(System.lineSeparator());
        this.visibleChunkMap.values().forEach(consumer);
        CrashReport crashreport = CrashReport.forThrowable(pException, "Chunk loading");
        CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk loading");
        crashreportcategory.setDetail("Details", pDetails);
        crashreportcategory.setDetail("Futures", stringbuilder);
        return new ReportedException(crashreport);
    }

    public CompletableFuture<ChunkResult<LevelChunk>> prepareEntityTickingChunk(ChunkHolder pChunk) {
        return this.getChunkRangeFuture(pChunk, 2, levelIn -> ChunkStatus.FULL)
            .thenApplyAsync(resultIn -> resultIn.map(chunksIn -> (LevelChunk)chunksIn.get(chunksIn.size() / 2)), this.mainThreadExecutor);
    }

    @Nullable
    ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel) {
        if (!ChunkLevel.isLoaded(pOldLevel) && !ChunkLevel.isLoaded(pNewLevel)) {
            return pHolder;
        } else {
            if (pHolder != null) {
                pHolder.setTicketLevel(pNewLevel);
            }

            if (pHolder != null) {
                if (!ChunkLevel.isLoaded(pNewLevel)) {
                    this.toDrop.add(pChunkPos);
                } else {
                    this.toDrop.remove(pChunkPos);
                }
            }

            if (ChunkLevel.isLoaded(pNewLevel) && pHolder == null) {
                pHolder = this.pendingUnloads.remove(pChunkPos);
                if (pHolder != null) {
                    pHolder.setTicketLevel(pNewLevel);
                } else {
                    pHolder = new ChunkHolder(new ChunkPos(pChunkPos), pNewLevel, this.level, this.lightEngine, this.queueSorter, this);
                }

                this.updatingChunkMap.put(pChunkPos, pHolder);
                this.modified = true;
            }

            if (Reflector.ForgeEventFactory_fireChunkTicketLevelUpdated.exists()) {
                Reflector.ForgeEventFactory_fireChunkTicketLevelUpdated.call(this.level, pChunkPos, pOldLevel, pNewLevel, pHolder);
            }

            return pHolder;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.queueSorter.close();
            this.poiManager.close();
        } finally {
            super.close();
        }
    }

    protected void saveAllChunks(boolean pFlush) {
        if (pFlush) {
            List<ChunkHolder> list = this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).toList();
            MutableBoolean mutableboolean = new MutableBoolean();

            do {
                mutableboolean.setFalse();
                list.stream()
                    .map(chunkHolderIn -> {
                        this.mainThreadExecutor.managedBlock(chunkHolderIn::isReadyForSaving);
                        return chunkHolderIn.getLatestChunk();
                    })
                    .filter(chunkIn -> chunkIn instanceof ImposterProtoChunk || chunkIn instanceof LevelChunk)
                    .filter(this::save)
                    .forEach(voidIn -> mutableboolean.setTrue());
            } while (mutableboolean.isTrue());

            this.processUnloads(() -> true);
            this.flushWorker();
        } else {
            this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
        }
    }

    protected void tick(BooleanSupplier pHasMoreTime) {
        ProfilerFiller profilerfiller = this.level.getProfiler();
        profilerfiller.push("poi");
        this.poiManager.tick(pHasMoreTime);
        profilerfiller.popPush("chunk_unload");
        if (!this.level.noSave()) {
            this.processUnloads(pHasMoreTime);
        }

        profilerfiller.pop();
    }

    public boolean hasWork() {
        return this.lightEngine.hasLightWork()
            || !this.pendingUnloads.isEmpty()
            || !this.updatingChunkMap.isEmpty()
            || this.poiManager.hasWork()
            || !this.toDrop.isEmpty()
            || !this.unloadQueue.isEmpty()
            || this.queueSorter.hasWork()
            || this.distanceManager.hasTickets();
    }

    private void processUnloads(BooleanSupplier pHasMoreTime) {
        LongIterator longiterator = this.toDrop.iterator();
        int i = 0;

        while (longiterator.hasNext() && (pHasMoreTime.getAsBoolean() || i < 200 || this.toDrop.size() > 2000)) {
            long j = longiterator.nextLong();
            ChunkHolder chunkholder = this.updatingChunkMap.get(j);
            if (chunkholder != null) {
                if (chunkholder.getGenerationRefCount() != 0) {
                    continue;
                }

                this.updatingChunkMap.remove(j);
                this.pendingUnloads.put(j, chunkholder);
                this.modified = true;
                i++;
                this.scheduleUnload(j, chunkholder);
            }

            longiterator.remove();
        }

        int k = Math.max(0, this.unloadQueue.size() - 2000);

        Runnable runnable;
        while ((pHasMoreTime.getAsBoolean() || k > 0) && (runnable = this.unloadQueue.poll()) != null) {
            k--;
            runnable.run();
        }

        int l = 0;
        ObjectIterator<ChunkHolder> objectiterator = this.visibleChunkMap.values().iterator();

        while (l < 20 && pHasMoreTime.getAsBoolean() && objectiterator.hasNext()) {
            if (this.saveChunkIfNeeded(objectiterator.next())) {
                l++;
            }
        }
    }

    private void scheduleUnload(long pChunkPos, ChunkHolder pChunkHolder) {
        pChunkHolder.getSaveSyncFuture().thenRunAsync(() -> {
            if (!pChunkHolder.isReadyForSaving()) {
                this.scheduleUnload(pChunkPos, pChunkHolder);
            } else {
                ChunkAccess chunkaccess = pChunkHolder.getLatestChunk();
                if (this.pendingUnloads.remove(pChunkPos, pChunkHolder) && chunkaccess != null) {
                    if (chunkaccess instanceof LevelChunk levelchunk) {
                        levelchunk.setLoaded(false);
                        if (Reflector.ForgeEventFactory_onChunkUnload.exists()) {
                            Reflector.ForgeEventFactory_onChunkUnload.call(chunkaccess);
                        }
                    }

                    this.save(chunkaccess);
                    if (chunkaccess instanceof LevelChunk levelchunk1) {
                        this.level.unload(levelchunk1);
                    }

                    this.lightEngine.updateChunkStatus(chunkaccess.getPos());
                    this.lightEngine.tryScheduleUpdate();
                    this.progressListener.onStatusChange(chunkaccess.getPos(), null);
                    this.chunkSaveCooldowns.remove(chunkaccess.getPos().toLong());
                }
            }
        }, this.unloadQueue::add).whenComplete((worldIn, throwableIn) -> {
            if (throwableIn != null) {
                LOGGER.error("Failed to save chunk {}", pChunkHolder.getPos(), throwableIn);
            }
        });
    }

    protected boolean promoteChunkMap() {
        if (!this.modified) {
            return false;
        } else {
            this.visibleChunkMap = this.updatingChunkMap.clone();
            this.modified = false;
            return true;
        }
    }

    private CompletableFuture<ChunkAccess> scheduleChunkLoad(ChunkPos pChunkPos) {
        return this.readChunk(pChunkPos).thenApply(tagIn -> tagIn.filter(tag2In -> {
                boolean flag = isChunkDataValid(tag2In);
                if (!flag) {
                    LOGGER.error("Chunk file at {} is missing level data, skipping", pChunkPos);
                }

                return flag;
            })).thenApplyAsync(tag3In -> {
            this.level.getProfiler().incrementCounter("chunkLoad");
            if (tag3In.isPresent()) {
                ChunkAccess chunkaccess = ChunkSerializer.read(this.level, this.poiManager, this.storageInfo(), pChunkPos, tag3In.get());
                this.markPosition(pChunkPos, chunkaccess.getPersistedStatus().getChunkType());
                return chunkaccess;
            } else {
                return this.createEmptyChunk(pChunkPos);
            }
        }, this.mainThreadExecutor).exceptionallyAsync(throwableIn -> this.handleChunkLoadFailure(throwableIn, pChunkPos), this.mainThreadExecutor);
    }

    private static boolean isChunkDataValid(CompoundTag pTag) {
        return pTag.contains("Status", 8);
    }

    private ChunkAccess handleChunkLoadFailure(Throwable pException, ChunkPos pChunkPos) {
        Throwable throwable = pException instanceof CompletionException completionexception ? completionexception.getCause() : pException;
        Throwable throwable1 = throwable instanceof ReportedException reportedexception ? reportedexception.getCause() : throwable;
        boolean flag1 = throwable1 instanceof Error;
        boolean flag = throwable1 instanceof IOException || throwable1 instanceof NbtException;
        if (!flag1) {
            if (!flag) {
            }

            this.level.getServer().reportChunkLoadFailure(throwable1, this.storageInfo(), pChunkPos);
            return this.createEmptyChunk(pChunkPos);
        } else {
            CrashReport crashreport = CrashReport.forThrowable(pException, "Exception loading chunk");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk being loaded");
            crashreportcategory.setDetail("pos", pChunkPos);
            this.markPositionReplaceable(pChunkPos);
            throw new ReportedException(crashreport);
        }
    }

    private ChunkAccess createEmptyChunk(ChunkPos pChunkPos) {
        this.markPositionReplaceable(pChunkPos);
        return new ProtoChunk(pChunkPos, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registries.BIOME), null);
    }

    private void markPositionReplaceable(ChunkPos pChunkPos) {
        this.chunkTypeCache.put(pChunkPos.toLong(), (byte)-1);
    }

    private byte markPosition(ChunkPos pChunkPos, ChunkType pChunkType) {
        return this.chunkTypeCache.put(pChunkPos.toLong(), (byte)(pChunkType == ChunkType.PROTOCHUNK ? -1 : 1));
    }

    @Override
    public GenerationChunkHolder acquireGeneration(long pChunkPos) {
        ChunkHolder chunkholder = this.updatingChunkMap.get(pChunkPos);
        chunkholder.increaseGenerationRefCount();
        return chunkholder;
    }

    @Override
    public void releaseGeneration(GenerationChunkHolder pChunk) {
        pChunk.decreaseGenerationRefCount();
    }

    @Override
    public CompletableFuture<ChunkAccess> applyStep(GenerationChunkHolder pChunk, ChunkStep pStep, StaticCache2D<GenerationChunkHolder> pCache) {
        ChunkPos chunkpos = pChunk.getPos();
        if (pStep.targetStatus() == ChunkStatus.EMPTY) {
            return this.scheduleChunkLoad(chunkpos);
        } else {
            try {
                GenerationChunkHolder generationchunkholder = pCache.get(chunkpos.x, chunkpos.z);
                ChunkAccess chunkaccess = generationchunkholder.getChunkIfPresentUnchecked(pStep.targetStatus().getParent());
                if (chunkaccess == null) {
                    throw new IllegalStateException("Parent chunk missing");
                } else {
                    CompletableFuture<ChunkAccess> completablefuture = pStep.apply(this.worldGenContext, pCache, chunkaccess);
                    this.progressListener.onStatusChange(chunkpos, pStep.targetStatus());
                    return completablefuture;
                }
            } catch (Exception exception1) {
                exception1.getStackTrace();
                CrashReport crashreport = CrashReport.forThrowable(exception1, "Exception generating new chunk");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
                crashreportcategory.setDetail("Status being generated", () -> pStep.targetStatus().getName());
                crashreportcategory.setDetail("Location", String.format(Locale.ROOT, "%d,%d", chunkpos.x, chunkpos.z));
                crashreportcategory.setDetail("Position hash", ChunkPos.asLong(chunkpos.x, chunkpos.z));
                crashreportcategory.setDetail("Generator", this.generator());
                this.mainThreadExecutor.execute(() -> {
                    throw new ReportedException(crashreport);
                });
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    public ChunkGenerationTask scheduleGenerationTask(ChunkStatus pTargetStatus, ChunkPos pPos) {
        ChunkGenerationTask chunkgenerationtask = ChunkGenerationTask.create(this, pTargetStatus, pPos);
        this.pendingGenerationTasks.add(chunkgenerationtask);
        return chunkgenerationtask;
    }

    private void runGenerationTask(ChunkGenerationTask p_344392_) {
        this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(p_344392_.getCenter(), () -> {
            CompletableFuture<?> completablefuture = p_344392_.runUntilWait();
            if (completablefuture != null) {
                completablefuture.thenRun(() -> this.runGenerationTask(p_344392_));
            }
        }));
    }

    @Override
    public void runGenerationTasks() {
        this.pendingGenerationTasks.forEach(this::runGenerationTask);
        this.pendingGenerationTasks.clear();
    }

    public CompletableFuture<ChunkResult<LevelChunk>> prepareTickingChunk(ChunkHolder pHolder) {
        CompletableFuture<ChunkResult<List<ChunkAccess>>> completablefuture = this.getChunkRangeFuture(pHolder, 1, levelIn -> ChunkStatus.FULL);
        CompletableFuture<ChunkResult<LevelChunk>> completablefuture1 = completablefuture.<ChunkResult<LevelChunk>>thenApplyAsync(
                resultIn -> resultIn.map(chunksIn -> (LevelChunk)chunksIn.get(chunksIn.size() / 2)),
                runnableIn -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(pHolder, runnableIn))
            )
            .thenApplyAsync(result2In -> result2In.ifSuccess(levelChunk2In -> {
                    levelChunk2In.postProcessGeneration();
                    this.level.startTickingChunk(levelChunk2In);
                    CompletableFuture<?> completablefuture2 = pHolder.getSendSyncFuture();
                    if (completablefuture2.isDone()) {
                        this.onChunkReadyToSend(levelChunk2In);
                    } else {
                        completablefuture2.thenAcceptAsync(voidIn -> this.onChunkReadyToSend(levelChunk2In), this.mainThreadExecutor);
                    }
                }), this.mainThreadExecutor);
        completablefuture1.handle((resultIn, throwableIn) -> {
            this.tickingGenerated.getAndIncrement();
            return null;
        });
        return completablefuture1;
    }

    private void onChunkReadyToSend(LevelChunk pChunk) {
        ChunkPos chunkpos = pChunk.getPos();

        for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            if (serverplayer.getChunkTrackingView().contains(chunkpos)) {
                markChunkPendingToSend(serverplayer, pChunk);
            }
        }
    }

    public CompletableFuture<ChunkResult<LevelChunk>> prepareAccessibleChunk(ChunkHolder pHolder) {
        return this.getChunkRangeFuture(pHolder, 1, ChunkLevel::getStatusAroundFullChunk)
            .thenApplyAsync(
                resultIn -> resultIn.map(worldsIn -> (LevelChunk)worldsIn.get(worldsIn.size() / 2)),
                runnableIn -> this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(pHolder, runnableIn))
            );
    }

    public int getTickingGenerated() {
        return this.tickingGenerated.get();
    }

    private boolean saveChunkIfNeeded(ChunkHolder p_198875_) {
        if (p_198875_.wasAccessibleSinceLastSave() && p_198875_.isReadyForSaving()) {
            ChunkAccess chunkaccess = p_198875_.getLatestChunk();
            if (!(chunkaccess instanceof ImposterProtoChunk) && !(chunkaccess instanceof LevelChunk)) {
                return false;
            } else {
                long i = chunkaccess.getPos().toLong();
                long j = this.chunkSaveCooldowns.getOrDefault(i, -1L);
                long k = System.currentTimeMillis();
                if (k < j) {
                    return false;
                } else {
                    boolean flag = this.save(chunkaccess);
                    p_198875_.refreshAccessibility();
                    if (flag) {
                        this.chunkSaveCooldowns.put(i, k + 10000L);
                    }

                    return flag;
                }
            }
        } else {
            return false;
        }
    }

    private boolean save(ChunkAccess p_140259_) {
        this.poiManager.flush(p_140259_.getPos());
        if (!p_140259_.isUnsaved()) {
            return false;
        } else {
            p_140259_.setUnsaved(false);
            ChunkPos chunkpos = p_140259_.getPos();

            try {
                ChunkStatus chunkstatus = p_140259_.getPersistedStatus();
                if (chunkstatus.getChunkType() != ChunkType.LEVELCHUNK) {
                    if (this.isExistingChunkFull(chunkpos)) {
                        return false;
                    }

                    if (chunkstatus == ChunkStatus.EMPTY && p_140259_.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                this.level.getProfiler().incrementCounter("chunkSave");
                CompoundTag compoundtag = ChunkSerializer.write(this.level, p_140259_);
                if (Reflector.ForgeEventFactory_onChunkDataSave.exists()) {
                    Level level = (Level)Reflector.call(p_140259_, Reflector.ForgeIChunk_getWorldForge);
                    Reflector.ForgeEventFactory_onChunkDataSave.call(p_140259_, level != null ? level : this.level, compoundtag);
                }

                this.write(chunkpos, compoundtag).exceptionally(voidIn -> {
                    this.level.getServer().reportChunkSaveFailure(voidIn, this.storageInfo(), chunkpos);
                    return null;
                });
                this.markPosition(chunkpos, chunkstatus.getChunkType());
                return true;
            } catch (Exception exception1) {
                this.level.getServer().reportChunkSaveFailure(exception1, this.storageInfo(), chunkpos);
                return false;
            }
        }
    }

    private boolean isExistingChunkFull(ChunkPos pChunkPos) {
        byte b0 = this.chunkTypeCache.get(pChunkPos.toLong());
        if (b0 != 0) {
            return b0 == 1;
        } else {
            CompoundTag compoundtag;
            try {
                compoundtag = this.readChunk(pChunkPos).join().orElse(null);
                if (compoundtag == null) {
                    this.markPositionReplaceable(pChunkPos);
                    return false;
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to read chunk {}", pChunkPos, exception);
                this.markPositionReplaceable(pChunkPos);
                return false;
            }

            ChunkType chunktype = ChunkSerializer.getChunkTypeFromTag(compoundtag);
            return this.markPosition(pChunkPos, chunktype) == 1;
        }
    }

    protected void setServerViewDistance(int pViewDistance) {
        int i = Mth.clamp(pViewDistance, 2, 64);
        if (i != this.serverViewDistance) {
            this.serverViewDistance = i;
            this.distanceManager.updatePlayerTickets(this.serverViewDistance);

            for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
                this.updateChunkTracking(serverplayer);
            }
        }
    }

    int getPlayerViewDistance(ServerPlayer pPlayer) {
        return Mth.clamp(pPlayer.requestedViewDistance(), 2, this.serverViewDistance);
    }

    private void markChunkPendingToSend(ServerPlayer pPlayer, ChunkPos pChunkPos) {
        LevelChunk levelchunk = this.getChunkToSend(pChunkPos.toLong());
        if (levelchunk != null) {
            markChunkPendingToSend(pPlayer, levelchunk);
        }
    }

    private static void markChunkPendingToSend(ServerPlayer pPlayer, LevelChunk pChunk) {
        pPlayer.connection.chunkSender.markChunkPendingToSend(pChunk);
    }

    private static void dropChunk(ServerPlayer pPlayer, ChunkPos pChunkPos) {
        pPlayer.connection.chunkSender.dropChunk(pPlayer, pChunkPos);
    }

    @Nullable
    public LevelChunk getChunkToSend(long pChunkPos) {
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
        return chunkholder == null ? null : chunkholder.getChunkToSend();
    }

    public int size() {
        return this.visibleChunkMap.size();
    }

    public net.minecraft.server.level.DistanceManager getDistanceManager() {
        return this.distanceManager;
    }

    protected Iterable<ChunkHolder> getChunks() {
        return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
    }

    void dumpChunks(Writer pWriter) throws IOException {
        CsvOutput csvoutput = CsvOutput.builder()
            .addColumn("x")
            .addColumn("z")
            .addColumn("level")
            .addColumn("in_memory")
            .addColumn("status")
            .addColumn("full_status")
            .addColumn("accessible_ready")
            .addColumn("ticking_ready")
            .addColumn("entity_ticking_ready")
            .addColumn("ticket")
            .addColumn("spawning")
            .addColumn("block_entity_count")
            .addColumn("ticking_ticket")
            .addColumn("ticking_level")
            .addColumn("block_ticks")
            .addColumn("fluid_ticks")
            .build(pWriter);
        TickingTracker tickingtracker = this.distanceManager.tickingTracker();

        for (Entry<ChunkHolder> entry : this.visibleChunkMap.long2ObjectEntrySet()) {
            long i = entry.getLongKey();
            ChunkPos chunkpos = new ChunkPos(i);
            ChunkHolder chunkholder = entry.getValue();
            Optional<ChunkAccess> optional = Optional.ofNullable(chunkholder.getLatestChunk());
            Optional<LevelChunk> optional1 = optional.flatMap(worldIn -> worldIn instanceof LevelChunk ? Optional.of((LevelChunk)worldIn) : Optional.empty());
            csvoutput.writeRow(
                chunkpos.x,
                chunkpos.z,
                chunkholder.getTicketLevel(),
                optional.isPresent(),
                optional.map(ChunkAccess::getPersistedStatus).orElse(null),
                optional1.map(LevelChunk::getFullStatus).orElse(null),
                printFuture(chunkholder.getFullChunkFuture()),
                printFuture(chunkholder.getTickingChunkFuture()),
                printFuture(chunkholder.getEntityTickingChunkFuture()),
                this.distanceManager.getTicketDebugString(i),
                this.anyPlayerCloseEnoughForSpawning(chunkpos),
                optional1.<Integer>map(chunkIn -> chunkIn.getBlockEntities().size()).orElse(0),
                tickingtracker.getTicketDebugString(i),
                tickingtracker.getLevel(i),
                optional1.<Integer>map(chunk2In -> chunk2In.getBlockTicks().count()).orElse(0),
                optional1.<Integer>map(chunk3In -> chunk3In.getFluidTicks().count()).orElse(0)
            );
        }
    }

    private static String printFuture(CompletableFuture<ChunkResult<LevelChunk>> pFuture) {
        try {
            ChunkResult<LevelChunk> chunkresult = pFuture.getNow(null);
            if (chunkresult != null) {
                return chunkresult.isSuccess() ? "done" : "unloaded";
            } else {
                return "not completed";
            }
        } catch (CompletionException completionexception) {
            return "failed " + completionexception.getCause().getMessage();
        } catch (CancellationException cancellationexception1) {
            return "cancelled";
        }
    }

    private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos pPos) {
        return this.read(pPos).thenApplyAsync(tagIn -> tagIn.map(this::upgradeChunkTag), Util.backgroundExecutor());
    }

    private CompoundTag upgradeChunkTag(CompoundTag p_214948_) {
        return this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, p_214948_, this.generator().getTypeNameForDataFixer());
    }

    boolean anyPlayerCloseEnoughForSpawning(ChunkPos pChunkPos) {
        if (!this.distanceManager.hasPlayersNearby(pChunkPos.toLong())) {
            return false;
        } else {
            for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
                if (this.playerIsCloseEnoughForSpawning(serverplayer, pChunkPos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos pChunkPos) {
        long i = pChunkPos.toLong();
        if (!this.distanceManager.hasPlayersNearby(i)) {
            return List.of();
        } else {
            Builder<ServerPlayer> builder = ImmutableList.builder();

            for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
                if (this.playerIsCloseEnoughForSpawning(serverplayer, pChunkPos)) {
                    builder.add(serverplayer);
                }
            }

            return builder.build();
        }
    }

    private boolean playerIsCloseEnoughForSpawning(ServerPlayer pPlayer, ChunkPos pChunkPos) {
        if (pPlayer.isSpectator()) {
            return false;
        } else {
            double d0 = euclideanDistanceSquared(pChunkPos, pPlayer);
            return d0 < 16384.0;
        }
    }

    private boolean skipPlayer(ServerPlayer pPlayer) {
        return pPlayer.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
    }

    void updatePlayerStatus(ServerPlayer pPlayer, boolean pTrack) {
        boolean flag = this.skipPlayer(pPlayer);
        boolean flag1 = this.playerMap.ignoredOrUnknown(pPlayer);
        if (pTrack) {
            this.playerMap.addPlayer(pPlayer, flag);
            this.updatePlayerPos(pPlayer);
            if (!flag) {
                this.distanceManager.addPlayer(SectionPos.of(pPlayer), pPlayer);
            }

            pPlayer.setChunkTrackingView(ChunkTrackingView.EMPTY);
            this.updateChunkTracking(pPlayer);
        } else {
            SectionPos sectionpos = pPlayer.getLastSectionPos();
            this.playerMap.removePlayer(pPlayer);
            if (!flag1) {
                this.distanceManager.removePlayer(sectionpos, pPlayer);
            }

            this.applyChunkTrackingView(pPlayer, ChunkTrackingView.EMPTY);
        }
    }

    private void updatePlayerPos(ServerPlayer pPlayer) {
        SectionPos sectionpos = SectionPos.of(pPlayer);
        pPlayer.setLastSectionPos(sectionpos);
    }

    public void move(ServerPlayer pPlayer) {
        for (ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
            if (chunkmap$trackedentity.entity == pPlayer) {
                chunkmap$trackedentity.updatePlayers(this.level.players());
            } else {
                chunkmap$trackedentity.updatePlayer(pPlayer);
            }
        }

        SectionPos sectionpos = pPlayer.getLastSectionPos();
        SectionPos sectionpos1 = SectionPos.of(pPlayer);
        boolean flag = this.playerMap.ignored(pPlayer);
        boolean flag1 = this.skipPlayer(pPlayer);
        boolean flag2 = sectionpos.asLong() != sectionpos1.asLong();
        if (flag2 || flag != flag1) {
            this.updatePlayerPos(pPlayer);
            if (!flag) {
                this.distanceManager.removePlayer(sectionpos, pPlayer);
            }

            if (!flag1) {
                this.distanceManager.addPlayer(sectionpos1, pPlayer);
            }

            if (!flag && flag1) {
                this.playerMap.ignorePlayer(pPlayer);
            }

            if (flag && !flag1) {
                this.playerMap.unIgnorePlayer(pPlayer);
            }

            this.updateChunkTracking(pPlayer);
        }
    }

    private void updateChunkTracking(ServerPlayer pPlayer) {
        ChunkPos chunkpos = pPlayer.chunkPosition();
        int i = this.getPlayerViewDistance(pPlayer);
        if (pPlayer.getChunkTrackingView() instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
            && chunktrackingview$positioned.center().equals(chunkpos)
            && chunktrackingview$positioned.viewDistance() == i) {
            return;
        }

        this.applyChunkTrackingView(pPlayer, ChunkTrackingView.of(chunkpos, i));
    }

    private void applyChunkTrackingView(ServerPlayer pPlayer, ChunkTrackingView pChunkTrackingView) {
        if (pPlayer.level() == this.level) {
            ChunkTrackingView chunktrackingview = pPlayer.getChunkTrackingView();
            if (pChunkTrackingView instanceof ChunkTrackingView.Positioned chunktrackingview$positioned
                && (
                    !(chunktrackingview instanceof ChunkTrackingView.Positioned chunktrackingview$positioned1)
                        || !chunktrackingview$positioned1.center().equals(chunktrackingview$positioned.center())
                )) {
                pPlayer.connection
                    .send(
                        new ClientboundSetChunkCacheCenterPacket(
                            chunktrackingview$positioned.center().x, chunktrackingview$positioned.center().z
                        )
                    );
            }

            ChunkTrackingView.difference(
                chunktrackingview, pChunkTrackingView, chunkPos2In -> this.markChunkPendingToSend(pPlayer, chunkPos2In), chunkPos3In -> dropChunk(pPlayer, chunkPos3In)
            );
            pPlayer.setChunkTrackingView(pChunkTrackingView);
        }
    }

    @Override
    public List<ServerPlayer> getPlayers(ChunkPos pPos, boolean pBoundaryOnly) {
        Set<ServerPlayer> set = this.playerMap.getAllPlayers();
        Builder<ServerPlayer> builder = ImmutableList.builder();

        for (ServerPlayer serverplayer : set) {
            if (pBoundaryOnly && this.isChunkOnTrackedBorder(serverplayer, pPos.x, pPos.z)
                || !pBoundaryOnly && this.isChunkTracked(serverplayer, pPos.x, pPos.z)) {
                builder.add(serverplayer);
            }
        }

        return builder.build();
    }

    protected void addEntity(Entity pEntity) {
        boolean flag = pEntity instanceof EnderDragonPart;
        if (Reflector.PartEntity.exists()) {
            flag = Reflector.PartEntity.isInstance(pEntity);
        }

        if (!flag) {
            EntityType<?> entitytype = pEntity.getType();
            int i = entitytype.clientTrackingRange() * 16;
            if (i != 0) {
                int j = entitytype.updateInterval();
                if (this.entityMap.containsKey(pEntity.getId())) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
                }

                ChunkMap.TrackedEntity chunkmap$trackedentity = new ChunkMap.TrackedEntity(pEntity, i, j, entitytype.trackDeltas());
                this.entityMap.put(pEntity.getId(), chunkmap$trackedentity);
                chunkmap$trackedentity.updatePlayers(this.level.players());
                if (pEntity instanceof ServerPlayer serverplayer) {
                    this.updatePlayerStatus(serverplayer, true);

                    for (ChunkMap.TrackedEntity chunkmap$trackedentity1 : this.entityMap.values()) {
                        if (chunkmap$trackedentity1.entity != serverplayer) {
                            chunkmap$trackedentity1.updatePlayer(serverplayer);
                        }
                    }
                }
            }
        }
    }

    protected void removeEntity(Entity pEntity) {
        if (pEntity instanceof ServerPlayer serverplayer) {
            this.updatePlayerStatus(serverplayer, false);

            for (ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
                chunkmap$trackedentity.removePlayer(serverplayer);
            }
        }

        ChunkMap.TrackedEntity chunkmap$trackedentity1 = this.entityMap.remove(pEntity.getId());
        if (chunkmap$trackedentity1 != null) {
            chunkmap$trackedentity1.broadcastRemoved();
        }
    }

    protected void tick() {
        for (ServerPlayer serverplayer : this.playerMap.getAllPlayers()) {
            this.updateChunkTracking(serverplayer);
        }

        List<ServerPlayer> list = Lists.newArrayList();
        List<ServerPlayer> list1 = this.level.players();

        for (ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
            SectionPos sectionpos = chunkmap$trackedentity.lastSectionPos;
            SectionPos sectionpos1 = SectionPos.of(chunkmap$trackedentity.entity);
            boolean flag = !Objects.equals(sectionpos, sectionpos1);
            if (flag) {
                chunkmap$trackedentity.updatePlayers(list1);
                Entity entity = chunkmap$trackedentity.entity;
                if (entity instanceof ServerPlayer) {
                    list.add((ServerPlayer)entity);
                }

                chunkmap$trackedentity.lastSectionPos = sectionpos1;
            }

            if (flag || this.distanceManager.inEntityTickingRange(sectionpos1.chunk().toLong())) {
                chunkmap$trackedentity.serverEntity.sendChanges();
            }
        }

        if (!list.isEmpty()) {
            for (ChunkMap.TrackedEntity chunkmap$trackedentity1 : this.entityMap.values()) {
                chunkmap$trackedentity1.updatePlayers(list);
            }
        }
    }

    public void broadcast(Entity pEntity, Packet<?> pPacket) {
        ChunkMap.TrackedEntity chunkmap$trackedentity = this.entityMap.get(pEntity.getId());
        if (chunkmap$trackedentity != null) {
            chunkmap$trackedentity.broadcast(pPacket);
        }
    }

    protected void broadcastAndSend(Entity pEntity, Packet<?> pPacket) {
        ChunkMap.TrackedEntity chunkmap$trackedentity = this.entityMap.get(pEntity.getId());
        if (chunkmap$trackedentity != null) {
            chunkmap$trackedentity.broadcastAndSend(pPacket);
        }
    }

    public void resendBiomesForChunks(List<ChunkAccess> pChunks) {
        Map<ServerPlayer, List<LevelChunk>> map = new HashMap<>();

        for (ChunkAccess chunkaccess : pChunks) {
            ChunkPos chunkpos = chunkaccess.getPos();
            LevelChunk levelchunk;
            if (chunkaccess instanceof LevelChunk levelchunk1) {
                levelchunk = levelchunk1;
            } else {
                levelchunk = this.level.getChunk(chunkpos.x, chunkpos.z);
            }

            for (ServerPlayer serverplayer : this.getPlayers(chunkpos, false)) {
                map.computeIfAbsent(serverplayer, playerIn -> new ArrayList<>()).add(levelchunk);
            }
        }

        map.forEach((playerIn, chunks2In) -> playerIn.connection.send(ClientboundChunksBiomesPacket.forChunks((List<LevelChunk>)chunks2In)));
    }

    protected PoiManager getPoiManager() {
        return this.poiManager;
    }

    public String getStorageName() {
        return this.storageName;
    }

    void onFullChunkStatusChange(ChunkPos pChunkPos, FullChunkStatus pFullChunkStatus) {
        this.chunkStatusListener.onChunkStatusChange(pChunkPos, pFullChunkStatus);
    }

    public void waitForLightBeforeSending(ChunkPos pChunkPos, int pRange) {
        int i = pRange + 1;
        ChunkPos.rangeClosed(pChunkPos, i).forEach(chunkPos2In -> {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(chunkPos2In.toLong());
            if (chunkholder != null) {
                chunkholder.addSendDependency(this.lightEngine.waitForPendingTasks(chunkPos2In.x, chunkPos2In.z));
            }
        });
    }

    class DistanceManager extends net.minecraft.server.level.DistanceManager {
        protected DistanceManager(final Executor pDispatcher, final Executor pMainThreadExecutor) {
            super(pDispatcher, pMainThreadExecutor);
        }

        @Override
        protected boolean isChunkToRemove(long pChunkPos) {
            return ChunkMap.this.toDrop.contains(pChunkPos);
        }

        @Nullable
        @Override
        protected ChunkHolder getChunk(long pChunkPos) {
            return ChunkMap.this.getUpdatingChunkIfPresent(pChunkPos);
        }

        @Nullable
        @Override
        protected ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel) {
            return ChunkMap.this.updateChunkScheduling(pChunkPos, pNewLevel, pHolder, pOldLevel);
        }
    }

    class TrackedEntity {
        final ServerEntity serverEntity;
        final Entity entity;
        private final int range;
        SectionPos lastSectionPos;
        private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

        public TrackedEntity(final Entity pEntity, final int pRange, final int pUpdateInterval, final boolean pTrackDelta) {
            this.serverEntity = new ServerEntity(ChunkMap.this.level, pEntity, pUpdateInterval, pTrackDelta, this::broadcast);
            this.entity = pEntity;
            this.range = pRange;
            this.lastSectionPos = SectionPos.of(pEntity);
        }

        @Override
        public boolean equals(Object pOther) {
            return pOther instanceof ChunkMap.TrackedEntity ? ((ChunkMap.TrackedEntity)pOther).entity.getId() == this.entity.getId() : false;
        }

        @Override
        public int hashCode() {
            return this.entity.getId();
        }

        public void broadcast(Packet<?> p_140490_) {
            for (ServerPlayerConnection serverplayerconnection : this.seenBy) {
                serverplayerconnection.send(p_140490_);
            }
        }

        public void broadcastAndSend(Packet<?> pPacket) {
            this.broadcast(pPacket);
            if (this.entity instanceof ServerPlayer) {
                ((ServerPlayer)this.entity).connection.send(pPacket);
            }
        }

        public void broadcastRemoved() {
            for (ServerPlayerConnection serverplayerconnection : this.seenBy) {
                this.serverEntity.removePairing(serverplayerconnection.getPlayer());
            }
        }

        public void removePlayer(ServerPlayer pPlayer) {
            if (this.seenBy.remove(pPlayer.connection)) {
                this.serverEntity.removePairing(pPlayer);
            }
        }

        public void updatePlayer(ServerPlayer pPlayer) {
            if (pPlayer != this.entity) {
                Vec3 vec3 = pPlayer.position().subtract(this.entity.position());
                int i = ChunkMap.this.getPlayerViewDistance(pPlayer);
                double d0 = (double)Math.min(this.getEffectiveRange(), i * 16);
                double d1 = vec3.x * vec3.x + vec3.z * vec3.z;
                double d2 = d0 * d0;
                boolean flag = d1 <= d2
                    && this.entity.broadcastToPlayer(pPlayer)
                    && ChunkMap.this.isChunkTracked(pPlayer, this.entity.chunkPosition().x, this.entity.chunkPosition().z);
                if (flag) {
                    if (this.seenBy.add(pPlayer.connection)) {
                        this.serverEntity.addPairing(pPlayer);
                    }
                } else if (this.seenBy.remove(pPlayer.connection)) {
                    this.serverEntity.removePairing(pPlayer);
                }
            }
        }

        private int scaledRange(int pTrackingDistance) {
            return ChunkMap.this.level.getServer().getScaledTrackingDistance(pTrackingDistance);
        }

        private int getEffectiveRange() {
            int i = this.range;
            if (!this.entity.getPassengers().isEmpty()) {
                for (Entity entity : this.entity.getIndirectPassengers()) {
                    int j = entity.getType().clientTrackingRange() * 16;
                    if (j > i) {
                        i = j;
                    }
                }
            }

            return this.scaledRange(i);
        }

        public void updatePlayers(List<ServerPlayer> pPlayersList) {
            for (ServerPlayer serverplayer : pPlayersList) {
                this.updatePlayer(serverplayer);
            }
        }
    }
}