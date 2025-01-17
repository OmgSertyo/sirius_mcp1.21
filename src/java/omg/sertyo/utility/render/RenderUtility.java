package omg.sertyo.utility.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import omg.sertyo.utility.Utility;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class RenderUtility implements Utility {
    static final Stack clipStack;

    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    public static void addWindow(@NotNull PoseStack poseStack, @NotNull Rectangle rectangle) {
        Matrix4f matrix4f = poseStack.last().pose();
        Vector4f vector4f = new Vector4f(rectangle.x, rectangle.y, 0.0f, 1.0f);
        Vector4f vector4f2 = new Vector4f(rectangle.x1, rectangle.y1, 0.0f, 1.0f);
        vector4f.mulTranspose((Matrix4fc)matrix4f);
        vector4f2.mulTranspose((Matrix4fc)matrix4f);
        float f = vector4f.x;
        float f2 = vector4f.y;
        float f3 = vector4f2.x;
        float f4 = vector4f2.y;
        Rectangle rectangle2 = new Rectangle(f, f2, f3, f4);
        if (clipStack.empty()) {
            clipStack.push(rectangle2);
            beginScissor(rectangle2.x, rectangle2.y, rectangle2.x1, rectangle2.y1);
        } else {
            Rectangle rectangle3 = (Rectangle)clipStack.peek();
            float f5 = rectangle3.x;
            float f6 = rectangle3.y;
            float f7 = rectangle3.x1;
            float f8 = rectangle3.y1;
            float f9 = Mth.clamp((float)rectangle2.x, (float)f5, (float)f7);
            float f10 = Mth.clamp((float)rectangle2.y, (float)f6, (float)f8);
            float f11 = Mth.clamp((float)rectangle2.x1, (float)f9, (float)f7);
            float f12 = Mth.clamp((float)rectangle2.y1, (float)f10, (float)f8);
            clipStack.push(new Rectangle(f9, f10, f11, f12));
            beginScissor(f9, f10, f11, f12);
        }
    }
    public static void popWindow(PoseStack stack) {
        clipStack.pop();
        if (clipStack.empty()) {
            endScissor(stack);
        } else {
            Rectangle rectangle = (Rectangle)clipStack.peek();
            beginScissor(rectangle.x, rectangle.y, rectangle.x1, rectangle.y1);
        }
    }
    public static void beginScissor(double d, double d2, double d3, double d4) {
        double d5 = d3 - d;
        double d6 = d4 - d2;
        d5 = Math.max(0.0, d5);
        d6 = Math.max(0.0, d6);
        float f = (float) mc.getWindow().getGuiScale();
        int n = (int)(((double)mc.getWindow().getGuiScaledHeight() - (d2 + d6)) * (double)f);
        RenderSystem.enableScissor((int)((int)(d * (double)f)), (int)n, (int)((int)(d5 * (double)f)), (int)((int)(d6 * (double)f)));
    }

    public static void endScissor(PoseStack stack) {
        stack.popPose();
    }

    public static void scaleStart(PoseStack stack, float x, float y, float scale) {
        // Используйте RenderSystem вместо прямого вызова OpenGL функций
        stack.pushPose();  // Push текущую матрицу
        stack.translate(x, y, 0.0F);  // Трансляция в координаты
        stack.scale(scale, scale, 1.0F);  // Масштабирование
        stack.translate(-x, -y, 0.0F);  // Возвращаемся в исходное положение
    }
    public static void renderRoundedQuadInternal2(Matrix4f matrix, float cr, float cg, float cb, float ca, float cr1, float cg1, float cb1, float ca1, float cr2, float cg2, float cb2, float ca2, float cr3, float cg3, float cb3, float ca3, double fromX, double fromY, double toX, double toY, double radC1) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        double[][] map = new double[][]{new double[]{toX - radC1, toY - radC1, radC1}, new double[]{toX - radC1, fromY + radC1, radC1}, new double[]{fromX + radC1, fromY + radC1, radC1}, new double[]{fromX + radC1, toY - radC1, radC1}};

        for (int i = 0; i < 4; i++) {
            double[] current = map[i];
            double rad = current[2];
            for (double r = i * 90; r < (90 + i * 90); r += 10) {
                float rad1 = (float) Math.toRadians(r);
                float sin = (float) (Math.sin(rad1) * rad);
                float cos = (float) (Math.cos(rad1) * rad);
                switch (i) {
                    case 0 ->
                            bufferBuilder.addVertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr1, cg1, cb1, ca1);
                    case 1 ->
                            bufferBuilder.addVertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr, cg, cb, ca);
                    case 2 ->
                            bufferBuilder.addVertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr2, cg2, cb2, ca2);
                    default ->
                            bufferBuilder.addVertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr3, cg3, cb3, ca3);
                }
            }
        }
        BufferUploader.drawWithShader(bufferBuilder.build());
    }
    public static void renderRoundedQuad2(PoseStack matrices, int c, int c2, int c3, int c4, double fromX, double fromY, double toX, double toY, double radius) {
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        renderRoundedQuadInternal2(matrices.last().pose(), c, c, c, c, c2, c2 , c2 , c2 , c3 , c3 , c3 , c3, c4 , c4, c4, c4, fromX, fromY, toX, toY, radius);
        endRender();
    }
    static Shader RECTANGLE_SHADER = Shader.create("rectangle", DefaultVertexFormat.POSITION_TEX);

    public static void drawGradientRound(PoseStack matrices, float x, float y, float width, float height, float rounding, int color, int color2, int color3, int color4) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tessellator = RenderSystem.renderThreadTesselator();

        Window window = mc.getWindow();
        float guiScale = (float) window.getGuiScale();

        RECTANGLE_SHADER.uniform("position").set(x * guiScale, window.getHeight() - (y * guiScale) - (height * guiScale));

        RECTANGLE_SHADER.uniform("size").set(width * guiScale, height * guiScale);
        RECTANGLE_SHADER.uniform("rounding").set(rounding * guiScale, rounding * guiScale, rounding * guiScale, rounding * guiScale);

        RECTANGLE_SHADER.uniform("smoothness").set(0F, 2F);

        RECTANGLE_SHADER.uniform("color1").set(
                ColorUtility.getRed(color) / 255F,
                ColorUtility.getGreen(color) / 255F,
                ColorUtility.getBlue(color) / 255F,
                ColorUtility.getAlpha(color) / 255F
        );

        RECTANGLE_SHADER.uniform("color2").set(
                ColorUtility.getRed(color2) / 255F,
                ColorUtility.getGreen(color2) / 255F,
                ColorUtility.getBlue(color2) / 255F,
                ColorUtility.getAlpha(color2) / 255F
        );

        RECTANGLE_SHADER.uniform("color3").set(
                ColorUtility.getRed(color3) / 255F,
                ColorUtility.getGreen(color3) / 255F,
                ColorUtility.getBlue(color3) / 255F,
                ColorUtility.getAlpha(color3) / 255F
        );

        RECTANGLE_SHADER.uniform("color4").set(
                ColorUtility.getRed(color4) / 255F,
                ColorUtility.getGreen(color4) / 255F,
                ColorUtility.getBlue(color4) / 255F,
                ColorUtility.getAlpha(color4) / 255F
        );

        RECTANGLE_SHADER.bind();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);


        bufferBuilder.addVertex(x, y, 0);
        bufferBuilder.addVertex(x, y + height, 0);
        bufferBuilder.addVertex(x + width, y + height, 0);
        bufferBuilder.addVertex( x + width, y, 0);

        tessellator.draw(bufferBuilder);

        RECTANGLE_SHADER.unbind();

        RenderSystem.disableBlend();
    }
    static Shader RECTANGLE_SHADER2 = Shader.create("gradientround", DefaultVertexFormat.POSITION);

    public static void drawGradientRect(float x, float y, float width, float height, float rounding,float value, int color, int color2, int color3, int color4) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tessellator = RenderSystem.renderThreadTesselator();

        float i = (float) mc.getWindow().getGuiScale();

        //RECTANGLE_SHADER2.uniform("position").set(x * guiScale, window.getHeight() - (y * guiScale) - (height * guiScale));
        RECTANGLE_SHADER2.uniform("uSize").set(width * i, height * i);
        RECTANGLE_SHADER2.uniform("uLocation").set(x * i, -y * i + (float)mc.getWindow().getGuiScaledHeight() * i - height * i);
        RECTANGLE_SHADER2.uniform("Size").set(rounding * i);

        RECTANGLE_SHADER2.uniform("color1").set(
                ColorUtility.getRed(color) / 255F,
                ColorUtility.getGreen(color) / 255F,
                ColorUtility.getBlue(color) / 255F,
                ColorUtility.getAlpha(color) / 255F
        );

        RECTANGLE_SHADER2.uniform("color2").set(
                ColorUtility.getRed(color2) / 255F,
                ColorUtility.getGreen(color2) / 255F,
                ColorUtility.getBlue(color2) / 255F,
                ColorUtility.getAlpha(color2) / 255F
        );

        RECTANGLE_SHADER2.uniform("color3").set(
                ColorUtility.getRed(color3) / 255F,
                ColorUtility.getGreen(color3) / 255F,
                ColorUtility.getBlue(color3) / 255F,
                ColorUtility.getAlpha(color3) / 255F
        );

        RECTANGLE_SHADER2.uniform("color4").set(
                ColorUtility.getRed(color4) / 255F,
                ColorUtility.getGreen(color4) / 255F,
                ColorUtility.getBlue(color4) / 255F,
                ColorUtility.getAlpha(color4) / 255F
        );

        RECTANGLE_SHADER2.bind();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);


        bufferBuilder.addVertex(x, y, 0);
        bufferBuilder.addVertex(x, y + height, 0);
        bufferBuilder.addVertex(x + width, y + height, 0);
        bufferBuilder.addVertex( x + width, y, 0);

        tessellator.draw(bufferBuilder);

        RECTANGLE_SHADER2.unbind();

        RenderSystem.disableBlend();
    }
    public static void endRender2() {
        RenderSystem.disableBlend();
    }
    public static void setupRender2() {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        renderRoundedQuad(color, x, y, width + x, height + y, radius / 2, 4);
    }
    public static void drawRoundedRect(float x, float y, float width, float height, float radius1, float radius2, float radius3, float radius4, int color) {
        renderRoundedQuad(color, x, y, width + x, height + y, radius1, radius2, radius3, radius4, 4);
    }
    public static void drawRect(float x, float y, float width, float height, int color) {
        renderRoundedQuad(color, x, y, width + x, height + y, 0, 4);
    }
    public static void allocTextureRectangle(float x, float y, float width, float height) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(x, y, 0).setUv(0.0F, 0.0F);
        buffer.addVertex(x, (y + height), 0).setUv((float) 0.0F, 1.0F);
        buffer.addVertex((x + width), (y + height), 0).setUv(1.0F, 1.0F);
        buffer.addVertex((x + width), y, 0).setUv((float) 1.0F, 0.0F);
        BufferUploader.draw(buffer.build());
    }
    private static void checkGLError(String operation) {
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            System.err.println("OpenGL Error during " + operation + ": " + error);
        }
    }



    public static void renderRoundedQuad(int c, double fromX, double fromY, double toX, double toY, double radius1, double radius2, double radius3, double radius4, double samples) {
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        renderRoundedQuadInternal(ColorUtility.getRed(c) / 255f, ColorUtility.getGreen(c) / 255f, ColorUtility.getBlue(c) / 255f, ColorUtility.getAlpha(c) / 255f, fromX, fromY, toX, toY, radius1, radius2, radius3, radius4, samples);
        endRender();
    }
    public static void renderRoundedQuadInternal(float cr, float cg, float cb, float ca,
                                                 double fromX, double fromY, double toX, double toY,
                                                 double radius1, double radius2, double radius3, double radius4,
                                                 double samples) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // Массивы для хранения координат для каждого угла
        double[][] map = new double[][]{
                {toX - radius1, toY - radius1, radius1},  // Левая верхняя (radius1)
                {toX - radius2, fromY + radius2, radius2}, // Правая верхняя (radius2)
                {fromX + radius3, fromY + radius3, radius3}, // Левая нижняя (radius3)
                {fromX + radius4, toY - radius4, radius4}  // Правая нижняя (radius4)
        };

        // Рисуем для каждого угла прямоугольника
        for (int i = 0; i < 4; i++) {
            double[] current = map[i];
            double rad = current[2];
            for (double r = i * 90d; r < (360 / 4d + i * 90d); r += (90 / samples)) {
                float rad1 = (float) Math.toRadians(r);
                float sin = (float) (Math.sin(rad1) * rad);
                float cos = (float) (Math.cos(rad1) * rad);
                bufferBuilder.addVertex((float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr, cg, cb, ca);
            }
            // Добавляем последнюю вершину для замкнутого круга
            float rad1 = (float) Math.toRadians((360 / 4d + i * 90d));
            float sin = (float) (Math.sin(rad1) * rad);
            float cos = (float) (Math.cos(rad1) * rad);
            bufferBuilder.addVertex((float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr, cg, cb, ca);
        }

        // Отображаем результат
        BufferUploader.drawWithShader(bufferBuilder.build());
    }

    public static void renderRoundedQuadInternal(float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        double[][] map = new double[][]{new double[]{toX - radius, toY - radius, radius}, new double[]{toX - radius, fromY + radius, radius}, new double[]{fromX + radius, fromY + radius, radius}, new double[]{fromX + radius, toY - radius, radius}};
        for (int i = 0; i < 4; i++) {
            double[] current = map[i];
            double rad = current[2];
            for (double r = i * 90d; r < (360 / 4d + i * 90d); r += (90 / samples)) {
                float rad1 = (float) Math.toRadians(r);
                float sin = (float) (Math.sin(rad1) * rad);
                float cos = (float) (Math.cos(rad1) * rad);
                bufferBuilder.addVertex((float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr, cg, cb, ca);
            }
            float rad1 = (float) Math.toRadians((360 / 4d + i * 90d));
            float sin = (float) (Math.sin(rad1) * rad);
            float cos = (float) (Math.cos(rad1) * rad);
            bufferBuilder.addVertex( (float) current[0] + sin, (float) current[1] + cos, 0.0F).setColor(cr, cg, cb, ca);
        }
        BufferUploader.drawWithShader(bufferBuilder.build());
    }
    public static void endRender() {
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
    public static void renderRoundedQuad(int c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        renderRoundedQuadInternal(ColorUtility.getRed(c) / 255f, ColorUtility.getGreen(c) / 255f, ColorUtility.getBlue(c) / 255f, ColorUtility.getAlpha(c) / 255f, fromX, fromY, toX, toY, radius, samples);
        endRender();
    }
    public record Rectangle(float x, float y, float x1, float y1) {
        public boolean contains(double d, double d2) {
            return d >= (double)this.x && d <= (double)this.x1 && d2 >= (double)this.y && d2 <= (double)this.y1;
        }
    }
    static {
        clipStack = new Stack();
    }
}
