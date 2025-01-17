package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.Babulka;
import omg.sertyo.manager.theme.Theme;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.RenderUtility;

public class ThemeComponent extends Component {
   private final Theme theme;

   public Theme getTheme() {
      return this.theme;
   }

   public ThemeComponent(Theme theme, float width, float height) {
      super(0.0F, 0.0F, width, height);
      this.theme = theme;
   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      if (Babulka.getInstance().getThemeManager().getCurrentStyleTheme().equals(this.theme))
         RenderUtility.drawRoundedRect(this.x, this.y, this.width, this.height, 8.0F, -1);
      RenderUtility.drawGradientRound(stack,this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, this.height - 2.0F, 3.5f, this.theme.getColors()[0].getRGB(), this.theme.getColors()[0].getRGB(), this.theme.getColors()[1].getRGB(), this.theme.getColors()[1].getRGB());
   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      if (RenderUtility.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height))
         Babulka.getInstance().getThemeManager().setCurrentStyleTheme(this.theme);
   }
}
