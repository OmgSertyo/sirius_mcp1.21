package com.mojang.realmsclient;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class RealmsGenericErrorScreen extends Screen{
    private final Screen nextScreen;
    private final RealmsGenericErrorScreen.ErrorMessage lines;
    private MultiLineLabel line2Split = MultiLineLabel.EMPTY;

    public RealmsGenericErrorScreen(Component pMessage, Screen pNextScreen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = pNextScreen;
        this.lines = errorMessage(pMessage);
    }

    public RealmsGenericErrorScreen(Component pTitle, Component pLine2, Screen pMessage) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = pMessage;
        this.lines = errorMessage(pTitle, pLine2);
    }


    private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component pMessage) {
        return errorMessage(Component.translatable("mco.errorMessage.generic"), pMessage);
    }

    private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component pTitle, Component pMessage) {
        return new RealmsGenericErrorScreen.ErrorMessage(pTitle, pMessage);
    }

    @Override
    public void init() {
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_OK, p_325126_ -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 52, 200, 20)
                .build()
        );
        this.line2Split = MultiLineLabel.create(this.font, this.lines.detail, this.width * 3 / 4);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.nextScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(this.lines.title).append(": ").append(this.lines.detail);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.lines.title, this.width / 2, 80, -1);
        this.line2Split.renderCentered(pGuiGraphics, this.width / 2, 100, 9, -2142128);
    }

    record ErrorMessage(Component title, Component detail) {
    }
}