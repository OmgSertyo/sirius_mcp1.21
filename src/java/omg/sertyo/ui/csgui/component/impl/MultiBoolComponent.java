package omg.sertyo.ui.csgui.component.impl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.module.setting.impl.MultiBooleanSetting;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;
import java.util.Comparator;

public class MultiBoolComponent extends Component {
   public ModuleComponent moduleComponent;

   public MultiBooleanSetting setting;

   private boolean extended;

   public MultiBoolComponent(ModuleComponent moduleComponent, MultiBooleanSetting setting) {
      super(0.0F, 0.0F, 0.0F, 15.0F);
      this.moduleComponent = moduleComponent;
      this.setting = setting;
   }

   public void render(PoseStack stack,int mouseX, int mouseY) {
      super.render(stack, mouseX, mouseY);
      int color = Color.decode("#1E1F30").getRGB();
      Fonts.mntsb14.drawString(stack, this.setting.getName(), this.x + 5.0F, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      float normalHeight = 0.0F;
      if (this.extended) {
         normalHeight = 11.5F;
         for (String mode : this.setting.values.stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())
            normalHeight += 10.0F;
      } else {
         normalHeight = 11.0F;
      }
      setHeight(normalHeight + 4.0F);
      int maxLength = this.setting.values.stream().mapToInt(this::getEnabledWidth).max().orElse(0);
      StringBuilder str = new StringBuilder();
      for (String mode : this.setting.values) {
         int index = this.setting.values.indexOf(mode);
         if (this.setting.get(index))
            str.append(mode).append(", ");
      }
      String result = str.toString();
      if (result.endsWith(", "))
         result = result.substring(0, result.length() - 2);
      if (result.isEmpty())
         result = "Empty";
      RenderUtility.drawRoundedRect(this.x + this.width - 24.0F - maxLength, this.y + 1.5F, maxLength + 16.5F, normalHeight, 4.0F, Color.decode("#2B2C44").getRGB());
      Fonts.mntsb14.drawString(stack, result, this.x + this.width - 21.0F - maxLength, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
  //    Fonts.icons12.drawString("t", this.x + this.width - 17.0F, this.y + 6.5F, (new Color(149, 149, 161)).getRGB());
      if (this.extended) {
         int offset = 0;
         for (String mode : Lists.reverse(this.setting.values.stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())) {
            int index = this.setting.values.indexOf(mode);
            if (this.setting.get(index))
               RenderUtility.drawRoundedRect(this.x + this.width - 22.0F - maxLength, this.y + 12.0F + offset, (Fonts.mntsb14.getStringWidth(mode) + 4), 9.0F, 4.0F, color);
            Fonts.mntsb14.drawString(stack, mode, this.x + this.width - 20.0F - maxLength, this.y + 15.0F + offset, this.setting.get(index) ? -1 : (new Color(149, 149, 161)).getRGB());
            offset += 10;
         }
      }
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      int maxLength = this.setting.values.stream().mapToInt(this::getEnabledWidth).max().orElse(0);
      if (mouseButton == 1 && RenderUtility.isHovered(mouseX, mouseY, (this.x + this.width - 23.0F - maxLength), (this.y + 1.5F), (maxLength + 15.5F), 11.0D))
         this.extended = !this.extended;
      if (this.extended) {
         int offset = 0;
         for (String mode : Lists.reverse(this.setting.values.stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())) {
            if (RenderUtility.isHovered(mouseX, mouseY, (this.x + this.width - 22.0F - maxLength), (this.y + 12.0F + offset), (Fonts.mntsb14.getStringWidth(mode) + 4), 9.0D)) {
               int index = this.setting.values.indexOf(mode);
               this.setting.selectedValues.set(index, Boolean.valueOf(!((Boolean)this.setting.selectedValues.get(index)).booleanValue()));
               return;
            }
            offset += 10;
         }
      }
   }

   private int getEnabledWidth(String mode) {
      return (int) (Fonts.mntsb14.getStringWidth(mode) + 4);
   }

   public boolean isVisible() {
      return ((Boolean)this.setting.getVisible().get()).booleanValue();
   }
}
