package omg.sertyo.utility.math;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class ScaleMath {
   private int scale;

   public void pushScale(GuiGraphics guiGraphics) {
      guiGraphics.pose().pushPose();
      if (!System.getProperty("os.name").contains("mac"))
       guiGraphics.pose().scale(2.0F, 2.0F, 2.0F);
      else
       guiGraphics.pose().scale(3, 3, 3);
   }

   public void popScale(GuiGraphics guiGraphics) {
      guiGraphics.pose().popPose();
   }

   public int calc(int value) {
      Window window = Minecraft.getInstance().getWindow();
      int scaleFactor = (int) window.getGuiScale();
      return value * scaleFactor / this.scale;
   }

   public float calc(float value) {
      Window window = Minecraft.getInstance().getWindow();
      float scaleFactor = (float) window.getGuiScale();
      return value * scaleFactor / (float) this.scale;
   }

   public Vec2i getMouse(int mouseX, int mouseY) {
      Window window = Minecraft.getInstance().getWindow();
      int scaleFactor = (int) window.getGuiScale();
      return new Vec2i(mouseX * scaleFactor / this.scale, mouseY * scaleFactor / this.scale);
   }

   public ScaleMath(int scale) {
      this.scale = scale;
   }

   public int getScale() {
      return this.scale;
   }

   public void setScale(int scale) {
      this.scale = scale;
   }
}
