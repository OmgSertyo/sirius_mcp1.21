package omg.sertyo.command.impl;

import net.minecraft.ChatFormatting;
import omg.sertyo.Babulka;
import omg.sertyo.command.Command;
import omg.sertyo.command.CommandAbstract;
import omg.sertyo.manager.macro.Macro;
import omg.sertyo.utility.misc.KeyMappings;
import omg.sertyo.utility.misc.StringUtility;

import java.util.Iterator;

@Command(
   name = "macro",
   description = "Позволяет отправить команду по нажатию кнопки"
)
public class MacroCommand extends CommandAbstract {
   public void error() {
      this.sendMessage(ChatFormatting.GRAY + "Ошибка в использовании" + ChatFormatting.WHITE + ":");
      this.sendMessage(ChatFormatting.WHITE + ".macro add " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "key" + ChatFormatting.GRAY + "> <" + ChatFormatting.RED + "message" + ChatFormatting.GRAY + ">");
      this.sendMessage(ChatFormatting.WHITE + ".macro remove " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "key" + ChatFormatting.GRAY + ">");
      this.sendMessage(ChatFormatting.WHITE + ".macro list");
      this.sendMessage(ChatFormatting.WHITE + ".macro clear");
   }

   public void execute(String[] args) throws Exception {
      if (args.length > 1) {
         String var2 = args[1];
         byte var3 = -1;
         switch(var2.hashCode()) {
         case -934610812:
            if (var2.equals("remove")) {
               var3 = 2;
            }
            break;
         case 96417:
            if (var2.equals("add")) {
               var3 = 0;
            }
            break;
         case 3322014:
            if (var2.equals("list")) {
               var3 = 3;
            }
            break;
         case 94746189:
            if (var2.equals("clear")) {
               var3 = 1;
            }
         }

         int digit;
         switch(var3) {
         case 0:
            int keyIndex = KeyMappings.keyMap.get(args[2].toUpperCase());
            StringBuilder sb = new StringBuilder();

            for(int i = 3; i < args.length; ++i) {
               sb.append(args[i]).append(" ");
            }

            String message = sb.toString().trim();
            String redMessage = StringUtility.getStringRedColor(message);
            if (keyIndex == 0) {
               if (args[2].startsWith("mouse")) {
                  try {
                     digit = Integer.parseInt(args[2].replaceAll("mouse", ""));
                     if (digit < 100) {
                        Babulka.getInstance().getMacroManager().addMacros(new Macro(message, digit - 100));
                        this.sendMessage(ChatFormatting.GRAY + "Успешно добавлен макрос для кнопки мыши" + ChatFormatting.RED + " \"" + digit + ChatFormatting.RED + "\" " + ChatFormatting.GRAY + "с командой " + ChatFormatting.RED + redMessage);
                     } else {
                        this.sendMessage(ChatFormatting.GRAY + "Кнопка мыши не может быть 100 и больше!");
                     }
                  } catch (NumberFormatException var12) {
                     this.error();
                  }
               } else {
                  this.sendMessage(ChatFormatting.GRAY + "Такой клавиши не существует!");
               }
            } else {
               Babulka.getInstance().getMacroManager().addMacros(new Macro(message, keyIndex));
               this.sendMessage(ChatFormatting.GRAY + "Успешно добавлен макрос для кнопки" + ChatFormatting.RED + " \"" + args[2].toUpperCase() + ChatFormatting.RED + "\" " + ChatFormatting.GRAY + "с командой " + ChatFormatting.RED + redMessage);
            }
            break;
         case 1:
            if (Babulka.getInstance().getMacroManager().getMacros().isEmpty()) {
               this.sendMessage(ChatFormatting.GRAY + "Список макросов пуст.");
            } else {
               this.sendMessage(ChatFormatting.GRAY + "Список макросов успешно очищен!");
               Babulka.getInstance().getMacroManager().getMacros().clear();
               Babulka.getInstance().getMacroManager().updateFile();
            }
            break;
         case 2:
            digit = KeyMappings.keyMap.get(args[2].toUpperCase());
            if (digit == 0) {
               if (args[2].startsWith("mouse")) {
                  String digits = StringUtility.getDigits(args[2]);

                  try {
                     digit = Integer.parseInt(digits);
                     Babulka.getInstance().getMacroManager().deleteMacro(digit - 100);
                     this.sendMessage(ChatFormatting.GRAY + "Макрос был удален с кнопки " + ChatFormatting.RED + "\"" + args[2] + "\"");
                  } catch (NumberFormatException var11) {
                     this.error();
                  }
               } else {
                  this.sendMessage(ChatFormatting.GRAY + "Такой клавиши не существует!");
               }
            } else {
               Babulka.getInstance().getMacroManager().deleteMacro(digit);
               this.sendMessage(ChatFormatting.GRAY + "Макрос был удален с кнопки " + ChatFormatting.RED + "\"" + args[2].toUpperCase() + "\"");
            }
            break;
         case 3:
            if (Babulka.getInstance().getMacroManager().getMacros().isEmpty()) {
               this.sendMessage(ChatFormatting.GRAY + "Список макросов пуст.");
            } else {
               this.sendMessage(ChatFormatting.GREEN + "Список макросов: ");
               Iterator var9 = Babulka.getInstance().getMacroManager().getMacros().iterator();

               while(var9.hasNext()) {
                  Macro macro = (Macro)var9.next();
                  if (macro.getKey() < 0) {
                     this.sendMessage(ChatFormatting.WHITE + "Команда: " + ChatFormatting.RED + macro.getMessage() + ChatFormatting.WHITE + ", Кнопка: " + ChatFormatting.RED + (macro.getKey() + 100));
                  } else {
                     this.sendMessage(ChatFormatting.WHITE + "Команда: " + ChatFormatting.RED + macro.getMessage() + ChatFormatting.WHITE + ", Клавиша: " + ChatFormatting.RED + Babulka.getKey(macro.getKey()));
                  }
               }
            }
         }
      } else {
         this.error();
      }

   }
}
