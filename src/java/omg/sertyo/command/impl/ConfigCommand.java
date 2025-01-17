package omg.sertyo.command.impl;

import net.minecraft.ChatFormatting;
import omg.sertyo.Babulka;
import omg.sertyo.command.Command;
import omg.sertyo.command.CommandAbstract;
import omg.sertyo.command.CommandManager;
import omg.sertyo.manager.config.ConfigManager;
import java.io.File;
import java.util.Objects;
import net.minecraft.client.Minecraft;

@Command(
   name = "cfg",
   description = "Позволяет управлять конфигами чита"
)
public class ConfigCommand extends CommandAbstract {
   public void error() {
      this.sendMessage(ChatFormatting.GRAY + "Ошибка в использовании" + ChatFormatting.WHITE + ":");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "cfg load " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "name" + ChatFormatting.GRAY + ">");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "cfg save " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "name" + ChatFormatting.GRAY + ">");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "cfg delete " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "name" + ChatFormatting.GRAY + ">");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "cfg list" + ChatFormatting.GRAY + " - список конфигов");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "cfg dir" + ChatFormatting.GRAY + " - открыть папку с конфигами");
   }

   public void execute(String[] args) throws Exception {
      if (args.length >= 2) {
         String var2 = args[1];
         byte var3 = -1;
         switch(var2.hashCode()) {
         case -1335458389:
            if (var2.equals("delete")) {
               var3 = 4;
            }
            break;
         case 99469:
            if (var2.equals("dir")) {
               var3 = 0;
            }
            break;
         case 3322014:
            if (var2.equals("list")) {
               var3 = 1;
            }
            break;
         case 3327206:
            if (var2.equals("load")) {
               var3 = 2;
            }
            break;
         case 3522941:
            if (var2.equals("save")) {
               var3 = 3;
            }
         }

         switch(var3) {
         case 0:
            Runtime.getRuntime().exec("explorer " + new File(Minecraft.getInstance().gameDirectory, "\\Babulka\\configs"));
            break;
         case 1:
            File file = new File(Minecraft.getInstance().gameDirectory, "\\Babulka\\configs");
            if (ConfigManager.getLoadedConfigs().isEmpty()) {
               this.sendMessage("Конфигурации не найдены.");
               break;
            } else {
               this.sendMessage(ChatFormatting.GREEN + "Список конфигураций: ");
               File[] var5 = (File[])Objects.requireNonNull(file.listFiles());
               int var6 = var5.length;

               for(int var7 = 0; var7 < var6; ++var7) {
                  File s = var5[var7];
                  this.sendMessage(s.getName().replaceAll(".neiron", ""));
               }

               return;
            }
         case 2:
            if (Babulka.getInstance().getConfigManager().loadConfig(args[2])) {
               this.sendMessage(ChatFormatting.GRAY + "Конфигурация " + ChatFormatting.RED + args[2] + ChatFormatting.GRAY + " была загружена.");
            } else {
               this.sendMessage(ChatFormatting.GRAY + "Конфигурация " + ChatFormatting.RED + args[2] + ChatFormatting.GRAY + " не была загружена. Скорее всего, она не существует.");
            }
            break;
         case 3:
            Babulka.getInstance().getConfigManager().saveConfig(args[2]);
            this.sendMessage(ChatFormatting.GRAY + "Конфигурация " + ChatFormatting.RED + args[2] + ChatFormatting.GRAY + " была сохранена.");
            break;
         case 4:
            if (Babulka.getInstance().getConfigManager().deleteConfig(args[2])) {
               this.sendMessage(ChatFormatting.GRAY + "Конфигурация " + ChatFormatting.RED + args[2] + ChatFormatting.GRAY + " была удалена.");
            } else {
               this.sendMessage(ChatFormatting.GRAY + "Конфигурация " + ChatFormatting.RED + args[2] + ChatFormatting.GRAY + " не была удалена. Скорее всего, она не существует.");
            }
         }
      } else {
         this.error();
      }

   }
}
