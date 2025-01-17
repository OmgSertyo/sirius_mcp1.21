package omg.sertyo.command;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.ChatFormatting;
import omg.sertyo.Babulka;
import omg.sertyo.event.misc.EventMessage;
import omg.sertyo.utility.misc.ChatUtility;

public class CommandHandler {
   public CommandManager commandManager;

   public CommandHandler(CommandManager commandManager) {
      this.commandManager = commandManager;
   }

   @EventTarget
   public void onMessage(EventMessage event) {
      if (!Babulka.unhooked) {
         String msg = event.getMessage();
         if (msg.startsWith(CommandManager.getPrefix())) {
            event.setCancelled(true);
            if (!this.commandManager.execute(msg)) {
               ChatUtility.addChatMessage(ChatFormatting.GRAY + "Такой команды не существует.");
            }
         }
      }
   }
}
