package net.minecraft.client.main;

import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.DisplayData;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.util.StringUtil;

public class GameConfig {
    public final GameConfig.UserData user;
    public final DisplayData display;
    public final GameConfig.FolderData location;
    public final GameConfig.GameData game;
    public final GameConfig.QuickPlayData quickPlay;

    public GameConfig(
        GameConfig.UserData pUser,
        DisplayData pDisplay,
        GameConfig.FolderData pLocation,
        GameConfig.GameData pGame,
        GameConfig.QuickPlayData pQuickPlay
    ) {
        this.user = pUser;
        this.display = pDisplay;
        this.location = pLocation;
        this.game = pGame;
        this.quickPlay = pQuickPlay;
    }

    public static class FolderData {
        public final File gameDirectory;
        public final File resourcePackDirectory;
        public final File assetDirectory;
        @Nullable
        public final String assetIndex;

        public FolderData(File pGameDirectory, File pResourcePackDirectory, File pAssetDirectory, @Nullable String pAssetIndex) {
            this.gameDirectory = pGameDirectory;
            this.resourcePackDirectory = pResourcePackDirectory;
            this.assetDirectory = pAssetDirectory;
            this.assetIndex = pAssetIndex;
        }

        public Path getExternalAssetSource() {
            return this.assetIndex == null ? this.assetDirectory.toPath() : IndexedAssetSource.createIndexFs(this.assetDirectory.toPath(), this.assetIndex);
        }
    }

    public static class GameData {
        public final String launchVersion;
        public final String versionType;

        public GameData(String pLaunchVersion, String pVersionType) {
            this.launchVersion = pLaunchVersion;
            this.versionType = pVersionType;
        }
    }

    public record QuickPlayData(@Nullable String path, @Nullable String singleplayer, @Nullable String multiplayer) {
        public boolean isEnabled() {
            return !StringUtil.isBlank(this.singleplayer) || !StringUtil.isBlank(this.multiplayer);
        }
    }

    public static class UserData {
        public final User user;
        public final PropertyMap userProperties;
        public final PropertyMap profileProperties;
        public final Proxy proxy;

        public UserData(User pUser, PropertyMap pUserProperties, PropertyMap pProfileProperties, Proxy pProxy) {
            this.user = pUser;
            this.userProperties = pUserProperties;
            this.profileProperties = pProfileProperties;
            this.proxy = pProxy;
        }
    }
}