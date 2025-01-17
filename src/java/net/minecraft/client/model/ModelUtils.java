package net.minecraft.client.model;


public class ModelUtils {
    public static float rotlerpRad(float pMin, float pMax, float pDelta) {
        float f = pMax - pMin;

        while (f < (float) -Math.PI) {
            f += (float) (Math.PI * 2);
        }

        while (f >= (float) Math.PI) {
            f -= (float) (Math.PI * 2);
        }

        return pMin + pDelta * f;
    }
}