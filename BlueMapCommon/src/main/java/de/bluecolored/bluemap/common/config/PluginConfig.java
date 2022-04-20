package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class PluginConfig {

    private boolean livePlayerMarkers = true;

    private boolean skinDownload = true;

    private List<String> hiddenGameModes = new ArrayList<>();
    private boolean hideVanished = true;
    private boolean hideInvisible = true;
    private boolean hideSneaking = true;

    private int playerRenderLimit = -1;

    private int fullUpdateInterval = 1440;

    public boolean isLivePlayerMarkers() {
        return livePlayerMarkers;
    }

    public boolean isSkinDownload() {
        return skinDownload;
    }

    public List<String> getHiddenGameModes() {
        return hiddenGameModes;
    }

    public boolean isHideVanished() {
        return hideVanished;
    }

    public boolean isHideInvisible() {
        return hideInvisible;
    }

    public boolean isHideSneaking() {
        return hideSneaking;
    }

    public int getPlayerRenderLimit() {
        return playerRenderLimit;
    }

    public int getFullUpdateInterval() {
        return fullUpdateInterval;
    }

}
