package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import omg.sertyo.Babulka;
import omg.sertyo.manager.theme.Themes;
import omg.sertyo.module.setting.impl.NumberSetting;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.math.MathUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.animation.AnimationMath;
import omg.sertyo.utility.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class SliderComponent extends Component {
   public ModuleComponent moduleComponent;
   public NumberSetting setting;
   public float animation = 0.0F;
   public boolean isDragging;

   public SliderComponent(ModuleComponent moduleComponent, NumberSetting setting) {
      super(0.0F, 0.0F, 0.0F, 19.0F);
      this.moduleComponent = moduleComponent;
      this.setting = setting;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      super.render(stack, mouseX, mouseY);
      Fonts.mntsb14.drawString(stack,this.setting.getName(), this.x + 5.0F, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      int textWidth = (int) Fonts.mntsb14.getStringWidth(this.setting.getName());
      RenderUtility.drawRoundedRect(this.x + 64.0F, this.y + 6.0F, 60.0F, 2.5F, 1.0F, Color.decode("#2B2C44").getRGB());
      float sliderWidth = (float)((this.setting.get() - this.setting.getMinValue()) / (this.setting.getMaxValue() - this.setting.getMinValue()) * 60.0D);
      this.animation = AnimationMath.fast(this.animation, sliderWidth, 15.0F);
      RenderUtility.drawRoundedRect(this.x + 64.0F + this.animation - 2.0F, this.y + 5.5F, 4.0F, 4.0F, 3.0F, -1);
      if (this.isDragging) {
         if (!(GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS))
            this.isDragging = false;
         float sliderValue = (float) Mth.clamp(MathUtility.round((float)(((mouseX - this.x - 64.0F) / 60.0F) * (this.setting.getMaxValue() - this.setting.getMinValue()) + this.setting.getMinValue()), this.setting.getIncrement()), this.setting.getMinValue(), this.setting.getMaxValue());
         this.setting.set(sliderValue);
      }
      RenderUtility.drawRoundedRect(this.x + this.width - 9.0F - Fonts.mntsb14.getStringWidth(String.valueOf(this.setting.get())), this.y + 3.0F, (Fonts.mntsb14.getStringWidth(String.valueOf(this.setting.get())) + 4), (Fonts.mntsb14.getFontHeight("") + 4), 3.0F, Color.decode("#2B2C44").getRGB());
      Fonts.mntsb14.drawString(stack, String.valueOf(this.setting.get()), this.x + this.width - 7.0F - Fonts.mntsb14.getStringWidth(String.valueOf(this.setting.get())), this.y + 5.5F, -1);
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      boolean isHovered = RenderUtility.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height);
      if (isHovered && mouseButton == 0) {
         this.isDragging = true;
      }

   }

   public boolean isVisible() {
      return (Boolean)this.setting.getVisible().get();
   }
}
