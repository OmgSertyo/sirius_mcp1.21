package com.mojang.blaze3d.shaders;


public enum FogShape {
    SPHERE(0),
    CYLINDER(1);

    private final int index;

    private FogShape(final int pIndex) {
        this.index = pIndex;
    }

    public int getIndex() {
        return this.index;
    }
}