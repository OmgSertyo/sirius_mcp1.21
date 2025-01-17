package omg.sertyo.ui.csgui.component.impl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.module.setting.impl.ModeSetting;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;
import java.util.Comparator;

public class ModeComponent extends Component {
   public ModuleComponent moduleComponent;

   public ModeSetting setting;

   private boolean extended;

   public ModeComponent(ModuleComponent moduleComponent, ModeSetting setting) {
      super(0.0F, 0.0F, 0.0F, 15.0F);
      this.moduleComponent = moduleComponent;
      this.setting = setting;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      super.render(stack,mouseX, mouseY);
      int color = Color.decode("#1E1F30").getRGB();
      Fonts.mntsb14.drawString(stack, this.setting.getName(), this.x + 5.0F, this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      float normalHeight = 0.0F;
      if (this.extended) {
         normalHeight = 12.5F;
         for (String mode : this.setting.getModes().stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())
            normalHeight += 9.0F;
      } else {
         normalHeight = 11.0F;
      }
      setHeight(normalHeight + 4.0F);
      int maxLength = this.setting.getModes().stream().mapToInt(this::getEnabledWidth).max().orElse(0);
      RenderUtility.drawRoundedRect(this.x + this.width - 20.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)), this.y + 1.5F, (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)) + 12.5F, normalHeight, 4.0F, Color.decode("#2B2C44").getRGB());
      Fonts.mntsb14.drawString(stack,this.setting.get(), this.x + this.width - 17.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)), this.y + 5.5F, (new Color(149, 149, 161)).getRGB());
      //Fonts.icons12.drawString("t", this.x + this.width - 17.0F, this.y + 6.5F, (new Color(149, 149, 161)).getRGB());
      if (this.extended) {
         int offset = 0;
         for (String mode : Lists.reverse(this.setting.getModes().stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())) {
            if (this.setting.is(mode))
               RenderUtility.drawRoundedRect(this.x + this.width - 18.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)), this.y + 12.0F + offset, (Fonts.mntsb14.getStringWidth(mode) + 4), 9.0F, 4.0F, color);
            Fonts.mntsb14.drawString(stack, mode, this.x + this.width - 16.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)), this.y + 15.0F + offset, this.setting.is(mode) ? -1 : (new Color(149, 149, 161)).getRGB());
            offset += 9;
         }
      }
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      int maxLength = this.setting.getModes().stream().mapToInt(this::getEnabledWidth).max().orElse(0);
      if (mouseButton == 1)
         if (RenderUtility.isHovered(mouseX, mouseY, (this.x + this.width - 19.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4))), (this.y + 1.5F), ((this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4)) + 11.5F), 11.0D))
            this.extended = !this.extended;
      if (this.extended) {
         int offset = 0;
         for (String mode : Lists.reverse(this.setting.getModes().stream().sorted(Comparator.comparingDouble(this::getEnabledWidth)).toList())) {
            if (RenderUtility.isHovered(mouseX, mouseY, (this.x + this.width - 18.0F - (this.extended ? maxLength : (Fonts.mntsb14.getStringWidth(this.setting.get()) + 4))), (this.y + 12.0F + offset), (Fonts.mntsb14.getStringWidth(mode) + 4), 9.0D)) {
               this.setting.set(mode);
               return;
            }
            offset += 9;
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
