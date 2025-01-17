package omg.sertyo.utility;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;


public interface Utility {
    Minecraft mc = Minecraft.getInstance();
    Window sr = mc.getWindow();
    Tesselator TESSELLATOR = Tesselator.getInstance();
    BufferBuilder BUILDER = TESSELLATOR.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);


}
