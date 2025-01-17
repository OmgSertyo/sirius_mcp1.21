package omg.sertyo.manager.macro;

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
import net.minecraft.network.chat.Component;

public class MacroManager {
   public List<Macro> macros = new ArrayList();
   private static final File macroFile;

   public void init() throws IOException {
      if (!macroFile.exists()) {
         macroFile.createNewFile();
      } else {
         this.readMacro();
      }

   }

   public void addMacros(Macro macro) {
      this.macros.add(macro);
      this.updateFile();
   }

   public void deleteMacro(int key) {
      this.macros.removeIf((macro) -> {
         return macro.getKey() == key;
      });
      this.updateFile();
   }

   public void onKeyPressed(int key) {
      this.macros.stream().filter((macro) -> {
         return macro.getKey() == key;
      }).forEach((macro) -> {
         Minecraft.getInstance().player.sendSystemMessage(Component.literal(macro.getMessage()));
      });
   }

   public void onMousePressed(int button) {
      this.macros.stream().filter((macro) -> {
         return macro.getKey() == button - 100;
      }).forEach((macro) -> {
         Minecraft.getInstance().player.sendSystemMessage(Component.literal(macro.getMessage()));
      });
   }

   public void updateFile() {
      try {
         StringBuilder builder = new StringBuilder();
         this.macros.forEach((macro) -> {
            builder.append(macro.getMessage()).append(":").append(String.valueOf(macro.getKey()).toUpperCase()).append("\n");
         });
         Files.write(macroFile.toPath(), builder.toString().getBytes(), new OpenOption[0]);
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   private void readMacro() {
      try {
         FileInputStream fileInputStream = new FileInputStream(macroFile.getAbsolutePath());
         BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));

         String line;
         while((line = reader.readLine()) != null) {
            String curLine = line.trim();
            String command = curLine.split(":")[0];
            String key = curLine.split(":")[1];
            this.macros.add(new Macro(command, Integer.parseInt(key)));
         }

         reader.close();
      } catch (Exception var7) {
         var7.printStackTrace();
      }

   }

   public List<Macro> getMacros() {
      return this.macros;
   }

   static {
      macroFile = new File(Minecraft.getInstance().gameDirectory, "\\Babulka\\macro.neiron");
   }
}
