package omg.sertyo;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.ibm.icu.number.Scale;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import omg.sertyo.command.CommandManager;
import omg.sertyo.event.input.EventInputKey;
import omg.sertyo.event.render.EventRender2D;
import omg.sertyo.manager.config.ConfigManager;
import omg.sertyo.manager.dragging.DragManager;
import omg.sertyo.manager.dragging.Draggable;
import omg.sertyo.manager.friend.FriendManager;
import omg.sertyo.manager.macro.MacroManager;
import omg.sertyo.manager.quickjoin.ServerManager;
import omg.sertyo.manager.theme.ThemeManager;
import omg.sertyo.module.Module;
import omg.sertyo.module.ModuleManager;
import omg.sertyo.ui.csgui.CsGui;
import omg.sertyo.utility.math.ScaleMath;
import omg.sertyo.utility.render.RenderUtility;
import omg.sertyo.utility.render.font.Fonts;
import org.lwjgl.glfw.GLFW;
import sovokguard.ApiContacts;

import static omg.sertyo.utility.Utility.mc;

/*

Я залез бабке под юбку
И лизнул её шкатулку
А уже через минутку
Я ебу бабульку

Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку
Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку

Дряхлый улей вместо вульвы
Бабка на грани инсульта
Старой надо пить пилюльку
Хочу ебать бабульку

Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку

Я залез бабке под юбку
И лизнул её шкатулку
А уже через минутку
Я ебу бабульку

Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку

Позвала сегодня внучка
Предлагала свою лунку
Но мне по боку на сучку
Хочу её бабульку

Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку

Ебу, ебу бабульку
Я ебу, ебу бабульку
Я ебу, ебу бабульку
Сочную бабульку

 */
@Getter
public class Babulka {
    public static String name = "Babulka";
    public static boolean unhooked = false;
    public String edition = "1.21.1 edition";
    public String build = "0.1 ";
    public String build_type = "release";
    private ThemeManager themeManager;
    private CommandManager commandManager;
    private FriendManager friendManager;
    private ScaleMath scaleMath;
    public Fonts font;
    public DragManager dragManager;
    public ModuleManager moduleManager;
    public ConfigManager configManager;
    public MacroManager macroManager;
    public ServerManager serverManager;

    @Getter
    private static final Babulka instance = new Babulka();
    public void start() throws Exception {
        EventManager.register(this);
        ApiContacts.start();
        themeManager = new ThemeManager();
        commandManager = new CommandManager();
        friendManager = new FriendManager();
        friendManager.init();
        dragManager = new DragManager();
        dragManager.init();
        macroManager = new MacroManager();
        macroManager.init();
        serverManager = new ServerManager();
        serverManager.init();
        moduleManager = new ModuleManager();
        configManager = new ConfigManager();
        configManager.loadConfig("autocfg");
        font = new Fonts();
        font.init();
        if (!System.getProperty("os.name").contains("mac"))
         scaleMath = new ScaleMath(2);
        else
         scaleMath = new ScaleMath(2);
        System.out.println("YA ZALEZ BABKE BOD YBKY");
        configManager.loadConfig("autocfg");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    public void shutdown() {
        this.dragManager.save();
        //this.altFileManager.saveAll();
        this.configManager.saveConfig("autocfg");
    }
    @EventTarget
    public void onRender(EventRender2D event) {
        RenderUtility.drawRoundedRect(4, 2, 50, 50, 4, -1);
    }
    @EventTarget
    public void onInputKey(EventInputKey eventInputKey) {
        if (!unhooked) {
            for (Module module : this.moduleManager.getModules()) {
                if (module.getBind() == eventInputKey.getKey()) {
                    module.toggle();
                }
            }
            if (eventInputKey.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT)
                mc.setScreen(new CsGui());
        }
    }


}
