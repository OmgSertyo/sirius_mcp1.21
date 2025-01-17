package omg.sertyo.event.render;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.client.gui.GuiGraphics;

public class EventRender2D implements Event {

   private GuiGraphics guiGraphics;
   private float partialTicks;

   public EventRender2D() {
      this.guiGraphics = null;
      this.partialTicks = 1;
   }

   public void setPoseStack(GuiGraphics poseStack) {
      this.guiGraphics = poseStack;
   }

   public void setPartialTicks(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   public float getPartialTicks() {
      return partialTicks;
   }

   public GuiGraphics getGuiGraphics() {
      return guiGraphics;
   }
}