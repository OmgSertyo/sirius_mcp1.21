package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public record VertexFormatElement(
    int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count, String name, int attributeIndex
) {
    public static final int MAX_COUNT = 32;
    private static final VertexFormatElement[] BY_ID = new VertexFormatElement[32];
    private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(32);
    public static final VertexFormatElement POSITION = register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3, "POSITION_3F", 0);
    public static final VertexFormatElement COLOR = register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4, "COLOR_4UB", 1);
    public static final VertexFormatElement UV0 = register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2, "TEX_2F", 2);
    public static final VertexFormatElement UV = UV0;
    public static final VertexFormatElement UV1 = register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2, "TEX_2S", 3);
    public static final VertexFormatElement UV2 = register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2, "TEX_2SB", 4);
    public static final VertexFormatElement NORMAL = register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3, "NORMAL_3B", 5);
    public static final VertexFormatElement PADDING = register(6, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.PADDING, 1, "PADDING_1B", -1);

    public VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        this(id, index, type, usage, count, null, -1);
    }

    public VertexFormatElement(
        int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count, String name, int attributeIndex
    ) {
        if (id < 0 || id >= BY_ID.length) {
            throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
        } else if (!this.supportsUsage(index, usage)) {
            throw new IllegalStateException("Multiple vertex elements of the same type other than UVs are not supported");
        } else {
            this.id = id;
            this.index = index;
            this.type = type;
            this.usage = usage;
            this.count = count;
            this.name = name;
            this.attributeIndex = attributeIndex;
        }
    }

    public static VertexFormatElement register(
        int pId, int pIndex, VertexFormatElement.Type pType, VertexFormatElement.Usage pUsage, int pCount
    ) {
        return register(pId, pIndex, pType, pUsage, pCount, null, -1);
    }

    public static VertexFormatElement register(
        int p_340492_0_,
        int p_340492_1_,
        VertexFormatElement.Type p_340492_2_,
        VertexFormatElement.Usage p_340492_3_,
        int p_340492_4_,
        String name,
        int attributeIndex
    ) {
        VertexFormatElement vertexformatelement = new VertexFormatElement(p_340492_0_, p_340492_1_, p_340492_2_, p_340492_3_, p_340492_4_, name, attributeIndex);
        if (BY_ID[p_340492_0_] != null) {
            throw new IllegalArgumentException("Duplicate element registration for: " + p_340492_0_);
        } else {
            BY_ID[p_340492_0_] = vertexformatelement;
            ELEMENTS.add(vertexformatelement);
            return vertexformatelement;
        }
    }

    private boolean supportsUsage(int pIndex, VertexFormatElement.Usage pUsage) {
        return pIndex == 0 || pUsage == VertexFormatElement.Usage.UV;
    }

    @Override
    public String toString() {
        return this.name != null ? this.name : this.count + "," + this.usage + "," + this.type + " (" + this.id + ")";
    }

    public int mask() {
        return 1 << this.id;
    }

    public int byteSize() {
        return this.type.size() * this.count;
    }

    public void setupBufferState(int pStateIndex, long pOffset, int pStride) {
        this.usage.setupState.setupBufferState(this.count, this.type.glType(), pStride, pOffset, pStateIndex);
    }

    @Nullable
    public static VertexFormatElement byId(int pId) {
        return BY_ID[pId];
    }

    public static Stream<VertexFormatElement> elementsFromMask(int pMask) {
        return ELEMENTS.stream().filter(p_340316_1_ -> p_340316_1_ != null && (pMask & p_340316_1_.mask()) != 0);
    }

    public final int getElementCount() {
        return this.count;
    }

    public String getName() {
        return this.name;
    }

    public int getAttributeIndex() {
        return this.attributeIndex;
    }

    public static int getElementsCount() {
        return ELEMENTS.size();
    }

    public static enum Type {
        FLOAT(4, "Float", 5126),
        UBYTE(1, "Unsigned Byte", 5121),
        BYTE(1, "Byte", 5120),
        USHORT(2, "Unsigned Short", 5123),
        SHORT(2, "Short", 5122),
        UINT(4, "Unsigned Int", 5125),
        INT(4, "Int", 5124);

        private final int size;
        private final String name;
        private final int glType;

        private Type(final int pSize, final String pName, final int pGlType) {
            this.size = pSize;
            this.name = pName;
            this.glType = pGlType;
        }

        public int size() {
            return this.size;
        }

        public int glType() {
            return this.glType;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static enum Usage {
        POSITION(
            "Position",
            (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> GlStateManager._vertexAttribPointer(indexIn, sizeIn, typeIn, false, strideIn, offsetIn)
        ),
        NORMAL(
            "Normal", (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> GlStateManager._vertexAttribPointer(indexIn, sizeIn, typeIn, true, strideIn, offsetIn)
        ),
        COLOR(
            "Vertex Color",
            (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> GlStateManager._vertexAttribPointer(indexIn, sizeIn, typeIn, true, strideIn, offsetIn)
        ),
        UV("UV", (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> {
            if (typeIn == 5126) {
                GlStateManager._vertexAttribPointer(indexIn, sizeIn, typeIn, false, strideIn, offsetIn);
            } else {
                GlStateManager._vertexAttribIPointer(indexIn, sizeIn, typeIn, strideIn, offsetIn);
            }
        }),
        PADDING("Padding", (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> {
        }),
        GENERIC(
            "Generic", (sizeIn, typeIn, strideIn, offsetIn, indexIn) -> GlStateManager._vertexAttribPointer(indexIn, sizeIn, typeIn, false, strideIn, offsetIn)
        );

        private final String name;
        final VertexFormatElement.Usage.SetupState setupState;

        private Usage(final String pName, final VertexFormatElement.Usage.SetupState pSetupState) {
            this.name = pName;
            this.setupState = pSetupState;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @FunctionalInterface
        interface SetupState {
            void setupBufferState(int pSize, int pType, int pStride, long pPointer, int pIndex);
        }
    }
}