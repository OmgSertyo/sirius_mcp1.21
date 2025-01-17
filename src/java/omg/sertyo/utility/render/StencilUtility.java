package omg.sertyo.utility.render;


import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import omg.sertyo.utility.Utility;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class StencilUtility implements Utility {
   public static void checkSetupFBO(RenderTarget framebuffer) {
      if (framebuffer != null && framebuffer.depthBufferId > -1) {
         setupFBO(framebuffer);
         framebuffer.depthBufferId = -1;
      }

   }

   public static void setupFBO(RenderTarget framebuffer) {
      if (framebuffer == null || framebuffer.depthBufferId == -1) {
         System.err.println("Framebuffer или depthBufferId не инициализирован.");
         return;
      }

      EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBufferId);
      int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
      EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
      EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight());
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
   }


   public static void doDispose() {
      GL11.glDisable(2960);
      GlStateManager.disableAlphaTest();
      GlStateManager._disableBlend();
   }

   public static void erase(boolean invert) {
      GL11.glStencilFunc(invert ? 514 : 517, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
      GlStateManager._colorMask(true, true, true, true);
      GlStateManager.enableAlphaTest();
      GlStateManager._enableBlend();
      GL11.glAlphaFunc(516, 0.0F);
   }

   public static void write(boolean renderClipLayer) {
      checkSetupFBO(mc.getMainRenderTarget());
      GL11.glClearStencil(0);
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 65535);
      GL11.glStencilOp(7680, 7680, 7681);
      if (!renderClipLayer) {
         GlStateManager._colorMask(false, false, false, false);
      }

   }

   public static void write() {
      mc.getMainRenderTarget().bindWrite(false);
      checkSetupFBO(mc.getMainRenderTarget());
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 1);
      GL11.glStencilOp(7681, 7681, 7681);
      GL11.glColorMask(false, false, false, false);
   }

   public static void initStencilToWrite() {
      mc.getMainRenderTarget().bindWrite(false);
      checkSetupFBO(mc.getMainRenderTarget());
      GL11.glClear(1024);
      GL11.glEnable(2960);
      GL11.glStencilFunc(519, 1, 1);
      GL11.glStencilOp(7681, 7681, 7681);
      GL11.glColorMask(false, false, false, false);
   }

   public static void readStencilBuffer(int ref) {
      GL11.glColorMask(true, true, true, true);
      GL11.glStencilFunc(514, ref, 1);
      GL11.glStencilOp(7680, 7680, 7680);
   }

   public static void uninitStencilBuffer() {
      GL11.glDisable(2960);
   }
}
