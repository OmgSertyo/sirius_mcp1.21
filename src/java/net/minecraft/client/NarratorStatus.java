package net.minecraft.client;

import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum NarratorStatus {
    OFF(0, "options.narrator.off"),
    ALL(1, "options.narrator.off"),
    CHAT(2, "options.narrator.off"),
    SYSTEM(3, "options.narrator.off");

    private static final IntFunction<NarratorStatus> BY_ID = ByIdMap.continuous(NarratorStatus::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    private final int id;
    private final Component name;

    private NarratorStatus(final int pId, final String pName) {
        this.id = pId;
        this.name = Component.translatable(pName);
    }

    public int getId() {
        return this.id;
    }

    public Component getName() {
        return this.name;
    }

    public static NarratorStatus byId(int pId) {
        return BY_ID.apply(pId);
    }

    public boolean shouldNarrateChat() {
        return this == ALL || this == CHAT;
    }

    public boolean shouldNarrateSystem() {
        return this == ALL || this == SYSTEM;
    }
}