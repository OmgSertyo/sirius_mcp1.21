package omg.sertyo.utility.render.animation;

import net.minecraft.client.Minecraft;
import omg.sertyo.utility.math.MathUtility;

public class AnimationMath {
   public static double deltaTime() {
      return Minecraft.getInstance().getFps() > 0 ? 1.0D / (double)Minecraft.getInstance().getFps() : 1.0D;
   }

   public static float fast(float end, float start, float multiple) {
      return (1.0F - MathUtility.clamp((float)(deltaTime() * (double)multiple), 0.0F, 1.0F)) * end + MathUtility.clamp((float)(deltaTime() * (double)multiple), 0.0F, 1.0F) * start;
   }
}
