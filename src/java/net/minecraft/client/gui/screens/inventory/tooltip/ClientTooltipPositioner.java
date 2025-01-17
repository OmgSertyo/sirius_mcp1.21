package net.minecraft.client.gui.screens.inventory.tooltip;

import org.joml.Vector2ic;

public interface ClientTooltipPositioner {
    Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight);
}