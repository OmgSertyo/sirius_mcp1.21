package omg.sertyo.module.impl.render;


import omg.sertyo.module.Category;
import omg.sertyo.module.Module;
import omg.sertyo.module.ModuleAnnotation;
import omg.sertyo.module.setting.impl.ColorSetting;
import omg.sertyo.module.setting.impl.ModeSetting;

import java.awt.Color;

@ModuleAnnotation(
   name = "Glint",
   category = Category.RENDER
)
public class Glint extends Module {
   public static ModeSetting mode = new ModeSetting("Color Mode", "Theme", new String[]{"Theme", "Custom"});
   public static ColorSetting color = new ColorSetting("Color", (new Color(68, 205, 205)).getRGB(), () -> {
      return mode.get().equals("Custom");
   });

   public static Color getColor() {
      Color customColor = Color.WHITE;
      String var1 = mode.get();
      byte var2 = -1;
      switch(var1.hashCode()) {
      case 80774569:
         if (var1.equals("Theme")) {
            var2 = 0;
         }
         break;
      case 2029746065:
         if (var1.equals("Custom")) {
            var2 = 1;
         }
      }

      switch(var2) {
      case 0:
         customColor = null;
         break;
      case 1:
         customColor = color.getColor();
      }

      System.out.println(customColor);return customColor;
   }
}
