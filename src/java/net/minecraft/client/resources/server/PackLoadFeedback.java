package net.minecraft.client.resources.server;

import java.util.UUID;

public interface PackLoadFeedback {
    void reportUpdate(UUID pId, PackLoadFeedback.Update pUpdate);

    void reportFinalResult(UUID pId, PackLoadFeedback.FinalResult pResult);

    public static enum FinalResult {
        DECLINED,
        APPLIED,
        DISCARDED,
        DOWNLOAD_FAILED,
        ACTIVATION_FAILED;
    }

    public static enum Update {
        ACCEPTED,
        DOWNLOADED;
    }
}