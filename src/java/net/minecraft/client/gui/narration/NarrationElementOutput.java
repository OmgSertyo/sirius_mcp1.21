package net.minecraft.client.gui.narration;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;

public interface NarrationElementOutput {
    default void add(NarratedElementType pType, Component pContents) {
        this.add(pType, NarrationThunk.from(pContents.getString()));
    }

    default void add(NarratedElementType pType, String pContents) {
        this.add(pType, NarrationThunk.from(pContents));
    }

    default void add(NarratedElementType pType, Component... pContents) {
        this.add(pType, NarrationThunk.from(ImmutableList.copyOf(pContents)));
    }

    void add(NarratedElementType pType, NarrationThunk<?> pContents);

    NarrationElementOutput nest();
}