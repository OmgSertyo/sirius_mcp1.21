package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.optifine.BlockPosM;
import net.optifine.Vec3M;
import org.slf4j.Logger;

public class SectionOcclusionGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    @Nullable
    private Future<?> fullUpdateTask;
    @Nullable
    private ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private LevelRenderer levelRenderer;

    public void waitAndReset(@Nullable ViewArea pViewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception exception) {
                LOGGER.warn("Full update failed", (Throwable)exception);
            }
        }

        this.viewArea = pViewArea;
        this.levelRenderer = Minecraft.getInstance().levelRenderer;
        if (pViewArea != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(pViewArea.sections.length));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
        }
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections) {
        this.addSectionsInFrustum(pFrustum, pSections, true, -1);
    }

    public void addSectionsInFrustum(Frustum frustumIn, List<SectionRenderDispatcher.RenderSection> sectionsIn, boolean updateSections, int maxChunkDistance) {
        List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTerrain();
        List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfosTileEntities();
        int i = (int)frustumIn.getCameraX() >> 4 << 4;
        int j = (int)frustumIn.getCameraY() >> 4 << 4;
        int k = (int)frustumIn.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;

        for (SectionOcclusionGraph.Node sectionocclusiongraph$node : this.currentGraph.get().storage().renderSections) {
            if (frustumIn.isVisible(sectionocclusiongraph$node.section.getBoundingBox())) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionocclusiongraph$node.section.getOrigin();
                    int i1 = i - blockpos.getX();
                    int j1 = j - blockpos.getY();
                    int k1 = k - blockpos.getZ();
                    int l1 = i1 * i1 + j1 * j1 + k1 * k1;
                    if (l1 > l) {
                        continue;
                    }
                }

                if (updateSections) {
                    sectionsIn.add(sectionocclusiongraph$node.section);
                }

                SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionocclusiongraph$node.section.getCompiled();
                if (!sectionrenderdispatcher$compiledsection.hasNoRenderableLayers()) {
                    list.add(sectionocclusiongraph$node.section);
                }

                if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty()) {
                    list1.add(sectionocclusiongraph$node.section);
                }
            }
        }
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkLoaded(ChunkPos pChunkPos) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            this.addNeighbors(sectionocclusiongraph$graphevents, pChunkPos);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            this.addNeighbors(sectionocclusiongraph$graphevents1, pChunkPos);
        }
    }

    public void onSectionCompiled(SectionRenderDispatcher.RenderSection pSection) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(pSection);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(pSection);
        }

        if (pSection.getCompiled().hasTerrainBlockEntities()) {
            this.needsFrustumUpdate.set(true);
        }
    }

    public void update(boolean pSmartCull, Camera pCamera, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
            this.scheduleFullUpdate(pSmartCull, pCamera, vec3);
        }

        this.runPartialUpdate(pSmartCull, pFrustum, pSections, vec3);
    }

    private void scheduleFullUpdate(boolean pSmartCull, Camera pCamera, Vec3 pCameraPosition) {
        this.needsFullUpdate = false;
        this.fullUpdateTask = Util.backgroundExecutor().submit(() -> {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea.sections.length);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(pCamera, queue);
            queue.forEach(nodeIn -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(nodeIn.section, nodeIn));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, sectionIn -> {
            });
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        });
    }

    private void runPartialUpdate(boolean pSmartCull, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections, Vec3 pCameraPosition) {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                    .sectionToNodeMap
                    .get(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfos();
            List<SectionRenderDispatcher.RenderSection> list2 = this.levelRenderer.getRenderInfosTerrain();
            List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTileEntities();
            Frustum frustum = LevelRenderer.offsetFrustum(pFrustum);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = sectionIn -> {
                if (frustum.isVisible(sectionIn.getBoundingBox())) {
                    pSections.add(sectionIn);
                    if (sectionIn == list1) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionIn.compiled.get();
                        if (!sectionrenderdispatcher$compiledsection.hasNoRenderableLayers()) {
                            list2.add(sectionIn);
                        }

                        if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty()) {
                            list.add(sectionIn);
                        }
                    }
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, consumer);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState pGraphState) {
        LongIterator longiterator = pGraphState.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext()) {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = pGraphState.storage.chunksWaitingForNeighbors.get(i);
            if (list != null && list.get(0).hasAllNeighbors()) {
                pGraphState.events.sectionsToPropagateFrom.addAll(list);
                pGraphState.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        pGraphState.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents pGraphEvents, ChunkPos pChunkPos) {
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera pCamera, Queue<SectionOcclusionGraph.Node> pNodeQueue) {
        int i = 16;
        Vec3 vec3 = pCamera.getPosition();
        BlockPos blockpos = pCamera.getBlockPosition();
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockpos);
        if (sectionrenderdispatcher$rendersection == null) {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = blockpos.getY() > levelheightaccessor.getMinBuildHeight();
            int j = flag ? levelheightaccessor.getMaxBuildHeight() - 8 : levelheightaccessor.getMinBuildHeight() + 8;
            int k = Mth.floor(vec3.x / 16.0) * 16;
            int l = Mth.floor(vec3.z / 16.0) * 16;
            int i1 = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();

            for (int j1 = -i1; j1 <= i1; j1++) {
                for (int k1 = -i1; k1 <= i1; k1++) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                        .getRenderSectionAt(new BlockPos(k + SectionPos.sectionToBlockCoord(j1, 8), j, l + SectionPos.sectionToBlockCoord(k1, 8)));
                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(blockpos, sectionrenderdispatcher$rendersection1.getOrigin())) {
                        Direction direction = flag ? Direction.DOWN : Direction.UP;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionrenderdispatcher$rendersection1.getRenderInfo(direction, 0);
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (j1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        } else if (j1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (k1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        } else if (k1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(nodeIn -> blockpos.distSqr(nodeIn.section.getOrigin().offset(8, 8, 8))));
            pNodeQueue.addAll(list);
        } else {
            pNodeQueue.add(sectionrenderdispatcher$rendersection.getRenderInfo(null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage pGraphStorage,
        Vec3 pCameraPosition,
        Queue<SectionOcclusionGraph.Node> pNodeQueue,
        boolean pSmartCull,
        Consumer<SectionRenderDispatcher.RenderSection> pSections
    ) {
        int i = 16;
        BlockPos blockpos = new BlockPos(
            Mth.floor(pCameraPosition.x / 16.0) * 16, Mth.floor(pCameraPosition.y / 16.0) * 16, Mth.floor(pCameraPosition.z / 16.0) * 16
        );
        BlockPos blockpos1 = blockpos.offset(8, 8, 8);

        while (!pNodeQueue.isEmpty()) {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = pNodeQueue.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
            if (pGraphStorage.renderSections.add(sectionocclusiongraph$node)) {
                pSections.accept(sectionocclusiongraph$node.section);
            }

            boolean flag = Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getX() - blockpos.getX()) > 60
                || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getY() - blockpos.getY()) > 60
                || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getZ() - blockpos.getZ()) > 60;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                    blockpos, sectionrenderdispatcher$rendersection, direction
                );
                if (sectionrenderdispatcher$rendersection1 != null && (!pSmartCull || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
                    if (pSmartCull && sectionocclusiongraph$node.hasSourceDirections()) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionrenderdispatcher$rendersection.getCompiled();
                        boolean flag1 = false;

                        for (int j = 0; j < DIRECTIONS.length; j++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(j)
                                && sectionrenderdispatcher$compiledsection.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction)) {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (pSmartCull && flag) {
                        BlockPos blockpos2 = sectionrenderdispatcher$rendersection1.getOrigin();
                        int l = (
                                direction.getAxis() == Direction.Axis.X
                                    ? blockpos1.getX() > blockpos2.getX()
                                    : blockpos1.getX() < blockpos2.getX()
                            )
                            ? 16
                            : 0;
                        int i1 = (
                                direction.getAxis() == Direction.Axis.Y
                                    ? blockpos1.getY() > blockpos2.getY()
                                    : blockpos1.getY() < blockpos2.getY()
                            )
                            ? 16
                            : 0;
                        int k = (
                                direction.getAxis() == Direction.Axis.Z
                                    ? blockpos1.getZ() > blockpos2.getZ()
                                    : blockpos1.getZ() < blockpos2.getZ()
                            )
                            ? 16
                            : 0;
                        Vec3M vec3m = pGraphStorage.vec3M1
                            .set((double)(blockpos2.getX() + l), (double)(blockpos2.getY() + i1), (double)(blockpos2.getZ() + k));
                        Vec3M vec3m1 = pGraphStorage.vec3M2.set(pCameraPosition).subtract(vec3m).normalize().scale(CEILED_SECTION_DIAGONAL);
                        boolean flag2 = true;

                        while (pGraphStorage.vec3M3.set(pCameraPosition).subtract(vec3m).lengthSqr() > 3600.0) {
                            vec3m = vec3m.add(vec3m1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                            if (vec3m.y > (double)levelheightaccessor.getMaxBuildHeight() || vec3m.y < (double)levelheightaccessor.getMinBuildHeight()) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                .getRenderSectionAt(pGraphStorage.blockPosM1.setXyz(vec3m.x, vec3m.y, vec3m.z));
                            if (sectionrenderdispatcher$rendersection2 == null || pGraphStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null
                                )
                             {
                                flag2 = false;
                                break;
                            }
                        }

                        if (!flag2) {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = pGraphStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
                    if (sectionocclusiongraph$node1 != null) {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    } else {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = sectionrenderdispatcher$rendersection1.getRenderInfo(
                            direction, sectionocclusiongraph$node.step + 1
                        );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                            pNodeQueue.add(sectionocclusiongraph$node2);
                            pGraphStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        } else if (this.isInViewDistance(blockpos, sectionrenderdispatcher$rendersection1.getOrigin())) {
                            pGraphStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            pGraphStorage.chunksWaitingForNeighbors
                                .computeIfAbsent(ChunkPos.asLong(sectionrenderdispatcher$rendersection1.getOrigin()), posLongIn -> new ArrayList<>())
                                .add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(BlockPos pPos, BlockPos pOrigin) {
        int i = SectionPos.blockToSectionCoord(pPos.getX());
        int j = SectionPos.blockToSectionCoord(pPos.getZ());
        int k = SectionPos.blockToSectionCoord(pOrigin.getX());
        int l = SectionPos.blockToSectionCoord(pOrigin.getZ());
        return ChunkTrackingView.isInViewDistance(i, j, this.viewArea.getViewDistance(), k, l);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(BlockPos pPos, SectionRenderDispatcher.RenderSection pSection, Direction pDirection) {
        BlockPos blockpos = pSection.getRelativeOrigin(pDirection);
        ClientLevel clientlevel = this.levelRenderer.level;
        if (blockpos.getY() < clientlevel.getMinBuildHeight() || blockpos.getY() >= clientlevel.getMaxBuildHeight()) {
            return null;
        } else if (Mth.abs(pPos.getY() - blockpos.getY()) > this.levelRenderer.renderDistance) {
            return null;
        } else {
            int i = pPos.getX() - blockpos.getX();
            int j = pPos.getZ() - blockpos.getZ();
            int k = i * i + j * j;
            return k > this.levelRenderer.renderDistanceXZSq ? null : this.viewArea.getRenderSectionAt(blockpos);
        }
    }

    @Nullable
    @VisibleForDebug
    protected SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection pSection) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(pSection);
    }

    public boolean needsFrustumUpdate() {
        return this.needsFrustumUpdate.get();
    }

    public void setNeedsFrustumUpdate(boolean val) {
        this.needsFrustumUpdate.set(val);
    }

    static record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        public GraphEvents() {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    static record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
        public GraphState(int pSize) {
            this(new SectionOcclusionGraph.GraphStorage(pSize), new SectionOcclusionGraph.GraphEvents());
        }
    }

    static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Set<SectionOcclusionGraph.Node> renderSections;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;
        public final Vec3M vec3M1 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M2 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M3 = new Vec3M(0.0, 0.0, 0.0);
        public final BlockPosM blockPosM1 = new BlockPosM();

        public GraphStorage(int pSize) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(pSize);
            this.renderSections = new ObjectLinkedOpenHashSet<>(pSize);
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }

        @Override
        public String toString() {
            return "sectionToNode: " + this.sectionToNodeMap + ", renderSections: " + this.renderSections + ", sectionsWaiting: " + this.chunksWaitingForNeighbors;
        }
    }

    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        public final SectionRenderDispatcher.RenderSection section;
        private int sourceDirections;
        int directions;
        @VisibleForDebug
        protected int step;

        public Node(SectionRenderDispatcher.RenderSection pSection, @Nullable Direction pSourceDirection, int pStep) {
            this.section = pSection;
            if (pSourceDirection != null) {
                this.addSourceDirection(pSourceDirection);
            }

            this.step = pStep;
        }

        void setDirections(int directionsIn, Direction directionIn) {
            this.directions = this.directions | directionsIn | 1 << directionIn.ordinal();
        }

        public void initialize(Direction facingIn, int counter) {
            this.sourceDirections = facingIn != null ? 1 << facingIn.ordinal() : 0;
            this.directions = 0;
            this.step = counter;
        }

        @Override
        public String toString() {
            return this.section.getOrigin() + "";
        }

        boolean hasDirection(Direction pDirection) {
            return (this.directions & 1 << pDirection.ordinal()) > 0;
        }

        void addSourceDirection(Direction pSourceDirection) {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << pSourceDirection.ordinal());
        }

        @VisibleForDebug
        protected boolean hasSourceDirection(int pDirection) {
            return (this.sourceDirections & 1 << pDirection) > 0;
        }

        boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return this.section.getOrigin().hashCode();
        }

        @Override
        public boolean equals(Object pOther) {
            return pOther instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node
                ? this.section.getOrigin().equals(sectionocclusiongraph$node.section.getOrigin())
                : false;
        }
    }

    static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int pSize) {
            this.nodes = new SectionOcclusionGraph.Node[pSize];
        }

        public void put(SectionRenderDispatcher.RenderSection pSection, SectionOcclusionGraph.Node pNode) {
            this.nodes[pSection.index] = pNode;
        }

        @Nullable
        public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection pSection) {
            int i = pSection.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }
    }
}