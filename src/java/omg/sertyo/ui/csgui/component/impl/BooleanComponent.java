package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.module.setting.impl.BooleanSetting;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.ColorUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.animation.AnimationMath;
import omg.sertyo.utility.render.font.Fonts;

import java.awt.Color;

public class BooleanComponent extends Component {
   public ModuleComponent moduleComponent;

   public BooleanSetting setting;

   public float animation = 0.0F;

   public BooleanComponent(ModuleComponent moduleComponent, BooleanSetting setting) {
      super(0.0F, 0.0F, 0.0F, 14.0F);
      this.moduleComponent = moduleComponent;
      this.setting = setting;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      super.render(stack, mouseX, mouseY);
      this.animation = AnimationMath.fast(this.animation, this.setting.state ? -1.0F : 0.0F, 15.0F);
      Fonts.mntsb14.drawString(stack, this.setting.getName(), this.x + 5.0F, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      Color glowColor = ColorUtility.applyOpacity(Color.BLACK, 0.2F);
   //   RenderUtility.drawGradientGlow(this.x + this.width - 25.0F, this.y + 2.0F, 17.5F, 10.0F, 5, glowColor, glowColor);
      RenderUtility.drawRoundedRect(this.x + this.width - 25.0F, this.y + 2.0F, 17.5F, 10.0F, 8.0F, Color.decode("#2B2C44").getRGB());
      Color c = ColorUtility.interpolateColorC((new Color(78, 79, 98)).getRGB(), (new Color(202, 202, 208)).getRGB(), Math.abs(this.animation));
      RenderUtility.drawRoundedRect(this.x + this.width - 23.0F - this.animation * 7.0F, this.y + 4.0F, 6.0F, 6.0F, 5.0F, c.getRGB());
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      boolean isHovered = RenderUtility.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      if (isHovered && mouseButton == 0)
         this.setting.state = !this.setting.get();
   }

   public boolean isVisible() {
      return ((Boolean)this.setting.getVisible().get()).booleanValue();
   }
}
