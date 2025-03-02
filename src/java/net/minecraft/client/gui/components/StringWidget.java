package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

public class StringWidget extends AbstractStringWidget {
    private float alignX = 0.5F;

    public StringWidget(Component pMessage, Font pFont) {
        this(0, 0, pFont.width(pMessage.getVisualOrderText()), 9, pMessage, pFont);
    }

    public StringWidget(int pWidth, int pHeight, Component pMessage, Font pFont) {
        this(0, 0, pWidth, pHeight, pMessage, pFont);
    }

    public StringWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage, Font pFont) {
        super(pX, pY, pWidth, pHeight, pMessage, pFont);
        this.active = false;
    }

    public StringWidget setColor(int pColor) {
        super.setColor(pColor);
        return this;
    }

    private StringWidget horizontalAlignment(float pHorizontalAlignment) {
        this.alignX = pHorizontalAlignment;
        return this;
    }

    public StringWidget alignLeft() {
        return this.horizontalAlignment(0.0F);
    }

    public StringWidget alignCenter() {
        return this.horizontalAlignment(0.5F);
    }

    public StringWidget alignRight() {
        return this.horizontalAlignment(1.0F);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        Component component = this.getMessage();
        Font font = this.getFont();
        int i = this.getWidth();
        int j = font.width(component);
        int k = this.getX() + Math.round(this.alignX * (float)(i - j));
        int l = this.getY() + (this.getHeight() - 9) / 2;
        FormattedCharSequence formattedcharsequence = j > i ? this.clipText(component, i) : component.getVisualOrderText();
        pGuiGraphics.drawString(font, formattedcharsequence, k, l, this.getColor());
    }

    private FormattedCharSequence clipText(Component pMessage, int pWidth) {
        Font font = this.getFont();
        FormattedText formattedtext = font.substrByWidth(pMessage, pWidth - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS));
    }
}