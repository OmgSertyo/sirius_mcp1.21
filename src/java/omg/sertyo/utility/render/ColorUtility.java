package omg.sertyo.utility.render;

import java.awt.Color;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import omg.sertyo.Babulka;
import omg.sertyo.utility.Utility;
import org.lwjgl.opengl.GL11;

public class ColorUtility implements Utility {
   public static float[] getRGBAf(int color) {
      return new float[]{(float)(color >> 16 & 255) / 255.0F, (float)(color >> 8 & 255) / 255.0F, (float)(color & 255) / 255.0F, (float)(color >> 24 & 255) / 255.0F};
   }

   public static void setColor(Color color) {
      if (color == null) {
         color = Color.WHITE;
      }

      setColor((double)((float)color.getRed() / 255.0F), (double)((float)color.getGreen() / 255.0F), (double)((float)color.getBlue() / 255.0F), (double)((float)color.getAlpha() / 255.0F));
   }

   public static int setAlpha(int color, int alpha) {
      Color c = new Color(color);
      return (new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha)).getRGB();
   }

   public static int setAlpha(int color, float alpha) {
      Color c = new Color(color);
      return (new Color((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F, alpha)).getRGB();
   }

   public static void setColor(double red, double green, double blue, double alpha) {
      GL11.glColor4d(red, green, blue, alpha);
   }

   public static void setColor(int color) {
      setColor(color, (float)(color >> 24 & 255) / 255.0F);
   }

   public static void setColor(int color, float alpha) {
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      RenderSystem.setShaderColor(r, g, b, alpha);
   }

   public static int getRed(int hex) {
      return hex >> 16 & 255;
   }

   public static int getGreen(int hex) {
      return hex >> 8 & 255;
   }

   public static int getBlue(int hex) {
      return hex & 255;
   }

   public static int getAlpha(int hex) {
      return hex >> 24 & 255;
   }

   public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static Color applyOpacity(Color color, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)((float)color.getAlpha() * opacity));
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return interpolate((double)oldValue, (double)newValue, (double)((float)interpolationValue)).intValue();
   }

   public static Color interpolateColorC(int color1, int color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      return new Color(interpolateInt(getRed(color1), getRed(color2), (double)amount), interpolateInt(getGreen(color1), getGreen(color2), (double)amount), interpolateInt(getBlue(color1), getBlue(color2), (double)amount), interpolateInt(getAlpha(color1), getAlpha(color2), (double)amount));
   }

   public static Color gradient(int speed, int index, Color... colors) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      int colorIndex = (int)((float)angle / 360.0F * (float)colors.length);
      if (colorIndex == colors.length) {
         --colorIndex;
      }

      Color color1 = colors[colorIndex];
      Color color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
      return interpolateColorC(color1.getRGB(), color2.getRGB(), (float)angle / 360.0F * (float)colors.length - (float)colorIndex);
   }

   public static Color fade(int speed, int index, Color color, float alpha) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], (float)angle / 360.0F));
      return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int)(alpha * 255.0F))));
   }

   public static int HUEtoRGB(int value) {
      float hue = (float)value / 360.0F;
      return Color.HSBtoRGB(hue, 1.0F, 1.0F);
   }

   public static Color getColor(int index) {
      Color color = Babulka.getInstance().getThemeManager().getCurrentStyleTheme().getColors()[0];
      Color color2 = Babulka.getInstance().getThemeManager().getCurrentStyleTheme().getColors()[1];
      return gradient((int)(10.0F - 5), index, color, color2);
   }

}
