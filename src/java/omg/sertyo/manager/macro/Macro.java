package omg.sertyo.manager.macro;

public class Macro {
   private String message;
   private int key;

   public String getMessage() {
      return this.message;
   }

   public int getKey() {
      return this.key;
   }

   public Macro(String message, int key) {
      this.message = message;
      this.key = key;
   }
}
