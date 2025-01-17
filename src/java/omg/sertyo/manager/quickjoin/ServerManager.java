package omg.sertyo.manager.quickjoin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class ServerManager {
   public List<ServerInstance> servers = new ArrayList();
   private static final File quickjoinFile;

   public void init() throws IOException {
      if (!quickjoinFile.exists()) {
         quickjoinFile.createNewFile();
      } else {
         this.readServers();
      }

      this.addDefault();
   }

   public void addDefault() {
      if (this.getServers().isEmpty()) {
         this.addServer(new ServerInstance("ReallyWorld", "mc.reallyworld.ru"));
         this.addServer(new ServerInstance("SunRise", "play.sunmc.ru"));
         this.addServer(new ServerInstance("PlayMine", "mc.playmine.org"));
      }
   }

   public void addServer(ServerInstance server) {
      this.servers.add(server);
      this.updateFile();
   }

   public void updateFile() {
      try {
         StringBuilder builder = new StringBuilder();
         this.servers.forEach((server) -> {
            builder.append(server.getName()).append(":").append(server.getIp()).append("\n");
         });
         Files.write(quickjoinFile.toPath(), builder.toString().getBytes(), new OpenOption[0]);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   private void readServers() {
      try {
         FileInputStream fileInputStream = new FileInputStream(quickjoinFile.getAbsolutePath());
         BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));

         String line;
         while((line = reader.readLine()) != null) {
            String curLine = line.trim();
            String command = curLine.split(":")[0];
            String key = curLine.split(":")[1];
            this.servers.add(new ServerInstance(command, key));
         }

         reader.close();
      } catch (Exception var7) {
         var7.printStackTrace();
      }

   }

   public List<ServerInstance> getServers() {
      return this.servers;
   }

   static {
      quickjoinFile = new File(Minecraft.getInstance().gameDirectory, "\\Babulka\\quickjoin.neiron");
   }
}
