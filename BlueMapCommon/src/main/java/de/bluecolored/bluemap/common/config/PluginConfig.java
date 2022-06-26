package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class PluginConfig {

    private boolean livePlayerMarkers = true;

    private List<String> hiddenGameModes = new ArrayList<>();
    private boolean hideVanished = true;
    private boolean hideInvisible = true;
    private boolean hideSneaking = false;
    private boolean hideDifferentWorld = false;

    private boolean skinDownload = true;

    private int playerRenderLimit = -1;

    private int fullUpdateInterval = 1440;

    public boolean isLivePlayerMarkers() {
        return livePlayerMarkers;
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

    public boolean isHideDifferentWorld() {
        return hideDifferentWorld;
    }

    public boolean isSkinDownload() {
        return skinDownload;
    }

    public int getPlayerRenderLimit() {
        return playerRenderLimit;
    }

    public int getFullUpdateInterval() {
        return fullUpdateInterval;
    }

}
