package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.Babulka;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.ColorUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class ConfigComponent extends Component {
   private final String name;

   public String getName() {
      return this.name;
   }

   private final List<String> buttons = Arrays.asList(new String[] { "Load", "Save", "Delete" });

   public ConfigComponent(String name, float width, float height) {
      super(0.0F, 0.0F, width, height);
      this.name = name;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      int elementsColor = Color.decode("#1E1F30").getRGB();
      Color glowColor = ColorUtility.applyOpacity(Color.BLACK, 0.2F);
     // RenderUtility.drawGradientGlow(this.x, this.y, this.width, this.height, 5, glowColor, glowColor);
      RenderUtility.drawRoundedRect(this.x, this.y, this.width, this.height, 10.0F, elementsColor);
      Fonts.mntsb14.drawString(stack, this.name, this.x + 5.0F, this.y + 9.0F, -1);
      float xOffset = 2.0F;
      float spacing = 3.0F;
      for (String mode : this.buttons) {
         float enabledWidth = getEnabledWidth(mode);
         float enabledHeight = 11.0F;
     //    RenderUtility.drawGradientGlow(this.x + 70.0F + xOffset, this.y + 6.0F, enabledWidth, enabledHeight, 5, glowColor, glowColor);
         RenderUtility.drawRoundedRect(this.x + 70.0F + xOffset, this.y + 6.0F, enabledWidth, enabledHeight, 3.0F, Color.decode("#2B2C44").getRGB());
         Fonts.mntsb14.drawString(stack, mode, this.x + 72.0F + xOffset, this.y + 10.0F, (new Color(149, 149, 161)).getRGB());
         xOffset += enabledWidth + spacing;
      }
   }

   public boolean mouseBoolClicked(double mouseX, double mouseY, int mouseButton) {
      float xOffset = 2.0F;
      float spacing = 3.0F;
      for (String mode : this.buttons) {
         float enabledWidth = getEnabledWidth(mode);
         float enabledHeight = 11.0F;
         if (RenderUtility.isHovered(mouseX, mouseY, (this.x + 70.0F + xOffset), (this.y + 6.0F), enabledWidth, enabledHeight))
            switch (mode) {
               case "Load":
                  Babulka.getInstance().getConfigManager().loadConfig(this.name);
                  break;
               case "Save":
                  Babulka.getInstance().getConfigManager().saveConfig(this.name);
                  break;
               case "Delete":
                  Babulka.getInstance().getConfigManager().deleteConfig(this.name);
                  return true;
            }
         xOffset += enabledWidth + spacing;
      }
      return false;
   }

   private float getEnabledWidth(String mode) {
      return (Fonts.mntsb14.getStringWidth(mode) + 4);
   }
}
