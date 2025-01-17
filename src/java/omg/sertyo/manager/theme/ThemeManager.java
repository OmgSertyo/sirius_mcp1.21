package omg.sertyo.manager.theme;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ThemeManager {
   private Theme currentGuiTheme;
   private Theme currentStyleTheme;

   public ThemeManager() {
      this.currentGuiTheme = Themes.DARK.getTheme();
      this.currentStyleTheme = Themes.LEEN.getTheme();
   }


}
