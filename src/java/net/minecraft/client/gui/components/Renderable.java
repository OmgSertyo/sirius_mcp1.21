package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;

public interface Renderable {
    void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick);
}