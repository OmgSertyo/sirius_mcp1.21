package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public class LazyLoadedImage {
    private final ResourceLocation id;
    private final Resource resource;
    private final AtomicReference<NativeImage> image = new AtomicReference<>();
    private final AtomicInteger referenceCount;

    public LazyLoadedImage(ResourceLocation pId, Resource pResource, int pReferenceCount) {
        this.id = pId;
        this.resource = pResource;
        this.referenceCount = new AtomicInteger(pReferenceCount);
    }

    public NativeImage get() throws IOException {
        NativeImage nativeimage = this.image.get();
        if (nativeimage == null) {
            synchronized (this) {
                nativeimage = this.image.get();
                if (nativeimage == null) {
                    try (InputStream inputstream = this.resource.open()) {
                        nativeimage = NativeImage.read(inputstream);
                        this.image.set(nativeimage);
                    } catch (IOException ioexception) {
                        throw new IOException("Failed to load image " + this.id, ioexception);
                    }
                }
            }
        }

        return nativeimage;
    }

    public void release() {
        int i = this.referenceCount.decrementAndGet();
        if (i <= 0) {
            NativeImage nativeimage = this.image.getAndSet(null);
            if (nativeimage != null) {
                nativeimage.close();
            }
        }
    }
}