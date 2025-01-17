package omg.sertyo.utility.render;

import java.awt.Color;


import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;

import static omg.sertyo.utility.Utility.mc;


public class RoundedProgram2 extends GlProgram {
    private Uniform uSize;
    private Uniform uLocation;
    private Uniform size;
    private Uniform color1;
    private Uniform color2;
    private Uniform color3;
    private Uniform color4;
    private RenderTarget input;

    public RoundedProgram2() {
        super(new ResourceLocation("minecraft", "gradientround"), DefaultVertexFormat.POSITION);

    }
    protected void setup() {
        System.out.println("Trying to get uniform: uSize");
        this.uSize = this.findUniform("uSize");
        this.uLocation = this.findUniform("uLocation");
        this.size = this.findUniform("Size");
        if (this.size == null) {
            System.err.println("Failed to get uniform uSize");
        }
        this.color1 = this.findUniform("color1");
        this.color2 = this.findUniform("color2");
        this.color3 = this.findUniform("color3");
        this.color4 = this.findUniform("color4");
        var window = Minecraft.getInstance().getWindow();

        input = new RenderTarget(Minecraft.ON_OSX);
        input.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);

    }
    public void setParameters(float x, float y, float width, float height, float radius, Color color1, Color color2, Color color3, Color color4) {
        System.out.println("Trying to get uniform: uSize");
        this.uSize = this.findUniform("uSize");
        this.uLocation = this.findUniform("uLocation");
        this.size = this.findUniform("Size");
        if (this.size == null) {
            System.err.println("Failed to get uniform uSize");
        }
        this.color1 = this.findUniform("color1");
        this.color2 = this.findUniform("color2");
        this.color3 = this.findUniform("color3");
        this.color4 = this.findUniform("color4");
        var window = Minecraft.getInstance().getWindow();
        input = new RenderTarget(Minecraft.ON_OSX);
        input.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
        float i = (float) mc.getWindow().getGuiScale();
        System.out.println("size " + size);
        this.size.set(radius * i);
        this.uSize.set(width * i, height * i);
        this.uLocation.set(x * i, -y * i + (float)mc.getWindow().getGuiScaledHeight() * i - height * i);
        this.color1.set((float)color1.getRed() / 255.0F, (float)color1.getGreen() / 255.0F, (float)color1.getBlue() / 255.0F, (float)color1.getAlpha() / 255.0F);
        this.color2.set((float)color2.getRed() / 255.0F, (float)color2.getGreen() / 255.0F, (float)color2.getBlue() / 255.0F, (float)color2.getAlpha() / 255.0F);
        this.color3.set((float)color3.getRed() / 255.0F, (float)color3.getGreen() / 255.0F, (float)color3.getBlue() / 255.0F, (float)color3.getAlpha() / 255.0F);
        this.color4.set((float)color4.getRed() / 255.0F, (float)color4.getGreen() / 255.0F, (float)color4.getBlue() / 255.0F, (float)color4.getAlpha() / 255.0F);
    }

    public void use() {
        var buffer = Minecraft.getInstance().getMainRenderTarget();

        this.input.bindWrite(false);
        GL30.glBindFramebuffer(36008, buffer.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, buffer.width, buffer.height, 0, 0, buffer.width, buffer.height, 16384, 9729);
        buffer.bindWrite(false);
        super.use();
    }

}
