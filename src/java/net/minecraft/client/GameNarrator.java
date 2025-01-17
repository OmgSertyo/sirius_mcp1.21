package net.minecraft.client;

import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

public class GameNarrator {
    public static final Component NO_TITLE = CommonComponents.EMPTY;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final Narrator narrator = Narrator.getNarrator();

    public GameNarrator(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void sayChat(Component pMessage) {
    }

    public void say(Component pMessage) {
    }

    public void sayNow(Component pMessage) {
        this.sayNow(pMessage.getString());
    }

    public void sayNow(String pMessage) {
    }

    private NarratorStatus getStatus() {
        return NarratorStatus.OFF;
    }



    public void updateNarratorStatus(NarratorStatus pStatus) {

    }

    public boolean isActive() {
        return false;
    }

    public void clear() {
    }

    public void destroy() {
    }

    public void checkStatus(boolean pNarratorEnabled) {
    }


}