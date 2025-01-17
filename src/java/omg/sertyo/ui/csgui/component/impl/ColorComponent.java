package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.module.setting.impl.ColorSetting;
import omg.sertyo.ui.csgui.CsGui;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.ui.csgui.window.ColorPickerWindow;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;

public class ColorComponent extends Component {
   public ModuleComponent moduleComponent;

   public ColorSetting setting;

   public ColorComponent(ModuleComponent moduleComponent, ColorSetting setting) {
      super(0.0F, 0.0F, 0.0F, 14.0F);
      this.moduleComponent = moduleComponent;
      this.setting = setting;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      super.render(stack, mouseX, mouseY);
      int elementsColor = Color.decode("#1E1F30").getRGB();
      Fonts.mntsb14.drawString(stack, this.setting.getName(), this.x + 5.0F, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      RenderUtility.drawRoundedRect(this.x + this.width - 16.0F, this.y + 1.5F, 11.0F, 11.0F, 10.0F, Color.decode("#2B2C44").brighter().getRGB());
      RenderUtility.drawRoundedRect(this.x + this.width - 15.0F, this.y + 2.5F, 9.0F, 9.0F, 8.0F, elementsColor);
      RenderUtility.drawRoundedRect(this.x + this.width - 14.0F, this.y + 3.5F, 7.0F, 7.0F, 6.0F, this.setting.get());
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (RenderUtility.isHovered(mouseX, mouseY, (this.x + this.width - 16.0F), (this.y + 1.5F), 11.0D, 11.0D) && (CsGui.colorPicker == null || !CsGui.colorPicker.getColorSetting().equals(this.setting)))
         CsGui.colorPicker = new ColorPickerWindow((float)(mouseX + 5.0D), (float)(mouseY + 5.0D), 80.0F, 80.0F, this.setting);
   }

   public boolean isVisible() {
      return ((Boolean)this.setting.getVisible().get()).booleanValue();
   }
}
