package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;

public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
    private final SocialInteractionsScreen socialInteractionsScreen;
    private final List<PlayerEntry> players = Lists.newArrayList();
    @Nullable
    private String filter;

    public SocialInteractionsPlayerList(SocialInteractionsScreen pSocialInteractionsScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
        this.socialInteractionsScreen = pSocialInteractionsScreen;
    }

    @Override
    protected void renderListBackground(GuiGraphics pGuiGraphics) {
    }

    @Override
    protected void renderListSeparators(GuiGraphics pGuiGraphics) {
    }

    @Override
    protected void enableScissor(GuiGraphics pGuiGraphics) {
        pGuiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
    }

    public void updatePlayerList(Collection<UUID> pIds, double pScrollAmount, boolean pAddChatLogPlayers) {
        Map<UUID, PlayerEntry> map = new HashMap<>();
        this.addOnlinePlayers(pIds, map);
        this.updatePlayersFromChatLog(map, pAddChatLogPlayers);
        this.updateFiltersAndScroll(map.values(), pScrollAmount);
    }

    private void addOnlinePlayers(Collection<UUID> pIds, Map<UUID, PlayerEntry> pPlayerMap) {
        ClientPacketListener clientpacketlistener = this.minecraft.player.connection;

        for (UUID uuid : pIds) {
            PlayerInfo playerinfo = clientpacketlistener.getPlayerInfo(uuid);
            if (playerinfo != null) {
                boolean flag = playerinfo.hasVerifiableChat();
                pPlayerMap.put(uuid, new PlayerEntry(this.minecraft, this.socialInteractionsScreen, uuid, playerinfo.getProfile().getName(), playerinfo::getSkin, flag));
            }
        }
    }

    private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> pPlayerMap, boolean pAddPlayers) {
        for (GameProfile gameprofile : collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog())) {
            PlayerEntry playerentry;
            if (pAddPlayers) {
                playerentry = pPlayerMap.computeIfAbsent(
                    gameprofile.getId(),
                    p_296219_ -> {
                        PlayerEntry playerentry1 = new PlayerEntry(
                            this.minecraft, this.socialInteractionsScreen, gameprofile.getId(), gameprofile.getName(), this.minecraft.getSkinManager().lookupInsecure(gameprofile), true
                        );
                        playerentry1.setRemoved(true);
                        return playerentry1;
                    }
                );
            } else {
                playerentry = pPlayerMap.get(gameprofile.getId());
                if (playerentry == null) {
                    continue;
                }
            }

            playerentry.setHasRecentMessages(true);
        }
    }

    private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog pChatLog) {
        Set<GameProfile> set = new ObjectLinkedOpenHashSet<>();

        for (int i = pChatLog.end(); i >= pChatLog.start(); i--) {
            LoggedChatEvent loggedchatevent = pChatLog.lookup(i);
            if (loggedchatevent instanceof LoggedChatMessage.Player) {
                LoggedChatMessage.Player loggedchatmessage$player = (LoggedChatMessage.Player)loggedchatevent;
                if (loggedchatmessage$player.message().hasSignature()) {
                    set.add(loggedchatmessage$player.profile());
                }
            }
        }

        return set;
    }

    private void sortPlayerEntries() {
        this.players.sort(Comparator.<PlayerEntry, Integer>comparing(p_240745_ -> {
            if (this.minecraft.isLocalPlayer(p_240745_.getPlayerId())) {
                return 0;
            } else if (this.minecraft.getReportingContext().hasDraftReportFor(p_240745_.getPlayerId())) {
                return 1;
            } else if (p_240745_.getPlayerId().version() == 2) {
                return 4;
            } else {
                return p_240745_.hasRecentMessages() ? 2 : 3;
            }
        }).thenComparing(p_240744_ -> {
            if (!p_240744_.getPlayerName().isBlank()) {
                int i = p_240744_.getPlayerName().codePointAt(0);
                if (i == 95 || i >= 97 && i <= 122 || i >= 65 && i <= 90 || i >= 48 && i <= 57) {
                    return 0;
                }
            }

            return 1;
        }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
    }

    private void updateFiltersAndScroll(Collection<PlayerEntry> pPlayers, double pScrollAmount) {
        this.players.clear();
        this.players.addAll(pPlayers);
        this.sortPlayerEntries();
        this.updateFilteredPlayers();
        this.replaceEntries(this.players);
        this.setScrollAmount(pScrollAmount);
    }

    private void updateFilteredPlayers() {
        if (this.filter != null) {
            this.players.removeIf(p_100710_ -> !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
            this.replaceEntries(this.players);
        }
    }

    public void setFilter(String pFilter) {
        this.filter = pFilter;
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public void addPlayer(PlayerInfo pPlayerInfo, SocialInteractionsScreen.Page pPage) {
        UUID uuid = pPlayerInfo.getProfile().getId();

        for (PlayerEntry playerentry : this.players) {
            if (playerentry.getPlayerId().equals(uuid)) {
                playerentry.setRemoved(false);
                return;
            }
        }

        if ((pPage == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uuid))
            && (Strings.isNullOrEmpty(this.filter) || pPlayerInfo.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
            boolean flag = pPlayerInfo.hasVerifiableChat();
            PlayerEntry playerentry1 = new PlayerEntry(
                this.minecraft, this.socialInteractionsScreen, pPlayerInfo.getProfile().getId(), pPlayerInfo.getProfile().getName(), pPlayerInfo::getSkin, flag
            );
            this.addEntry(playerentry1);
            this.players.add(playerentry1);
        }
    }

    public void removePlayer(UUID pId) {
        for (PlayerEntry playerentry : this.players) {
            if (playerentry.getPlayerId().equals(pId)) {
                playerentry.setRemoved(true);
                return;
            }
        }
    }
}