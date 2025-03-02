package net.minecraft.client;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.OptionEnum;

public enum PrioritizeChunkUpdates implements OptionEnum {
    NONE(0, "options.prioritizeChunkUpdates.none"),
    PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
    NEARBY(2, "options.prioritizeChunkUpdates.nearby");

    private static final IntFunction<PrioritizeChunkUpdates> BY_ID = ByIdMap.continuous(
        PrioritizeChunkUpdates::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP
    );
    private final int id;
    private final String key;

    private PrioritizeChunkUpdates(final int pId, final String pKey) {
        this.id = pId;
        this.key = pKey;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public static PrioritizeChunkUpdates byId(int pId) {
        return BY_ID.apply(pId);
    }
}