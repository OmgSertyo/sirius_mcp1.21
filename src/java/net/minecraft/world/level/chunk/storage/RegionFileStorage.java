package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.world.level.ChunkPos;

public final class RegionFileStorage implements AutoCloseable {
    public static final String ANVIL_EXTENSION = ".mca";
    private static final int MAX_CACHE_SIZE = 256;
    private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap<>();
    private final RegionStorageInfo info;
    private final Path folder;
    private final boolean sync;

    RegionFileStorage(RegionStorageInfo pInfo, Path pFolder, boolean pSync) {
        this.folder = pFolder;
        this.sync = pSync;
        this.info = pInfo;
    }

    private RegionFile getRegionFile(ChunkPos pChunkPos) throws IOException {
        long i = ChunkPos.asLong(pChunkPos.getRegionX(), pChunkPos.getRegionZ());
        RegionFile regionfile = this.regionCache.getAndMoveToFirst(i);
        if (regionfile != null) {
            return regionfile;
        } else {
            if (this.regionCache.size() >= 256) {
                this.regionCache.removeLast().close();
            }

            FileUtil.createDirectoriesSafe(this.folder);
            Path path = this.folder.resolve("r." + pChunkPos.getRegionX() + "." + pChunkPos.getRegionZ() + ".mca");
            RegionFile regionfile1 = new RegionFile(this.info, path, this.folder, this.sync);
            this.regionCache.putAndMoveToFirst(i, regionfile1);
            return regionfile1;
        }
    }

    @Nullable
    public CompoundTag read(ChunkPos pChunkPos) throws IOException {
        RegionFile regionfile = this.getRegionFile(pChunkPos);

        CompoundTag compoundtag;
        try (DataInputStream datainputstream = regionfile.getChunkDataInputStream(pChunkPos)) {
            if (datainputstream == null) {
                return null;
            }

            compoundtag = NbtIo.read(datainputstream);
        }

        return compoundtag;
    }

    public void scanChunk(ChunkPos pChunkPos, StreamTagVisitor pVisitor) throws IOException {
        RegionFile regionfile = this.getRegionFile(pChunkPos);

        try (DataInputStream datainputstream = regionfile.getChunkDataInputStream(pChunkPos)) {
            if (datainputstream != null) {
                NbtIo.parse(datainputstream, pVisitor, NbtAccounter.unlimitedHeap());
            }
        }
    }

    protected void write(ChunkPos pChunkPos, @Nullable CompoundTag pChunkData) throws IOException {
        RegionFile regionfile = this.getRegionFile(pChunkPos);
        if (pChunkData == null) {
            regionfile.clear(pChunkPos);
        } else {
            try (DataOutputStream dataoutputstream = regionfile.getChunkDataOutputStream(pChunkPos)) {
                NbtIo.write(pChunkData, dataoutputstream);
            }
        }
    }

    @Override
    public void close() throws IOException {
        ExceptionCollector<IOException> exceptioncollector = new ExceptionCollector<>();

        for (RegionFile regionfile : this.regionCache.values()) {
            try {
                regionfile.close();
            } catch (IOException ioexception) {
                exceptioncollector.add(ioexception);
            }
        }

        exceptioncollector.throwIfPresent();
    }

    public void flush() throws IOException {
        for (RegionFile regionfile : this.regionCache.values()) {
            regionfile.flush();
        }
    }

    public RegionStorageInfo info() {
        return this.info;
    }
}