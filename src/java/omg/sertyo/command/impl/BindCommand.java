package omg.sertyo.command.impl;

import net.minecraft.ChatFormatting;
import omg.sertyo.Babulka;
import omg.sertyo.command.Command;
import omg.sertyo.command.CommandAbstract;
import omg.sertyo.module.Module;
import omg.sertyo.utility.misc.KeyMappings;

import java.util.Iterator;
//omg.sertyo
@Command(
        name = "bind",
        description = "Позволяет биндить модули"
)
public class BindCommand extends CommandAbstract {
  public void error() {
    this.sendMessage(ChatFormatting.GRAY + "Ошибка в использовании" + ChatFormatting.WHITE + ":");
    this.sendMessage(ChatFormatting.WHITE + ".bind " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "name" + ChatFormatting.GRAY + "> <" + ChatFormatting.RED + "key" + ChatFormatting.GRAY + "> - забиндить модуль");
    this.sendMessage(ChatFormatting.WHITE + ".bind " + ChatFormatting.GRAY + "<" + ChatFormatting.RED + "name" + ChatFormatting.GRAY + "> " + ChatFormatting.WHITE + "none" + ChatFormatting.GRAY + " - разбиндить модуль");
    this.sendMessage(ChatFormatting.WHITE + ".bind list" + ChatFormatting.GRAY + " - список всех биндов");
    this.sendMessage(ChatFormatting.WHITE + ".bind clear" + ChatFormatting.GRAY + " - очистить все бинды");
  }

  public void execute(String[] args) throws Exception {
    if (args.length >= 2) {
      Iterator var4;
      Module module;
      if (args[1].equals("clear")) {



        this.sendMessage(ChatFormatting.GRAY + "Все бинды были очищены!");
        return;
      }

      if (args[1].equals("list")) {
        this.sendMessage(ChatFormatting.GREEN + "Список биндов: ");
        var4 = Babulka.getInstance().getModuleManager().getModules().iterator();

        while(var4.hasNext()) {
          module = (Module)var4.next();
          if (module.getBind() != 0) {
            if (module.getBind() < 0) {
              this.sendMessage(ChatFormatting.WHITE + "Модуль: " + ChatFormatting.RED + module.getName() + ChatFormatting.WHITE + ", Кнопка: " + ChatFormatting.RED + (module.getBind() + 100));
            } else {
              this.sendMessage(ChatFormatting.WHITE + "Модуль: " + ChatFormatting.RED + module.getName() + ChatFormatting.WHITE + ", Клавиша: " + ChatFormatting.RED + Babulka.getKey(module.getBind()));
            }
          }
        }

        return;
      }

      module = Babulka.getInstance().getModuleManager().getModule(args[1]);
      if (module == null) {
        this.sendMessage(ChatFormatting.GRAY + "Модуль " + ChatFormatting.RED + args[1] + ChatFormatting.GRAY + " не существует!");
      } else if (args[2].equalsIgnoreCase("none")) {
        module.bind = 0;
        this.sendMessage(ChatFormatting.GRAY + "Модуль " + ChatFormatting.RED + args[1] + ChatFormatting.GRAY + " был разбинжен!");
      } else {
        int keyBind = KeyMappings.keyMap.get(args[2].toUpperCase());
        if (keyBind == 0) {
          this.sendMessage(ChatFormatting.GRAY + "Такой клавиши не существует!");
        } else {
          module.bind = keyBind;
          this.sendMessage(ChatFormatting.GRAY + "Модуль " + ChatFormatting.RED + args[1] + ChatFormatting.GRAY + " был забинжен на клавишу " + ChatFormatting.RED + args[2].toUpperCase());
        }
      }
    } else {
      this.error();
    }

  }
}
