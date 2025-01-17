package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.optifine.SmartAnimations;
import net.optifine.render.IBufferSourceListener;
import net.optifine.render.VertexBuilderDummy;
import net.optifine.util.TextureUtils;

public interface MultiBufferSource {
    static MultiBufferSource.BufferSource immediate(ByteBufferBuilder pSharedBuffer) {
        return immediateWithBuffers(Object2ObjectSortedMaps.emptyMap(), pSharedBuffer);
    }

    static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers, ByteBufferBuilder pSharedBuffer) {
        return new MultiBufferSource.BufferSource(pSharedBuffer, pFixedBuffers);
    }

    VertexConsumer getBuffer(RenderType pRenderType);

    default void flushRenderBuffers() {
    }

    default void flushCache() {
    }

    public static class BufferSource implements MultiBufferSource {
        protected final ByteBufferBuilder sharedBuffer;
        protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
        protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
        @Nullable
        protected RenderType lastSharedType;
        private final VertexConsumer DUMMY_BUFFER = new VertexBuilderDummy(this);
        private List<IBufferSourceListener> listeners = new ArrayList<>(4);
        private int maxCachedBuffers = 0;
        private Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> cachedBuffers = new Object2ObjectLinkedOpenHashMap<>();
        private Deque<BufferBuilder> freeBufferBuilders = new ArrayDeque<>();

        protected BufferSource(ByteBufferBuilder pSharedBuffer, SequencedMap<RenderType, ByteBufferBuilder> pFixedBuffers) {
            this.sharedBuffer = pSharedBuffer;
            this.fixedBuffers = new Object2ObjectLinkedOpenHashMap<>(pFixedBuffers);
        }

        @Override
        public VertexConsumer getBuffer(RenderType pRenderType) {
            this.addCachedBuffer(pRenderType);
            BufferBuilder bufferbuilder = this.startedBuilders.get(pRenderType);
            if (bufferbuilder != null && !pRenderType.canConsolidateConsecutiveGeometry()) {
                this.endBatch(pRenderType, bufferbuilder);
                bufferbuilder = null;
            }

            if (bufferbuilder != null) {
                return (VertexConsumer)(pRenderType.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder);
            } else {
                ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.get(pRenderType);
                if (bytebufferbuilder != null) {
                    bufferbuilder = new BufferBuilder(bytebufferbuilder, pRenderType.mode(), pRenderType.format(), pRenderType);
                } else {
                    if (this.lastSharedType != null) {
                        this.endBatch(this.lastSharedType);
                    }

                    bufferbuilder = new BufferBuilder(this.sharedBuffer, pRenderType.mode(), pRenderType.format(), pRenderType);
                    this.lastSharedType = pRenderType;
                }

                this.startedBuilders.put(pRenderType, bufferbuilder);
                bufferbuilder.setRenderTypeBuffer(this);
                return (VertexConsumer)(pRenderType.getTextureLocation() == TextureUtils.LOCATION_TEXTURE_EMPTY ? this.DUMMY_BUFFER : bufferbuilder);
            }
        }

        public void endLastBatch() {
            if (this.lastSharedType != null) {
                this.endBatch(this.lastSharedType);
                this.lastSharedType = null;
            }
        }

        public void endBatch() {
            if (!this.startedBuilders.isEmpty()) {
                this.endLastBatch();
                if (!this.startedBuilders.isEmpty()) {
                    for (RenderType rendertype : this.fixedBuffers.keySet()) {
                        this.endBatch(rendertype);
                        if (this.startedBuilders.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        public void endBatch(RenderType pRenderType) {
            BufferBuilder bufferbuilder = this.startedBuilders.remove(pRenderType);
            if (bufferbuilder != null) {
                this.endBatch(pRenderType, bufferbuilder);
            }
        }

        private void endBatch(RenderType pRenderType, BufferBuilder pBuilder) {
            this.fireFinish(pRenderType, pBuilder);
            MeshData meshdata = pBuilder.build();
            if (meshdata != null) {
                if (pRenderType.sortOnUpload()) {
                    ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.getOrDefault(pRenderType, this.sharedBuffer);
                    meshdata.sortQuads(bytebufferbuilder, RenderSystem.getVertexSorting());
                }

                if (pBuilder.animatedSprites != null) {
                    SmartAnimations.spritesRendered(pBuilder.animatedSprites);
                }

                pRenderType.draw(meshdata);
            }

            if (pRenderType.equals(this.lastSharedType)) {
                this.lastSharedType = null;
            }
        }

        public VertexConsumer getBuffer(ResourceLocation textureLocation, VertexConsumer bufferIn) {
            RenderType rendertype = bufferIn.getRenderType();
            if (!(rendertype instanceof RenderType.CompositeRenderType)) {
                return bufferIn;
            } else {
                textureLocation = RenderType.getCustomTexture(textureLocation);
                RenderType.CompositeRenderType rendertype$compositerendertype = (RenderType.CompositeRenderType)rendertype;
                RenderType.CompositeRenderType rendertype$compositerendertype1 = rendertype$compositerendertype.getTextured(textureLocation);
                return this.getBuffer(rendertype$compositerendertype1);
            }
        }

        public RenderType getLastRenderType() {
            return this.lastSharedType;
        }

        public BufferBuilder getStartedBuffer(RenderType renderType) {
            return this.startedBuilders.get(renderType);
        }

        @Override
        public void flushRenderBuffers() {
            RenderType rendertype = this.lastSharedType;
            BufferBuilder bufferbuilder = this.startedBuilders.get(rendertype);
            this.endBatch();
            this.restoreRenderState(rendertype, bufferbuilder);
        }

        public void restoreRenderState(RenderType renderTypeIn, BufferBuilder bufferBuilderIn) {
            if (renderTypeIn != null) {
                this.endLastBatch();
                this.lastSharedType = renderTypeIn;
                if (bufferBuilderIn != null) {
                    this.startedBuilders.put(renderTypeIn, bufferBuilderIn);
                }
            }
        }

        public void addListener(IBufferSourceListener bsl) {
            this.listeners.add(bsl);
        }

        public boolean removeListener(IBufferSourceListener bsl) {
            return this.listeners.remove(bsl);
        }

        private void fireFinish(RenderType renderTypeIn, BufferBuilder bufferIn) {
            for (int i = 0; i < this.listeners.size(); i++) {
                IBufferSourceListener ibuffersourcelistener = this.listeners.get(i);
                ibuffersourcelistener.finish(renderTypeIn, bufferIn);
            }
        }

        public VertexConsumer getDummyBuffer() {
            return this.DUMMY_BUFFER;
        }

        public void enableCache() {
        }

        @Override
        public void flushCache() {
            int i = this.maxCachedBuffers;
            this.setMaxCachedBuffers(0);
            this.setMaxCachedBuffers(i);
        }

        public void disableCache() {
            this.setMaxCachedBuffers(0);
        }

        private void setMaxCachedBuffers(int maxCachedBuffers) {
            this.maxCachedBuffers = Math.max(maxCachedBuffers, 0);
            this.trimCachedBuffers();
        }

        private void addCachedBuffer(RenderType rt) {
            if (this.maxCachedBuffers > 0) {
                this.cachedBuffers.getAndMoveToLast(rt);
                if (!this.fixedBuffers.containsKey(rt)) {
                    if (this.shouldCache(rt)) {
                        this.trimCachedBuffers();
                        BufferBuilder bufferbuilder = this.freeBufferBuilders.pollLast();
                        this.cachedBuffers.put(rt, bufferbuilder);
                    }
                }
            }
        }

        private boolean shouldCache(RenderType rt) {
            ResourceLocation resourcelocation = rt.getTextureLocation();
            if (resourcelocation == null) {
                return false;
            } else if (!rt.canConsolidateConsecutiveGeometry()) {
                return false;
            } else {
                String s = resourcelocation.getPath();
                if (s.startsWith("skins/")) {
                    return false;
                } else if (s.startsWith("capes/")) {
                    return false;
                } else if (s.startsWith("capeof/")) {
                    return false;
                } else if (s.startsWith("textures/entity/horse/")) {
                    return false;
                } else {
                    return s.startsWith("textures/entity/villager/") ? false : !s.startsWith("textures/entity/warden/");
                }
            }
        }

        private void trimCachedBuffers() {
            while (this.cachedBuffers.size() > this.maxCachedBuffers) {
                RenderType rendertype = this.cachedBuffers.firstKey();
                if (rendertype == this.lastSharedType) {
                    return;
                }

                this.removeCachedBuffer(rendertype);
            }
        }

        private void removeCachedBuffer(RenderType rt) {
        }
    }
}