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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
public class WebappConfig {

    private boolean enabled = true;
    private boolean updateSettingsFile = true;

    private Path webroot = Path.of("bluemap", "web");

    private boolean useCookies = true;

    private boolean enableFreeFlight = true;
    private boolean defaultToFlatView = false;

    private String startLocation = null;

    private float resolutionDefault = 1;

    private int minZoomDistance = 5;
    private int maxZoomDistance = 100000;

    private int hiresSliderMax = 500;
    private int hiresSliderDefault = 100;
    private int hiresSliderMin = 0;

    private int lowresSliderMax = 7000;
    private int lowresSliderDefault = 2000;
    private int lowresSliderMin = 500;

    private String mapDataRoot = "maps/";
    private String liveDataRoot = "maps/";

    private Set<String> scripts = new HashSet<>();
    private Set<String> styles = new HashSet<>();
    public boolean isEnabled() {
        return enabled;
    }

    public Path getWebroot() {
        return webroot;
    }

    public boolean isUpdateSettingsFile() {
        return updateSettingsFile;
    }

    public boolean isUseCookies() {
        return useCookies;
    }

    public boolean isEnableFreeFlight() {
        return enableFreeFlight;
    }

    public boolean isDefaultToFlatView() {
        return defaultToFlatView;
    }

    public Optional<String> getStartLocation() {
        return Optional.ofNullable(startLocation);
    }

    public float getResolutionDefault() {
        return resolutionDefault;
    }

    public int getMinZoomDistance() {
        return minZoomDistance;
    }

    public int getMaxZoomDistance() {
        return maxZoomDistance;
    }

    public int getHiresSliderMax() {
        return hiresSliderMax;
    }

    public int getHiresSliderDefault() {
        return hiresSliderDefault;
    }

    public int getHiresSliderMin() {
        return hiresSliderMin;
    }

    public int getLowresSliderMax() {
        return lowresSliderMax;
    }

    public int getLowresSliderDefault() {
        return lowresSliderDefault;
    }

    public int getLowresSliderMin() {
        return lowresSliderMin;
    }

    public String getMapDataRoot() {
        return mapDataRoot;
    }

    public String getLiveDataRoot() {
        return liveDataRoot;
    }

    public Set<String> getScripts() {
        return scripts;
    }

    public Set<String> getStyles() {
        return styles;
    }

}
