package omg.sertyo.command.impl;

import net.minecraft.ChatFormatting;
import omg.sertyo.command.Command;
import omg.sertyo.command.CommandAbstract;
import omg.sertyo.command.CommandManager;

@Command(
        name = "prefix",
        description = "Изменяет префикс для команд"
)
public class PrefixCommand extends CommandAbstract {
   public void error() {
      this.sendMessage(ChatFormatting.GRAY + "Ошибка в использовании" + ChatFormatting.WHITE + ":");
      this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + "prefix " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "symbol" + ChatFormatting.GRAY + ">");
   }

   public void execute(String[] args) throws Exception {
      CommandManager.setPrefix(args[1]);
      this.sendMessage(ChatFormatting.GRAY + "Префикс успешно изменен на " + ChatFormatting.RED + args[1]);
   }
}
