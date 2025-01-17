package net.minecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;

public class ToggleKeyMapping extends KeyMapping {
    private final BooleanSupplier needsToggle;

    public ToggleKeyMapping(String pName, int pKeyCode, String pCategory, BooleanSupplier pNeedsToggle) {
        super(pName, InputConstants.Type.KEYSYM, pKeyCode, pCategory);
        this.needsToggle = pNeedsToggle;
    }

    @Override
    public void setDown(boolean pValue) {
        if (this.needsToggle.getAsBoolean()) {
            if (pValue) {
                super.setDown(!this.isDown());
            }
        } else {
            super.setDown(pValue);
        }
    }

    protected void reset() {
        super.setDown(false);
    }
}