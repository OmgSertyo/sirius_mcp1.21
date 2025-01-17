package omg.sertyo.ui.csgui.window;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import omg.sertyo.module.setting.impl.ColorSetting;
import omg.sertyo.module.setting.impl.NumberSetting;
import omg.sertyo.ui.csgui.CsGui;
import omg.sertyo.utility.render.ColorUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.animation.Animation;
import omg.sertyo.utility.render.animation.Direction;
import omg.sertyo.utility.render.animation.impl.EaseInOutQuad;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ColorPickerWindow extends Window {
   private final ColorSetting colorSetting;

   public ColorSetting getColorSetting() {
      return this.colorSetting;
   }

   private Animation animation = (Animation)new EaseInOutQuad(250, 1.0F, Direction.FORWARDS);

   private final NumberSetting hueSlider = new NumberSetting("Hue", 1.0F, 1.0F, 360.0F, 1.0F);

   private boolean isHueDragging;

   private int hueValue;

   public ColorPickerWindow(float x, float y, float width, float height, ColorSetting colorSetting) {
      super(x, y, width, height);
      this.colorSetting = colorSetting;
      float[] color = ColorUtility.getRGBAf(colorSetting.get());
      float[] hueArray = Color.RGBtoHSB((int)(color[0] * 255.0F), (int)(color[1] * 255.0F), (int)(color[2] * 255.0F), color);
      this.hueValue = (int)(hueArray[0] * 360.0F);
   }

   public void init() {
      this.animation = (Animation)new EaseInOutQuad(250, 1.0F, Direction.FORWARDS);
   }

   public void render(int mouseX, int mouseY) {
      if (this.animation.finished(Direction.BACKWARDS)) {
         CsGui.colorPicker = null;
         return;
      }
      int bgColor = Color.decode("#151521").getRGB();
      int elementsColor = Color.decode("#1E1F30").getRGB();
      RenderUtility.scaleStart(new PoseStack(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, this.animation.getOutput());
      RenderUtility.drawRoundedRect(this.x - 1.0F, this.y - 1.0F, this.width + 2.0F, this.height + 2.0F, 6.0F, Color.decode("#2B2C44").brighter().getRGB());
      RenderUtility.drawRoundedRect(this.x, this.y, this.width, this.height, 5.0F, elementsColor);
      int hueColor = ColorUtility.HUEtoRGB(this.hueValue);
      RenderUtility.drawGradientRect(this.x + 5.0F, this.y + 5.0F, this.width - 10.0F, this.height - 20.0F, 0.0F,0, Color.WHITE.getRGB(), Color.BLACK.getRGB(), hueColor, Color.BLACK.getRGB());
      int pixelColor = 0;
      if (RenderUtility.isHovered(mouseX, mouseY, this.x + 5.0F, this.y + 5.0F, this.width - 10.0F, this.height - 20.0F) &&
              !this.isHueDragging &&
              GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {

            float saturation = (mouseX - this.x + 5.0F) / (this.width - 10.0F);
         float brightness = 1.0F - (mouseY - this.y + 5.0F) / (this.height - 20.0F);
         pixelColor = Color.HSBtoRGB(this.hueValue / 360.0F, saturation, brightness);
      }
      if (pixelColor != 0)
         this.colorSetting.setColor(pixelColor);
      if (this.isHueDragging)
         this.hueValue = getSliderValue(this.hueSlider, this.x + 5.0F, mouseX);
      float inc = 0.2F;
      float times = 1.0F / inc;
      float xHuePos = this.x + 5.0F;
      float size = (this.width - 10.0F) / times;
      for (int i = 0; i < times; i++) {
         boolean last = (i == times - 1.0F);
         if (last)
            size--;
         RenderUtility.drawGradientRect(xHuePos, this.y + this.height - 10.0F, size, 5.0F, 0,0,
                 Color.HSBtoRGB(inc * i, 1.0F, 1.0F),
                 Color.HSBtoRGB(inc * (i + 1), 1.0F, 1.0F),
                 Color.HSBtoRGB(inc * (i + 1), 1.0F, 1.0F),
                 Color.HSBtoRGB(inc * i, 1.0F, 1.0F));
         if (!last)
            xHuePos += size;
      }
      RenderUtility.drawRoundedRect(this.x + 4.0F + getPos(this.hueSlider, this.hueValue), this.y + this.height - 11.0F, 2.0F, 7.0F, 1.0F, -1);
      RenderUtility.endScissor(new PoseStack());
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (!RenderUtility.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height) && this.animation.isDone()) {
         this.animation.setDirection(Direction.BACKWARDS);
         this.animation.setDuration(225);
      }
      if (RenderUtility.isHovered(mouseX, mouseY, (this.x + 5.0F), (this.y + this.height - 10.0F), (this.width - 10.0F), 5.0D) && mouseButton == 0)
         this.isHueDragging = true;
   }

   public void mouseReleased(double mouseX, double mouseY, int state) {
      this.isHueDragging = false;
   }

   public int getSliderValue(NumberSetting numberSetting, float posX, int mouseX) {
      int delta = (int)(numberSetting.getMaxValue() - numberSetting.getMinValue());
      float clickedX = mouseX - posX;
      float value = clickedX / (this.width - 10.0F);
      float outValue = (float)(numberSetting.getMinValue() + (delta * value));
      return (int)Mth.clamp((int) Mth.round(outValue, numberSetting.getIncrement()), (float)numberSetting.getMinValue(), (float)numberSetting.getMaxValue());
   }

   public int getPos(NumberSetting numberSetting, int value) {
      int delta = (int)(numberSetting.getMaxValue() - numberSetting.getMinValue());
      return (int)((this.width - 10.0F) * (value - numberSetting.getMinValue()) / delta);
   }
}
