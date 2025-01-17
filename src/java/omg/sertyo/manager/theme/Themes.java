package omg.sertyo.manager.theme;

import java.awt.Color;
import java.util.HashMap;

public enum Themes {
   DARK(new Theme("Dark", Theme.ThemeType.GUI, new Color[]{new Color(30, 30, 30), new Color(25, 25, 25), new Color(75, 75, 75), new Color(65, 65, 65), new Color(42, 42, 42), new Color(37, 37, 37, 200)})),
   WHITE(new Theme("White", Theme.ThemeType.GUI, new Color[]{new Color(245, 245, 245), new Color(225, 225, 225), new Color(195, 195, 195), new Color(195, 195, 195), new Color(215, 215, 215), new Color(205, 205, 205, 200)})),
   SIRIUS(new Theme("Sirius", Theme.ThemeType.STYLE, new Color[]{(new Color(102, 153, 255)).brighter(), (new Color(102, 153, 255)).darker()})),
   CANDY(new Theme("Candy", Theme.ThemeType.STYLE, new Color[]{new Color(28, 167, 222), new Color(236, 133, 209)})),
   SUMMER(new Theme("Summer", Theme.ThemeType.STYLE, new Color[]{new Color(34, 193, 195), new Color(253, 187, 45)})),
   RIVER(new Theme("River", Theme.ThemeType.STYLE, new Color[]{new Color(4443810), new Color(1596061)})),
   GLORIA(new Theme("Gloria", Theme.ThemeType.STYLE, new Color[]{new Color(641745), new Color(6363340)})),
   LOLLIPOP(new Theme("Lollipop", Theme.ThemeType.STYLE, new Color[]{new Color(16234331), new Color(15476088)})),
   JESIC(new Theme("Jesic", Theme.ThemeType.STYLE, new Color[]{new Color(12189542), new Color(7049291)})),
   SHINE(new Theme("Shine", Theme.ThemeType.STYLE, new Color[]{new Color(11760581), new Color(7170255)})),
   HERON(new Theme("Heron", Theme.ThemeType.STYLE, new Color[]{new Color(15893890), new Color(6855044)})),
   DOUP(new Theme("Doup", Theme.ThemeType.STYLE, new Color[]{new Color(14412429), new Color(8225371)})),
   LEEN(new Theme("Leen", Theme.ThemeType.STYLE, new Color[]{new Color(10285765), new Color(4291953)})),
   CRIMSON(new Theme("Crimson", Theme.ThemeType.STYLE, new Color[]{new Color(215, 60, 67), new Color(140, 23, 39)})),
   SUNDAE(new Theme("Windy", Theme.ThemeType.STYLE, new Color[]{new Color(11319013), new Color(8846824)})),
   ORANGE(new Theme("Orange", Theme.ThemeType.STYLE, new Color[]{new Color(242, 201, 76), new Color(241, 143, 56)})),
   ATLAS(new Theme("Atlas", Theme.ThemeType.STYLE, new Color[]{new Color(16690270), new Color(4964552)})),
   SUBLIME(new Theme("Sublime", Theme.ThemeType.STYLE, new Color[]{new Color(16538749), new Color(6980347)})),
   AZURE(new Theme("Azure", Theme.ThemeType.STYLE, new Color[]{new Color(239, 50, 217), new Color(137, 255, 253)})),
   MAGIC(new Theme("Magic", Theme.ThemeType.STYLE, new Color[]{new Color(5882227), new Color(6104769)})),
   ORCA(new Theme("Orca", Theme.ThemeType.STYLE, new Color[]{new Color(4497549), new Color(1006685)})),
   EMERALD(new Theme("Emerald", Theme.ThemeType.STYLE, new Color[]{new Color(3731325), new Color(1153422)})),
   WITCHERY(new Theme("Witchery", Theme.ThemeType.STYLE, new Color[]{new Color(12784690), new Color(6430354)})),
   FLARE(new Theme("Flare", Theme.ThemeType.STYLE, new Color[]{new Color(15804177), new Color(16101145)})),
   FALLING(new Theme("Falling", Theme.ThemeType.STYLE, new Color[]{new Color(12993134), new Color(6564723)})),
   MOONLIGHT(new Theme("Moonlight", Theme.ThemeType.STYLE, new Color[]{new Color(10986455), new Color(5855641)}));

   private final Theme theme;
   private static final HashMap<String, Theme> map = new HashMap();

   private Themes(Theme theme) {
      this.theme = theme;
   }

   public static Theme findByName(String name) {
      return (Theme)map.get(name);
   }

   public Theme getTheme() {
      return this.theme;
   }

   static {
      Themes[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         Themes v = var0[var2];
         map.put(v.theme.getName(), v.theme);
      }

   }
}
