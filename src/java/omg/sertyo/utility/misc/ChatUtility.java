package omg.sertyo.utility.misc;



import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import omg.sertyo.Babulka;
import omg.sertyo.utility.Utility;


public class ChatUtility implements Utility {
    public static String chatPrefix;

    public static void addChatMessage(String message) {
        Component textComponent = Component.literal(chatPrefix + message);
        mc.player.sendSystemMessage(textComponent);

    }

    static {
        chatPrefix = ChatFormatting.DARK_GRAY + "[" + ChatFormatting.BLUE + ChatFormatting.BOLD + Babulka.name + ChatFormatting.DARK_GRAY + "] >> " + ChatFormatting.RESET;
    }
}
