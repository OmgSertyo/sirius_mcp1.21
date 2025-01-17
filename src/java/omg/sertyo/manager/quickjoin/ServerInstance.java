package omg.sertyo.manager.quickjoin;

public class ServerInstance {
   private String name;
   private String ip;

   public String getName() {
      return this.name;
   }

   public String getIp() {
      return this.ip;
   }

   public ServerInstance(String name, String ip) {
      this.name = name;
      this.ip = ip;
   }
}
