package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class DirectJoinServerScreen extends Screen {
    private static final Component ENTER_IP_LABEL = Component.translatable("addServer.enterIp");
    private Button selectButton;
    private final ServerData serverData;
    private EditBox ipEdit;
    private final BooleanConsumer callback;
    private final Screen lastScreen;

    public DirectJoinServerScreen(Screen pLastScreen, BooleanConsumer pCallback, ServerData pServerData) {
        super(Component.translatable("selectServer.direct"));
        this.lastScreen = pLastScreen;
        this.serverData = pServerData;
        this.callback = pCallback;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (!this.selectButton.active || this.getFocused() != this.ipEdit || pKeyCode != 257 && pKeyCode != 335) {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        } else {
            this.onSelect();
            return true;
        }
    }

    @Override
    protected void init() {
        this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 116, 200, 20, Component.translatable("addServer.enterIp"));
        this.ipEdit.setMaxLength(128);
        this.ipEdit.setValue(this.minecraft.options.lastMpIp);
        this.ipEdit.setResponder(p_95983_ -> this.updateSelectButtonStatus());
        this.addWidget(this.ipEdit);
        this.selectButton = this.addRenderableWidget(
            Button.builder(Component.translatable("selectServer.select"), p_95981_ -> this.onSelect())
                .bounds(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_95977_ -> this.callback.accept(false))
                .bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
                .build()
        );
        this.updateSelectButtonStatus();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.ipEdit);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.ipEdit.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.ipEdit.setValue(s);
    }

    private void onSelect() {
        this.serverData.ip = this.ipEdit.getValue();
        this.callback.accept(true);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void removed() {
        this.minecraft.options.lastMpIp = this.ipEdit.getValue();
        this.minecraft.options.save();
    }

    private void updateSelectButtonStatus() {
        this.selectButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue());
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        pGuiGraphics.drawString(this.font, ENTER_IP_LABEL, this.width / 2 - 100 + 1, 100, 10526880);
        this.ipEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}