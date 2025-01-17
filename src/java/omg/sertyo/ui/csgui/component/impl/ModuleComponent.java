//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package omg.sertyo.ui.csgui.component.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import omg.sertyo.module.Module;
import omg.sertyo.module.setting.Setting;
import omg.sertyo.module.setting.impl.BooleanSetting;
import omg.sertyo.module.setting.impl.ColorSetting;
import omg.sertyo.module.setting.impl.ModeSetting;
import omg.sertyo.module.setting.impl.MultiBooleanSetting;
import omg.sertyo.module.setting.impl.NumberSetting;
import omg.sertyo.ui.csgui.CsGui;
import omg.sertyo.ui.csgui.component.Component;
import omg.sertyo.utility.render.ColorUtility;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.animation.AnimationMath;
import omg.sertyo.utility.render.font.Fonts;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ModuleComponent extends Component {
   private final Module module;
   public boolean binding;
   public List<Component> elements = new ArrayList();
   public float enableAnimation = 0.0F;

   public ModuleComponent(Module module, float width, float height) {
      super(0.0F, 0.0F, width, height);
      this.module = module;
      Iterator var4 = module.getSettings().iterator();

      while(var4.hasNext()) {
         Setting setting = (Setting)var4.next();
         if (setting instanceof BooleanSetting) {
            this.elements.add(new BooleanComponent(this, (BooleanSetting)setting));
         } else if (setting instanceof NumberSetting) {
            this.elements.add(new SliderComponent(this, (NumberSetting)setting));
         } else if (setting instanceof ModeSetting) {
            this.elements.add(new ModeComponent(this, (ModeSetting)setting));
         } else if (setting instanceof MultiBooleanSetting) {
            this.elements.add(new MultiBoolComponent(this, (MultiBooleanSetting)setting));
         } else if (setting instanceof ColorSetting) {
            this.elements.add(new ColorComponent(this, (ColorSetting)setting));
         }
      }

   }

   public void render(PoseStack stack, int mouseX, int mouseY) {
      int offset = 0;
      Iterator var4 = this.elements.iterator();

      while(var4.hasNext()) {
         Component element = (Component)var4.next();
         if (element.isVisible()) {
            offset = (int)((float)offset + element.height);
         }
      }

      float normalHeight = this.height + (float)offset;
      int elementsColor = Color.decode("#1E1F30").getRGB();
      Color glowColor = ColorUtility.applyOpacity(Color.BLACK, 0.2F);
      //RenderUtility.drawGradientGlow(this.x, this.y, this.width, normalHeight, 5, glowColor, glowColor);
      RenderUtility.drawRoundedRect(this.x, this.y, this.width, normalHeight, 10.0F, elementsColor);
      //String var10000 = this.module.bind < 0 ? "MOUSE " + this.module.getMouseBind() : Keyboard.getKeyName(this.module.getBind());
      //String bindText = "[" + var10000 + "]";
      Fonts.mntsb14.drawString(stack, this.module.getName(), this.x + 5.0F, this.y + 9.0F, -1);
      this.enableAnimation = AnimationMath.fast(this.enableAnimation, this.module.enabled ? -1.0F : 0.0F, 15.0F);
  //    RenderUtility.drawGradientGlow(this.x + this.width - 25.0F, this.y + 6.0F, 17.5F, 10.0F, 5, glowColor, glowColor);
      RenderUtility.drawRoundedRect(this.x + this.width - 25.0F, this.y + 6.0F, 17.5F, 10.0F, 8.0F, Color.decode("#2B2C44").getRGB());
      Color c = ColorUtility.interpolateColorC((new Color(78, 79, 98)).getRGB(), (new Color(202, 202, 208)).getRGB(), Math.abs(this.enableAnimation));
      RenderUtility.drawRoundedRect(this.x + this.width - 23.0F - this.enableAnimation * 7.0F, this.y + 8.0F, 6.0F, 6.0F, 5.0F, c.getRGB());
      offset = 0;
      Iterator var9 = this.elements.iterator();

      while(var9.hasNext()) {
         Component element = (Component)var9.next();
         if (element.isVisible()) {
            element.x = this.x;
            element.y = this.y + 18.0F + (float)offset;
            element.width = this.width;
            element.render(stack,mouseX, mouseY);
            offset = (int)((float)offset + element.height);
         }
      }

   }

   public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.binding && mouseButton > 2) {
         this.module.bind = mouseButton - 100;
         this.binding = false;
      }

      boolean enableButtonHovered = RenderUtility.isHovered(mouseX, mouseY, (double)(this.x + this.width - 25.0F), (double)(this.y + 6.0F), 17.5, 10.0);
      boolean isTitleHovered = RenderUtility.isHovered(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, 20.0);
      if (enableButtonHovered && mouseButton == 0) {
         this.module.toggle();
      } else if (isTitleHovered && mouseButton == 2) {
         this.binding = !this.binding;
      }

      Iterator var8 = this.elements.iterator();

      while(var8.hasNext()) {
         Component element = (Component)var8.next();
         element.mouseClicked(mouseX, mouseY, mouseButton);
      }

   }

   public void keyTyped(int keyCode) {
      super.keyTyped(keyCode);
      if (this.binding) {
         if (keyCode == 1) {
            CsGui.escapeInUse = true;
            this.binding = false;
            return;
         }

         if (keyCode == 211) {
            this.module.bind = 0;
         } else {
            this.module.bind = keyCode;
         }

         this.binding = false;
      }

   }

   public Module getModule() {
      return this.module;
   }
}
