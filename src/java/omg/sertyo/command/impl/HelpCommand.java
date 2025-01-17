package omg.sertyo.command.impl;

import net.minecraft.ChatFormatting;
import omg.sertyo.Babulka;
import omg.sertyo.command.Command;
import omg.sertyo.command.CommandAbstract;
import omg.sertyo.command.CommandManager;
import java.util.Iterator;

@Command(
   name = "help",
   description = "help"
)
public class HelpCommand extends CommandAbstract {
   public void execute(String[] args) throws Exception {
      this.sendMessage(ChatFormatting.GRAY + "Список команд: ");
      Iterator var2 = Babulka.getInstance().getCommandManager().getCommands().iterator();

      while(var2.hasNext()) {
         CommandAbstract command = (CommandAbstract)var2.next();
         if (!(command instanceof HelpCommand)) {
            this.sendMessage(ChatFormatting.WHITE + CommandManager.getPrefix() + command.name + ChatFormatting.GRAY + " - " + command.description);
         }
      }

   }

   public void error() {
      this.sendMessage(ChatFormatting.RED + "Error");
   }
}
