package com.mojang.blaze3d.platform;


public interface WindowEventHandler {
    void setWindowActive(boolean pWindowActive);

    void resizeDisplay();

    void cursorEntered();
}