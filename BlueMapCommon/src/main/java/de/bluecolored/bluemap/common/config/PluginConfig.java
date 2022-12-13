/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.api.debug.DebugDump;
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
    private int hideBelowSkyLight = 0;
    private int hideBelowBlockLight = 0;

    private int writeMarkersInterval = 0;
    private int writePlayersInterval = 0;

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

    public int getHideBelowSkyLight() {
        return hideBelowSkyLight;
    }

    public int getHideBelowBlockLight() {
        return hideBelowBlockLight;
    }

    public int getWriteMarkersInterval() {
        return writeMarkersInterval;
    }

    public int getWritePlayersInterval() {
        return writePlayersInterval;
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
