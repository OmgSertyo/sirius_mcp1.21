package net.minecraft.client.resources.sounds;

import java.util.List;
import javax.annotation.Nullable;

public class SoundEventRegistration {
    private final List<Sound> sounds;
    private final boolean replace;
    @Nullable
    private final String subtitle;

    public SoundEventRegistration(List<Sound> pSounds, boolean pReplace, @Nullable String pSubtitle) {
        this.sounds = pSounds;
        this.replace = pReplace;
        this.subtitle = pSubtitle;
    }

    public List<Sound> getSounds() {
        return this.sounds;
    }

    public boolean isReplace() {
        return this.replace;
    }

    @Nullable
    public String getSubtitle() {
        return this.subtitle;
    }
}