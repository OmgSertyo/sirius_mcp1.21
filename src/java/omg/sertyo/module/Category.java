package omg.sertyo.module;


import omg.sertyo.utility.render.animation.Animation;
import omg.sertyo.utility.render.animation.impl.DecelerateAnimation;

public enum Category {
   COMBAT("Combat", "a"),
   MOVEMENT("Movement", "b"),
   RENDER("Render", "c"),
   PLAYER("Player", "d"),
   UTIL("Util", "e"),
   CONFIGS("Configs", "f", true),
   SCRIPTS("Scripts", "g", true),
   THEMES("Themes", "h", true);

   private final String name;
   private final String icon;
   private boolean bottom = false;
   private final Animation animation = new DecelerateAnimation(340, 1.0F);

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   private Category(String name, String icon, boolean bottom) {
      this.name = name;
      this.icon = icon;
      this.bottom = bottom;
   }

   public String getName() {
      return this.name;
   }

   public String getIcon() {
      return this.icon;
   }

   public boolean isBottom() {
      return this.bottom;
   }

   public Animation getAnimation() {
      return this.animation;
   }
}
