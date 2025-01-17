package omg.sertyo.utility.math;

import omg.sertyo.utility.Utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtility implements Utility {
   public static float clamp(float val, float min, float max) {
      if (val <= min) {
         val = min;
      }

      if (val >= max) {
         val = max;
      }

      return val;
   }

   public static double round(double num, double increment) {
      double v = (double)Math.round(num / increment) * increment;
      BigDecimal bd = new BigDecimal(v);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return bd.doubleValue();
   }

   public static float randomizeFloat(float min, float max) {
      return (float)(Math.random() * (double)(max - min)) + min;
   }

   public static double getDistance(double x1, double y1, double x2, double y2) {
      return Math.sqrt(Math.pow(x2 - x1, 2.0D) + Math.pow(y2 - y1, 2.0D));
   }

   public static float lerp(float a, float b, float f) {
      return a + f * (b - a);
   }

   public static int getMiddle(int old, int newValue) {
      return (old + newValue) / 2;
   }

   public static int getRandomInRange(int int1, int int2) {
      return (int)(Math.random() * (double)(int2 - int1) + (double)int1);
   }
}
