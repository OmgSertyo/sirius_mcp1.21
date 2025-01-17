package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest.ThirdPartyServerInfo;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;

public record ReportEnvironment(String clientVersion, @Nullable ReportEnvironment.Server server) {
    public static ReportEnvironment local() {
        return create(null);
    }

    public static ReportEnvironment thirdParty(String pIp) {
        return create(new ReportEnvironment.Server.ThirdParty(pIp));
    }

    public static ReportEnvironment create(@Nullable ReportEnvironment.Server pServer) {
        return new ReportEnvironment(getClientVersion(), pServer);
    }

    public ClientInfo clientInfo() {
        return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
    }

    @Nullable
    public ThirdPartyServerInfo thirdPartyServerInfo() {
        return this.server instanceof ReportEnvironment.Server.ThirdParty reportenvironment$server$thirdparty
            ? new ThirdPartyServerInfo(reportenvironment$server$thirdparty.ip)
            : null;
    }

    private static String getClientVersion() {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append("1.21.1");

        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            stringbuilder.append(" (modded)");
        }

        return stringbuilder.toString();
    }

    public interface Server {
        record ThirdParty(String ip) implements ReportEnvironment.Server {
        }
    }
}