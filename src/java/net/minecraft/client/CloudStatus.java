package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.util.OptionEnum;
import net.minecraft.util.StringRepresentable;

public enum CloudStatus implements OptionEnum, StringRepresentable {
    OFF(0, "false", "options.off"),
    FAST(1, "fast", "options.clouds.fast"),
    FANCY(2, "true", "options.clouds.fancy");

    public static final Codec<CloudStatus> CODEC = StringRepresentable.fromEnum(CloudStatus::values);
    private final int id;
    private final String legacyName;
    private final String key;

    private CloudStatus(final int pId, final String pLegacyName, final String pKey) {
        this.id = pId;
        this.legacyName = pLegacyName;
        this.key = pKey;
    }

    @Override
    public String getSerializedName() {
        return this.legacyName;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getKey() {
        return this.key;
    }
}