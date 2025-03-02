package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class LegacyStuffWrapper {
    @Deprecated
    public static int[] getPixels(ResourceManager pManager, ResourceLocation pLocation) throws IOException {
        int[] aint;
        try (
            InputStream inputstream = pManager.open(pLocation);
            NativeImage nativeimage = NativeImage.read(inputstream);
        ) {
            aint = nativeimage.makePixelArray();
        }

        return aint;
    }
}