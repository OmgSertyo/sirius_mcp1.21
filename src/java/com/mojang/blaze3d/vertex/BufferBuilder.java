package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.render.BufferBuilderCache;
import net.optifine.render.MultiTextureBuilder;
import net.optifine.render.MultiTextureData;
import net.optifine.render.RenderEnv;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.SVertexFormat;
import net.optifine.util.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class BufferBuilder implements VertexConsumer {
    private static final long NOT_BUILDING = -1L;
    private static final long UNKNOWN_ELEMENT = -1L;
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private final ByteBufferBuilder buffer;
    private long vertexPointer = -1L;
    private int vertices;
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final boolean fastFormat;
    private final boolean fullFormat;
    private final int vertexSize;
    private int initialElementsToFill;
    private final int[] offsetsByElement;
    private int elementsToFill;
    private boolean building = true;
    private final RenderType renderType;
    private BufferBuilderCache cache;
    protected TextureAtlasSprite[] quadSprites;
    private TextureAtlasSprite quadSprite;
    private MultiTextureBuilder multiTextureBuilder;
    public SVertexBuilder sVertexBuilder;
    public RenderEnv renderEnv;
    public BitSet animatedSprites;
    private VertexPosition[] quadVertexPositions;
    protected MultiBufferSource.BufferSource renderTypeBuffer;
    private Vector3f midBlock;

    public BufferBuilder(ByteBufferBuilder pBuffer, VertexFormat.Mode pMode, VertexFormat pFormat) {
        this(pBuffer, pMode, pFormat, null);
    }

    public BufferBuilder(ByteBufferBuilder byteBufferIn, VertexFormat.Mode drawModeIn, VertexFormat vertexFormatIn, RenderType renderTypeIn) {
        if (!vertexFormatIn.contains(VertexFormatElement.POSITION)) {
            throw new IllegalArgumentException("Cannot build mesh with no position element");
        } else {
            this.buffer = byteBufferIn;
            this.mode = drawModeIn;
            this.format = vertexFormatIn;
            this.vertexSize = vertexFormatIn.getVertexSize();
            this.initialElementsToFill = vertexFormatIn.getElementsMask() & ~VertexFormatElement.POSITION.mask();
            if (this.format.isExtended()) {
                this.initialElementsToFill = SVertexFormat.removeExtendedElements(this.initialElementsToFill);
            }

            this.offsetsByElement = vertexFormatIn.getOffsetsByElement();
            boolean flag = vertexFormatIn == DefaultVertexFormat.NEW_ENTITY;
            boolean flag1 = vertexFormatIn == DefaultVertexFormat.BLOCK;
            this.fastFormat = flag || flag1;
            this.fullFormat = flag;
            this.renderType = renderTypeIn;
            this.cache = this.buffer.getBufferBuilderCache();
            this.multiTextureBuilder = this.cache.getMultiTextureBuilder();
            this.sVertexBuilder = this.cache.getSVertexBuilder();
            this.renderEnv = this.cache.getRenderEnv();
            this.midBlock = this.cache.getMidBlock();
            if (Config.isShaders()) {
                SVertexBuilder.endSetVertexFormat(this);
            }

            if (Config.isMultiTexture()) {
                this.initQuadSprites();
            }

            if (SmartAnimations.isActive()) {
                this.animatedSprites = this.cache.getAnimatedSprites();
                this.animatedSprites.clear();
            }

            this.checkCapacity();
        }
    }

    @Nullable
    public MeshData build() {
        this.ensureBuilding();
        this.endLastVertex();
        if (this.animatedSprites != null) {
            SmartAnimations.spritesRendered(this.animatedSprites);
        }

        MeshData meshdata = this.storeMesh();
        this.building = true;
        this.vertices = 0;
        this.quadSprite = null;
        if (this.animatedSprites != null) {
            this.animatedSprites.clear();
        }

        this.vertexPointer = -1L;
        return meshdata;
    }

    public MeshData buildOrThrow() {
        MeshData meshdata = this.build();
        if (meshdata == null) {
            throw new IllegalStateException("BufferBuilder was empty");
        } else {
            return meshdata;
        }
    }

    private void ensureBuilding() {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
    }

    @Nullable
    private MeshData storeMesh() {
        if (this.vertices == 0) {
            return null;
        } else {
            ByteBufferBuilder.Result bytebufferbuilder$result = this.buffer.build();
            if (bytebufferbuilder$result == null) {
                return null;
            } else {
                int i = this.mode.indexCount(this.vertices);
                VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(this.vertices);
                MultiTextureData multitexturedata = this.multiTextureBuilder.build(this.vertices, this.renderType, this.quadSprites, null);
                return new MeshData(
                    bytebufferbuilder$result, new MeshData.DrawState(this.format, this.vertices, i, this.mode, vertexformat$indextype), multitexturedata
                );
            }
        }
    }

    private long beginVertex() {
        return this.beginVertex(1);
    }

    private long beginVertex(int countIn) {
        this.ensureBuilding();
        this.endLastVertex();
        if (this.vertices == 0) {
            this.initPreDraw();
        }

        this.vertices += countIn;
        long i = this.buffer.reserve(this.vertexSize * countIn);
        this.checkCapacity();
        this.vertexPointer = i;
        return i;
    }

    private void initPreDraw() {
        if (Config.isShaders() && this.sVertexBuilder.getVertexFormat() != this.format) {
            SVertexBuilder.endSetVertexFormat(this);
        }
    }

    private long beginElement(VertexFormatElement pElement) {
        int i = this.elementsToFill;
        int j = i & ~pElement.mask();
        if (j == i) {
            return -1L;
        } else {
            this.elementsToFill = j;
            long k = this.vertexPointer;
            if (k == -1L) {
                throw new IllegalArgumentException("Not currently building vertex");
            } else {
                return k + (long)this.offsetsByElement[pElement.id()];
            }
        }
    }

    private void endLastVertex() {
        if (this.vertices != 0) {
            if (this.elementsToFill != 0) {
                String s = VertexFormatElement.elementsFromMask(this.elementsToFill).map(this.format::getElementName).collect(Collectors.joining(", "));
                throw new IllegalStateException("Missing elements in vertex: " + s);
            }

            if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
                long i = this.buffer.reserve(this.vertexSize);
                MemoryUtil.memCopy(i - (long)this.vertexSize, i, (long)this.vertexSize);
                this.vertices++;
            }
        }
    }

    private static void putRgba(long pPointer, int pColor) {
        int i = FastColor.ABGR32.fromArgb32(pColor);
        MemoryUtil.memPutInt(pPointer, IS_LITTLE_ENDIAN ? i : Integer.reverseBytes(i));
    }

    private static void putPackedUv(long pPointer, int pPackedUv) {
        if (IS_LITTLE_ENDIAN) {
            MemoryUtil.memPutInt(pPointer, pPackedUv);
        } else {
            MemoryUtil.memPutShort(pPointer, (short)(pPackedUv & 65535));
            MemoryUtil.memPutShort(pPointer + 2L, (short)(pPackedUv >> 16 & 65535));
        }
    }

    @Override
    public VertexConsumer addVertex(float pX, float pY, float pZ) {
        long i = this.beginVertex() + (long)this.offsetsByElement[VertexFormatElement.POSITION.id()];
        this.elementsToFill = this.initialElementsToFill;
        MemoryUtil.memPutFloat(i, pX);
        MemoryUtil.memPutFloat(i + 4L, pY);
        MemoryUtil.memPutFloat(i + 8L, pZ);
        return this;
    }

    public VertexConsumer vertex(float pX, float pY, float pZ) {
        long i = this.beginVertex() + (long)this.offsetsByElement[VertexFormatElement.POSITION.id()];
        this.elementsToFill = this.initialElementsToFill;
        MemoryUtil.memPutFloat(i, pX);
        MemoryUtil.memPutFloat(i + 4L, pY);
        MemoryUtil.memPutFloat(i + 8L, pZ);
        return this;
    }

    @Override
    public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
        long i = this.beginElement(VertexFormatElement.COLOR);
        if (i != -1L) {
            MemoryUtil.memPutByte(i, (byte)pRed);
            MemoryUtil.memPutByte(i + 1L, (byte)pGreen);
            MemoryUtil.memPutByte(i + 2L, (byte)pBlue);
            MemoryUtil.memPutByte(i + 3L, (byte)pAlpha);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    @Override
    public VertexConsumer setColor(int pColor) {
        long i = this.beginElement(VertexFormatElement.COLOR);
        if (i != -1L) {
            putRgba(i, pColor);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv(float pU, float pV) {
        if (this.quadSprite != null && this.quadSprites != null) {
            pU = this.quadSprite.toSingleU(pU);
            pV = this.quadSprite.toSingleV(pV);
            this.quadSprites[this.vertices / 4] = this.quadSprite;
        }

        long i = this.beginElement(VertexFormatElement.UV0);
        if (i != -1L) {
            MemoryUtil.memPutFloat(i, pU);
            MemoryUtil.memPutFloat(i + 4L, pV);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv1(int pU, int pV) {
        return this.uvShort((short)pU, (short)pV, VertexFormatElement.UV1);
    }

    @Override
    public VertexConsumer setOverlay(int pPackedOverlay) {
        long i = this.beginElement(VertexFormatElement.UV1);
        if (i != -1L) {
            putPackedUv(i, pPackedOverlay);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    @Override
    public VertexConsumer setUv2(int pU, int pV) {
        return this.uvShort((short)pU, (short)pV, VertexFormatElement.UV2);
    }

    @Override
    public VertexConsumer setLight(int pPackedLight) {
        long i = this.beginElement(VertexFormatElement.UV2);
        if (i != -1L) {
            putPackedUv(i, pPackedLight);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    private VertexConsumer uvShort(short pU, short pV, VertexFormatElement pElement) {
        long i = this.beginElement(pElement);
        if (i != -1L) {
            MemoryUtil.memPutShort(i, pU);
            MemoryUtil.memPutShort(i + 2L, pV);
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    @Override
    public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
        long i = this.beginElement(VertexFormatElement.NORMAL);
        if (i != -1L) {
            MemoryUtil.memPutByte(i, normalIntValue(pNormalX));
            MemoryUtil.memPutByte(i + 1L, normalIntValue(pNormalY));
            MemoryUtil.memPutByte(i + 2L, normalIntValue(pNormalZ));
        }

        if (Config.isShaders() && this.elementsToFill == 0) {
            SVertexBuilder.endAddVertex(this);
        }

        return this;
    }

    public static byte normalIntValue(float pValue) {
        return (byte)((int)(Mth.clamp(pValue, -1.0F, 1.0F) * 127.0F) & 0xFF);
    }

    @Override
    public void addVertex(
        float pX,
        float pY,
        float pZ,
        int pColor,
        float pU,
        float pV,
        int pPackedOverlay,
        int pPackedLight,
        float pNormalX,
        float pNormalY,
        float pNormalZ
    ) {
        if (this.fastFormat) {
            long i = this.beginVertex();
            MemoryUtil.memPutFloat(i + 0L, pX);
            MemoryUtil.memPutFloat(i + 4L, pY);
            MemoryUtil.memPutFloat(i + 8L, pZ);
            putRgba(i + 12L, pColor);
            MemoryUtil.memPutFloat(i + 16L, pU);
            MemoryUtil.memPutFloat(i + 20L, pV);
            long j;
            if (this.fullFormat) {
                putPackedUv(i + 24L, pPackedOverlay);
                j = i + 28L;
            } else {
                j = i + 24L;
            }

            putPackedUv(j + 0L, pPackedLight);
            MemoryUtil.memPutByte(j + 4L, normalIntValue(pNormalX));
            MemoryUtil.memPutByte(j + 5L, normalIntValue(pNormalY));
            MemoryUtil.memPutByte(j + 6L, normalIntValue(pNormalZ));
            if (Config.isShaders()) {
                SVertexBuilder.endAddVertex(this);
            }
        } else {
            VertexConsumer.super.addVertex(
                pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ
            );
        }
    }

    @Override
    public void putSprite(TextureAtlasSprite sprite) {
        if (this.animatedSprites != null && sprite != null && sprite.isTerrain() && sprite.getAnimationIndex() >= 0) {
            this.animatedSprites.set(sprite.getAnimationIndex());
        }

        if (this.quadSprites != null) {
            int i = this.vertices / 4;
            this.quadSprites[i] = sprite;
        }
    }

    @Override
    public void setSprite(TextureAtlasSprite sprite) {
        if (this.animatedSprites != null && sprite != null && sprite.isTerrain() && sprite.getAnimationIndex() >= 0) {
            this.animatedSprites.set(sprite.getAnimationIndex());
        }

        if (this.quadSprites != null) {
            this.quadSprite = sprite;
        }
    }

    @Override
    public boolean isMultiTexture() {
        return this.quadSprites != null;
    }

    @Override
    public RenderType getRenderType() {
        return this.renderType;
    }

    private void initQuadSprites() {
        if (this.renderType != null) {
            if (this.renderType.isAtlasTextureBlocks()) {
                this.quadSprites = this.cache.getQuadSprites();
            }
        }
    }

    private int getBufferQuadCapacity() {
        int i = this.buffer.getCapacity() / this.format.getVertexSize();
        return i / 4;
    }

    @Override
    public RenderEnv getRenderEnv(BlockState blockStateIn, BlockPos blockPosIn) {
        this.renderEnv.reset(blockStateIn, blockPosIn);
        return this.renderEnv;
    }

    private static void quadsToTriangles(ByteBuffer byteBuffer, VertexFormat vertexFormat, int vertexCount, ByteBuffer byteBufferTriangles) {
        int i = vertexFormat.getVertexSize();
        int j = byteBuffer.limit();
        byteBuffer.rewind();
        byteBufferTriangles.clear();

        for (int k = 0; k < vertexCount; k += 4) {
            byteBuffer.limit((k + 3) * i);
            byteBuffer.position(k * i);
            byteBufferTriangles.put(byteBuffer);
            byteBuffer.limit((k + 1) * i);
            byteBuffer.position(k * i);
            byteBufferTriangles.put(byteBuffer);
            byteBuffer.limit((k + 2 + 2) * i);
            byteBuffer.position((k + 2) * i);
            byteBufferTriangles.put(byteBuffer);
        }

        byteBuffer.limit(j);
        byteBuffer.rewind();
        byteBufferTriangles.flip();
    }

    public VertexFormat.Mode getDrawMode() {
        return this.mode;
    }

    @Override
    public int getVertexCount() {
        return this.vertices;
    }

    @Override
    public Vector3f getTempVec3f() {
        return this.cache.getTempVec3f();
    }

    @Override
    public float[] getTempFloat4(float f1, float f2, float f3, float f4) {
        float[] afloat = this.cache.getTempFloat4();
        afloat[0] = f1;
        afloat[1] = f2;
        afloat[2] = f3;
        afloat[3] = f4;
        return afloat;
    }

    @Override
    public int[] getTempInt4(int i1, int i2, int i3, int i4) {
        int[] aint = this.cache.getTempInt4();
        aint[0] = i1;
        aint[1] = i2;
        aint[2] = i3;
        aint[3] = i4;
        return aint;
    }

    public int getBufferIntSize() {
        return this.vertices * this.format.getIntegerSize();
    }

    @Override
    public MultiBufferSource.BufferSource getRenderTypeBuffer() {
        return this.renderTypeBuffer;
    }

    public void setRenderTypeBuffer(MultiBufferSource.BufferSource renderTypeBuffer) {
        this.renderTypeBuffer = renderTypeBuffer;
    }

    public boolean canAddVertexText() {
        return this.format.getVertexSize() != DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize() ? false : this.elementsToFill == 0;
    }

    public void addVertexText(Matrix4f mat4, float x, float y, float z, int col, float texU, float texV, int lightmapUV) {
        if (mat4 != null) {
            float f = MathUtils.getTransformX(mat4, x, y, z);
            float f1 = MathUtils.getTransformY(mat4, x, y, z);
            float f2 = MathUtils.getTransformZ(mat4, x, y, z);
            x = f;
            y = f1;
            z = f2;
        }

        long i = this.beginVertex();
        MemoryUtil.memPutFloat(i + 0L, x);
        MemoryUtil.memPutFloat(i + 4L, y);
        MemoryUtil.memPutFloat(i + 8L, z);
        MemoryUtil.memPutInt(i + 12L, col);
        MemoryUtil.memPutFloat(i + 16L, texU);
        MemoryUtil.memPutFloat(i + 20L, texV);
        putPackedUv(i + 24L, lightmapUV);
        if (Config.isShaders()) {
            SVertexBuilder.endAddVertex(this);
        }
    }

    @Override
    public boolean canAddVertexFast() {
        return this.fastFormat && this.elementsToFill == 0 && this.fullFormat;
    }

    @Override
    public void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals) {
        long i = this.beginVertex();
        MemoryUtil.memPutFloat(i + 0L, x);
        MemoryUtil.memPutFloat(i + 4L, y);
        MemoryUtil.memPutFloat(i + 8L, z);
        putRgba(i + 12L, color);
        MemoryUtil.memPutFloat(i + 16L, texU);
        MemoryUtil.memPutFloat(i + 20L, texV);
        putPackedUv(i + 24L, overlayUV);
        putPackedUv(i + 28L, lightmapUV);
        MemoryUtil.memPutInt(i + 32L, normals);
        if (Config.isShaders()) {
            SVertexBuilder.endAddVertex(this);
        }
    }

    @Override
    public void setQuadVertexPositions(VertexPosition[] vps) {
        this.quadVertexPositions = vps;
    }

    public VertexPosition[] getQuadVertexPositions() {
        return this.quadVertexPositions;
    }

    @Override
    public void setMidBlock(float mx, float my, float mz) {
        this.midBlock.set(mx, my, mz);
    }

    @Override
    public Vector3f getMidBlock() {
        return this.midBlock;
    }

    @Override
    public void putBulkData(ByteBuffer bufferIn) {
        if (Config.isShaders()) {
            SVertexBuilder.beginAddVertexData(this, bufferIn);
        }

        int i = bufferIn.limit() / this.format.getVertexSize();
        this.beginVertex(i);
        ByteBuffer bytebuffer = this.getByteBuffer();
        bytebuffer.position(this.buffer.getNextResultOffset());
        bytebuffer.put(bufferIn);
        bytebuffer.position(0);
        if (Config.isShaders()) {
            SVertexBuilder.endAddVertexData(this, i);
        }
    }

    @Override
    public void getBulkData(ByteBuffer bufferIn) {
        ByteBuffer bytebuffer = this.getByteBuffer();
        bytebuffer.position(this.buffer.getNextResultOffset());
        bytebuffer.limit(this.buffer.getWriteOffset());
        bufferIn.put(bytebuffer);
        bytebuffer.clear();
    }

    public VertexFormat getVertexFormat() {
        return this.format;
    }

    public int getStartPosition() {
        return this.buffer.getNextResultOffset();
    }

    public int getIntStartPosition() {
        return this.getStartPosition() / 4;
    }

    private int getNextElementBytes() {
        return this.vertices * this.format.getVertexSize();
    }

    public ByteBuffer getByteBuffer() {
        return this.buffer.getByteBuffer();
    }

    public FloatBuffer getFloatBuffer() {
        return this.buffer.getFloatBuffer();
    }

    public IntBuffer getIntBuffer() {
        return this.buffer.getIntBuffer();
    }

    private void checkCapacity() {
        if (this.quadSprites != null) {
            TextureAtlasSprite[] atextureatlassprite = this.quadSprites;
            int i = this.getBufferQuadCapacity() + 1;
            if (this.quadSprites.length < i) {
                this.quadSprites = new TextureAtlasSprite[i];
                System.arraycopy(atextureatlassprite, 0, this.quadSprites, 0, Math.min(atextureatlassprite.length, this.quadSprites.length));
                this.cache.setQuadSprites(this.quadSprites);
            }
        }
    }

    public boolean isDrawing() {
        return this.building;
    }

    @Override
    public String toString() {
        return "renderType: "
            + (this.renderType != null ? this.renderType.getName() : this.renderType)
            + ", vertexFormat: "
            + this.format.getName()
            + ", vertexSize: "
            + this.vertexSize
            + ", drawMode: "
            + this.mode
            + ", vertexCount: "
            + this.vertices
            + ", elementsLeft: "
            + Integer.bitCount(this.elementsToFill)
            + "/"
            + Integer.bitCount(this.initialElementsToFill)
            + ", byteBuffer: ("
            + this.buffer
            + ")";
    }
}