package net.minecraft.network.protocol;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.Level;
import net.optifine.util.PacketRunnable;
import org.slf4j.Logger;

public class PacketUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static ResourceKey<Level> lastDimensionType = null;

    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> pPacket, T pProcessor, ServerLevel pLevel) throws RunningOnDifferentThreadException {
        ensureRunningOnSameThread(pPacket, pProcessor, pLevel.getServer());
    }

    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> pPacket, T pProcessor, BlockableEventLoop<?> pExecutor) throws RunningOnDifferentThreadException {
        if (!pExecutor.isSameThread()) {
            pExecutor.executeIfPossible(new PacketRunnable(pPacket, () -> {
                clientPreProcessPacket(pPacket);
                if (pProcessor.shouldHandleMessage(pPacket)) {
                    try {
                        pPacket.handle(pProcessor);
                    } catch (Exception exception) {
                        if (exception instanceof ReportedException reportedexception && reportedexception.getCause() instanceof OutOfMemoryError) {
                            throw makeReportedException(exception, pPacket, pProcessor);
                        }

                        pProcessor.onPacketError(pPacket, exception);
                    }
                } else {
                    LOGGER.debug("Ignoring packet due to disconnection: {}", pPacket);
                }
            }));
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        } else {
            clientPreProcessPacket(pPacket);
        }
    }

    protected static void clientPreProcessPacket(Packet packetIn) {
        if (packetIn instanceof ClientboundPlayerPositionPacket) {
            Minecraft.getInstance().levelRenderer.onPlayerPositionSet();
        }

        if (packetIn instanceof ClientboundRespawnPacket clientboundrespawnpacket) {
            lastDimensionType = clientboundrespawnpacket.commonPlayerSpawnInfo().dimension();
        } else if (packetIn instanceof ClientboundLoginPacket clientboundloginpacket) {
            lastDimensionType = clientboundloginpacket.commonPlayerSpawnInfo().dimension();
        } else {
            lastDimensionType = null;
        }
    }

    public static <T extends PacketListener> ReportedException makeReportedException(Exception pException, Packet<T> pPacket, T pPacketListener) {
        if (pException instanceof ReportedException reportedexception) {
            fillCrashReport(reportedexception.getReport(), pPacketListener, pPacket);
            return reportedexception;
        } else {
            CrashReport crashreport = CrashReport.forThrowable(pException, "Main thread packet handler");
            fillCrashReport(crashreport, pPacketListener, pPacket);
            return new ReportedException(crashreport);
        }
    }

    public static <T extends PacketListener> void fillCrashReport(CrashReport pCrashReport, T pPacketListener, @Nullable Packet<T> pPacket) {
        if (pPacket != null) {
            CrashReportCategory crashreportcategory = pCrashReport.addCategory("Incoming Packet");
            crashreportcategory.setDetail("Type", () -> pPacket.type().toString());
            crashreportcategory.setDetail("Is Terminal", () -> Boolean.toString(pPacket.isTerminal()));
            crashreportcategory.setDetail("Is Skippable", () -> Boolean.toString(pPacket.isSkippable()));
        }

        pPacketListener.fillCrashReport(pCrashReport);
    }
}