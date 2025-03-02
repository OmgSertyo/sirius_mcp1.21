package net.optifine.shaders;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class SVertexAttrib {
    public int index;
    public int count;
    public VertexFormatElement.Type type;
    public int offset;

    public SVertexAttrib(int index, int count, VertexFormatElement.Type type) {
        this.index = index;
        this.count = count;
        this.type = type;
    }
}