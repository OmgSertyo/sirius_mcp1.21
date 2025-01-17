//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package omg.sertyo.ui.csgui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import omg.sertyo.Babulka;
import omg.sertyo.manager.config.Config;
import omg.sertyo.manager.config.ConfigManager;
import omg.sertyo.manager.theme.Themes;
import omg.sertyo.manager.theme.Theme.ThemeType;
import omg.sertyo.module.Category;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.ui.csgui.component.impl.ConfigComponent;
import omg.sertyo.ui.csgui.component.impl.ModuleComponent;
import omg.sertyo.ui.csgui.component.impl.ThemeComponent;
import omg.sertyo.ui.csgui.window.ColorPickerWindow;
import omg.sertyo.utility.render.ColorUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.animation.Animation;
import omg.sertyo.utility.render.animation.AnimationMath;
import omg.sertyo.utility.render.animation.Direction;
import omg.sertyo.utility.render.animation.impl.DecelerateAnimation;
import omg.sertyo.utility.render.animation.impl.EaseInOutQuad;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallback;


public class CsGui extends Screen {
   public int x;
   public int y;
   public static Category selected;
   public static ColorPickerWindow colorPicker;
   public static List<ModuleComponent> modules;
   public static List<ModuleComponent> modules2;
   public static List<ThemeComponent> themes;
   public static List<ConfigComponent> configs;
   public static List<ConfigComponent> configs2;
   private static Animation openAnimation;
   private static final Animation moduleAnimation;
   public static boolean escapeInUse;
   private final float maxParticleAmount = 32.0F;
   private final float particleScale = 32.0F;
   public boolean isClosed;
   float windowWidth = 350.0F;
   float windowHeight = 250.0F;
   public float scrollY = 32.5F;
   private Minecraft mc = Minecraft.getInstance();
   public CsGui() {
       super(net.minecraft.network.chat.Component.literal("sosite"));
       Window sr = mc.getWindow();
      this.x = Babulka.getInstance().getScaleMath().calc(sr.getGuiScaledWidth()) / 2 - this.width / 2 + 90;
      this.y = Babulka.getInstance().getScaleMath().calc(sr.getGuiScaledHeight()) / 2 - this.height / 2;
      modules.clear();
      modules2.clear();
      themes.clear();
      Babulka.getInstance().getModuleManager().getModules().forEach((module) -> {
         if (Babulka.getInstance().getModuleManager().getModules().indexOf(module) % 2 == 0) {
            modules.add(new ModuleComponent(module, 155.0F, 23.0F));
         }

         if (Babulka.getInstance().getModuleManager().getModules().indexOf(module) % 2 != 0) {
            modules2.add(new ModuleComponent(module, 155.0F, 23.0F));
         }

      });
      Themes[] var2 = Themes.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Themes theme = var2[var4];
         if (theme.getTheme().getType().equals(ThemeType.STYLE)) {
            themes.add(new ThemeComponent(theme.getTheme(), 320.0F, 20.0F));
         }
      }

   }

   public void init() {
      super.init();

      openAnimation = new EaseInOutQuad(250, 1.0F, Direction.FORWARDS);
      if (colorPicker != null) {
         colorPicker.init();
      }

      updateConfigComponents();
   }
   

   public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
      super.render(stack, mouseX, mouseY, partialTicks);

      Window sr = mc.getWindow();
      this.x = Babulka.getInstance().getScaleMath().calc(sr.getGuiScaledWidth()) / 2 - (int)this.windowWidth / 2;
      this.y = Babulka.getInstance().getScaleMath().calc(sr.getGuiScaledHeight()) / 2 - (int)this.windowHeight / 2;

      if (this.isClosed && openAnimation.isDone()) {
         this.mc.setScreen(null);
         this.isClosed = false;
      }

      float scale = openAnimation.getOutput();
      PoseStack p = stack.pose();
      p.pushPose();
      p.translate(this.x + this.windowWidth / 2.0F, (this.y + this.windowHeight) / 2.0F, 0.0F);
      p.scale(scale, scale, 0.0F);
      p.translate(-(this.x + this.windowWidth / 2.0F), -((this.y + this.windowHeight) / 2.0F), 0.0F);
      RenderUtility.drawRect(0.0F, 0.0F, (float)this.mc.getWindow().getWidth(), (float)this.mc.getWindow().getHeight(), ColorUtility.setAlpha(Color.BLACK.getRGB(), (int)(150.0F * scale)));
      int bgColor = Color.decode("#151521").getRGB();
      int elementsColor = Color.decode("#1E1F30").getRGB();
     // RenderUtility.drawGlow((float)(this.x - 1), (float)(this.y - 1), this.windowWidth + 2.0F, this.windowHeight + 2.0F, 10, new Color(bgColor));
      RenderUtility.drawRoundedRect(this.x, this.y, this.windowWidth, this.windowHeight, 10.0F, bgColor);   //   RenderUtility.drawGlow((float)(this.x + 15), (float)(this.y + 5), 110.0F, 20.0F, 5, ColorUtility.applyOpacity(Color.BLACK, 0.1F));


   //   RenderUtility.drawGlow((float)(this.x + 130), (float)(this.y + 5), 60.0F, 20.0F, 5, ColorUtility.applyOpacity(Color.BLACK, 0.1F));
      RenderUtility.drawRoundedRect((float)(this.x + 130), (float)(this.y + 5), 60.0F, 20.0F, 5.0F, elementsColor);
     // Fonts.icons21.drawString("g", (float)(this.x + 135), (float)this.y + 12.5F, (new Color(160, 160, 160)).getRGB());
      Fonts.mntsb16.drawString(stack.pose(), "Configs", (float)(this.x + 150), (float)this.y + 12.5F, (new Color(160, 160, 160)).getRGB());
   //   RenderUtility.drawGlow((float)(this.x + 195), (float)(this.y + 5), 63.0F, 20.0F, 5, ColorUtility.applyOpacity(Color.BLACK, 0.1F));
      RenderUtility.drawRoundedRect((float)(this.x + 195), (float)(this.y + 5), 63.0F, 20.0F, 5.0F, elementsColor);
      //Fonts.icons21.drawString("h", (float)(this.x + 200), (float)this.y + 12.5F, (new Color(160, 160, 160)).getRGB());
      Fonts.mntsb16.drawString(stack.pose(), "Themes", (float)(this.x + 217), (float)this.y + 12.5F, (new Color(160, 160, 160)).getRGB());
   //   RenderUtility.drawGlow((float)(this.x + 280), (float)(this.y + 5), 20.0F, 20.0F, 5, ColorUtility.applyOpacity(Color.BLACK, 0.1F));
      RenderUtility.drawRoundedRect((float)(this.x + 280), (float)(this.y + 5), 20.0F, 20.0F, 5.0F, elementsColor);

      /*StencilUtility.initStencilToWrite();
      RenderUtility.drawRect((float)(this.x + 15), (float)(this.y + 29), 320.0F, this.windowHeight - 29.0F, -1);
      StencilUtility.readStencilBuffer(1);*/
      if (selected.equals(Category.THEMES)) {
         this.drawThemes(mouseX, mouseY);
      } else if (selected.equals(Category.CONFIGS)) {
         this.drawConfigs(mouseX, mouseY);
      } else {
         this.drawComponents(mouseX, mouseY);
      }

      Color trueBgColor = new Color(bgColor);
      RenderUtility.drawRect((float)(this.x + 15), (float)(this.y + 29), 320.0F, this.windowHeight - 29.0F, (new Color((float)trueBgColor.getRed() / 255.0F, (float)trueBgColor.getGreen() / 255.0F, (float)trueBgColor.getBlue() / 255.0F, moduleAnimation.getOutput())).getRGB());
   //   StencilUtility.uninitStencilBuffer();
      int categoryWidth = 145;
      float catX = (float)this.x + this.windowWidth / 2.0F - (float)categoryWidth / 2.0F;
      float catY = (float)this.y + this.windowHeight - 30.0F;
      Color glowColor = Color.decode("#3A3B65").darker();
  //    RenderUtility.drawGradientGlow(catX, catY, (float)categoryWidth, 25.0F, 10, glowColor, glowColor);
      RenderUtility.drawRoundedRect(catX, catY, (float)categoryWidth, 25.0F, 15.0F, Color.decode("#3A3B65").darker().getRGB());
      int catIndex = 0;
      Category[] var14 = Category.values();
      int var15 = var14.length;

      for(int var16 = 0; var16 < var15; ++var16) {
         Category category = var14[var16];
         if (!category.equals(Category.CONFIGS) && !category.equals(Category.THEMES) && !category.equals(Category.SCRIPTS)) {
            Fonts.mntsb22.drawString(stack.pose(), category.getIcon(), catX + 15.0F + (float)catIndex, catY + 9.0F, selected.equals(category) ? Color.decode("#3A3B65").brighter().getRGB() : (new Color(160, 160, 160)).getRGB());
            catIndex += category.equals(Category.MOVEMENT) ? 30 : (category.equals(Category.PLAYER) ? 22 : 25);
         }
      }

      if (colorPicker != null) {
         colorPicker.render(mouseX, mouseY);
      }
      p.popPose();
   }

   private void drawThemes(int mouseX, int mouseY) {
            GLFW.glfwSetScrollCallback(GLFW.glfwGetCurrentContext(), new GLFWScrollCallback() {
         @Override
         public void invoke(long window, double xoffset, double yoffset) {
            scrollY += (float) yoffset * 5.0F;
         }
      });    
      float size = 0.0F;
      float offset = this.scrollY;

      ThemeComponent themeComponent;
      for(Iterator var5 = themes.iterator(); var5.hasNext(); offset += themeComponent.height + 4.0F) {
         themeComponent = (ThemeComponent)var5.next();
         themeComponent.setX((float)(this.x + 15));
         themeComponent.setY(AnimationMath.fast(themeComponent.y, (float)this.y + offset, 15.0F));
         themeComponent.render(new PoseStack(),mouseX, mouseY);
         size += themeComponent.height + 4.0F;
      }

      this.scrollY = size > 250.0F ? Mth.clamp(this.scrollY, -size + 223.0F, 32.5F) : 32.5F;
   }

   private void drawConfigs(int mouseX, int mouseY) {
            GLFW.glfwSetScrollCallback(GLFW.glfwGetCurrentContext(), new GLFWScrollCallback() {
         @Override
         public void invoke(long window, double xoffset, double yoffset) {
            scrollY += (float) yoffset * 5.0F;
         }
      });    
      float offset = this.scrollY;
      float offset1 = 32.5F;
      float offset2 = 32.5F;

      Iterator var6;
      ConfigComponent configComponent;
      for(var6 = configs.iterator(); var6.hasNext(); offset1 += configComponent.height + 5.0F) {
         configComponent = (ConfigComponent)var6.next();
         configComponent.setX((float)(this.x + 15));
         configComponent.setY(AnimationMath.fast(configComponent.y, (float)this.y + offset, 15.0F));
         configComponent.render(new PoseStack(),mouseX, mouseY);
         offset += configComponent.height + 5.0F;
      }

      offset = this.scrollY;

      for(var6 = configs2.iterator(); var6.hasNext(); offset2 += configComponent.height + 5.0F) {
         configComponent = (ConfigComponent)var6.next();
         configComponent.setX((float)(this.x + 180));
         configComponent.setY(AnimationMath.fast(configComponent.y, (float)this.y + offset, 15.0F));
         configComponent.render(new PoseStack(),mouseX, mouseY);
         offset += configComponent.height + 5.0F;
      }

      float scrollMax = Math.max(offset1, offset2);
      this.scrollY = scrollMax > 250.0F ? Mth.clamp(this.scrollY, -scrollMax + 253.0F, 32.5F) : 32.5F;
   }

   private void drawComponents(int mouseX, int mouseY) {
      List<ModuleComponent> categoryModules1 = (List)modules.stream().filter((module) -> {
         return module.getModule().getCategory().equals(selected);
      }).collect(Collectors.toList());
      List<ModuleComponent> categoryModules2 = (List)modules2.stream().filter((module) -> {
         return module.getModule().getCategory().equals(selected);
      }).collect(Collectors.toList());
      GLFW.glfwSetScrollCallback(GLFW.glfwGetCurrentContext(), new GLFWScrollCallback() {
         @Override
         public void invoke(long window, double xoffset, double yoffset) {
            scrollY += (float) yoffset * 5.0F;
         }
      });     
      float offset = this.scrollY;
      float offset1 = 32.5F;
      float offset2 = 32.5F;
      Iterator var8 = categoryModules1.iterator();

      while(true) {
         ModuleComponent moduleElement;
         Iterator var10;
         Component element;
         do {
            if (!var8.hasNext()) {
               offset = this.scrollY;
               var8 = categoryModules2.iterator();

               while(true) {
                  do {
                     if (!var8.hasNext()) {
                        float scrollMax = Math.max(offset1, offset2);
                        this.scrollY = scrollMax > 250.0F ? Mth.clamp(this.scrollY, -scrollMax + 253.0F, 32.5F) : 32.5F;
                        return;
                     }

                     moduleElement = (ModuleComponent)var8.next();
                     moduleElement.setX((float)(this.x + 180));
                     moduleElement.setY(AnimationMath.fast(moduleElement.y, (float)this.y + offset, 15.0F));
                  } while(!moduleElement.getModule().isSearched());

                  var10 = moduleElement.elements.iterator();

                  while(var10.hasNext()) {
                     element = (Component)var10.next();
                     if (element.isVisible()) {
                        offset += element.height;
                        offset2 += element.height;
                     }
                  }

                  moduleElement.render(new PoseStack(),mouseX, mouseY);
                  offset += moduleElement.height + 5.0F;
                  offset2 += moduleElement.height + 5.0F;
               }
            }

            moduleElement = (ModuleComponent)var8.next();
            moduleElement.setX((float)(this.x + 15));
            moduleElement.setY(AnimationMath.fast(moduleElement.y, (float)this.y + offset, 15.0F));
         } while(!moduleElement.getModule().isSearched());

         var10 = moduleElement.elements.iterator();

         while(var10.hasNext()) {
            element = (Component)var10.next();
            if (element.isVisible()) {
               offset += element.height;
               offset1 += element.height;
            }
         }

         moduleElement.render(new PoseStack(),mouseX, mouseY);
         offset += moduleElement.height + 5.0F;
         offset1 += moduleElement.height + 5.0F;
      }
   }

   public static void updateConfigComponents() {
      configs.clear();
      configs2.clear();
      ArrayList<Config> allConfigs = ConfigManager.getLoadedConfigs();
      Iterator var1 = allConfigs.iterator();

      while(var1.hasNext()) {
         Config config = (Config)var1.next();
         if (allConfigs.indexOf(config) % 2 == 0) {
            configs.add(new ConfigComponent(config.getName(), 155.0F, 23.0F));
         }

         if (allConfigs.indexOf(config) % 2 != 0) {
            configs2.add(new ConfigComponent(config.getName(), 155.0F, 23.0F));
         }
      }

   }
   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      int categoryWidth = 145;
      float catX = (float)this.x + 175.0F - (float)categoryWidth / 2.0F;
      float catY = (float)(this.y + 250 - 30);
      if (mouseButton == 0) {
         int catIndex = 0;
         Category[] var8 = Category.values();
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            Category category = var8[var10];
            if (!category.equals(Category.CONFIGS) && !category.equals(Category.THEMES) && !category.equals(Category.SCRIPTS)) {
               boolean hovered = RenderUtility.isHovered((float)mouseX, (float)mouseY, catX + 15.0F + (float)catIndex, catY + 9.0F, (float)Fonts.mntsb22.getStringWidth(category.getIcon()), (float)Fonts.mntsb22.getFontHeight(""));
               if (hovered && !selected.equals(category)) {
                  selected = category;
                  this.scrollY = 32.5F;
                  moduleAnimation.reset();
               }

               catIndex += category.equals(Category.MOVEMENT) ? 30 : (category.equals(Category.PLAYER) ? 22 : 25);
            }
         }

         if (RenderUtility.isHovered((float)mouseX, (float)mouseY, (float)(this.x + 130), (float)(this.y + 5), 60.0F, 20.0F)) {
            selected = Category.CONFIGS;
            this.scrollY = 32.5F;
            moduleAnimation.reset();
         }

         if (RenderUtility.isHovered((float)mouseX, (float)mouseY, (float)(this.x + 195), (float)(this.y + 5), 63.0F, 20.0F)) {
            selected = Category.THEMES;
            this.scrollY = 32.5F;
            moduleAnimation.reset();
         }
      }

      if (!RenderUtility.isHovered((float)mouseX, (float)mouseY, catX, catY, (float)categoryWidth, 25.0F)) {
         if (colorPicker != null) {
            colorPicker.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
         } else {
            Iterator var13 = modules.iterator();

            ModuleComponent moduleComponent;
            while(var13.hasNext()) {
               moduleComponent = (ModuleComponent)var13.next();
               if (moduleComponent.getModule().getCategory().equals(selected) && moduleComponent.getModule().isSearched()) {
                  moduleComponent.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
               }
            }

            var13 = modules2.iterator();

            while(var13.hasNext()) {
               moduleComponent = (ModuleComponent)var13.next();
               if (moduleComponent.getModule().getCategory().equals(selected) && moduleComponent.getModule().isSearched()) {
                  moduleComponent.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
               }
            }

            if (selected.equals(Category.THEMES)) {
               var13 = themes.iterator();

               while(var13.hasNext()) {
                  ThemeComponent themeComponent = (ThemeComponent)var13.next();
                  themeComponent.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
               }
            }

            if (selected.equals(Category.CONFIGS)) {
               var13 = configs.iterator();

               ConfigComponent configComponent;
               while(var13.hasNext()) {
                  configComponent = (ConfigComponent)var13.next();
                  if (configComponent.mouseBoolClicked((double)mouseX, (double)mouseY, mouseButton)) {
                     updateConfigComponents();
                     break;
                  }
               }

               var13 = configs2.iterator();

               while(var13.hasNext()) {
                  configComponent = (ConfigComponent)var13.next();
                  if (configComponent.mouseBoolClicked((double)mouseX, (double)mouseY, mouseButton)) {
                     updateConfigComponents();
                     break;
                  }
               }
            }

         }
      }
      return       super.mouseClicked(mouseX, mouseY, mouseButton);
   }
   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int state) {
      if (colorPicker != null) {
         colorPicker.mouseReleased((double)mouseX, (double)mouseY, state);
      }
      return       super.mouseReleased(mouseX, mouseY, state);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      Iterator var3 = modules.iterator();

      ModuleComponent moduleComponent;
      while(var3.hasNext()) {
         moduleComponent = (ModuleComponent)var3.next();
         if (moduleComponent.getModule().getCategory().equals(selected) && moduleComponent.getModule().isSearched()) {
            moduleComponent.keyTyped(keyCode);
         }
      }

      var3 = modules2.iterator();

      while(var3.hasNext()) {
         moduleComponent = (ModuleComponent)var3.next();
         if (moduleComponent.getModule().getCategory().equals(selected) && moduleComponent.getModule().isSearched()) {
            moduleComponent.keyTyped(keyCode);
         }
      }

      if (keyCode == 1 && !escapeInUse) {
         openAnimation.setDirection(Direction.BACKWARDS);
         openAnimation.setDuration(225);
         this.isClosed = true;
      } else {
         escapeInUse = false;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public static float getAnimationAlpha() {
      return moduleAnimation.getOutput();
   }

   static {
      selected = Category.COMBAT;
      modules = new ArrayList();
      modules2 = new ArrayList();
      themes = new ArrayList();
      configs = new ArrayList();
      configs2 = new ArrayList();
      openAnimation = new EaseInOutQuad(250, 1.0F, Direction.FORWARDS);
      moduleAnimation = new DecelerateAnimation(500, 1.0F, Direction.BACKWARDS);
   }
}
