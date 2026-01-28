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
package de.bluecolored.bluemap.common;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.common.config.WebappConfig;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.util.FileHelper;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class WebFilesManager {

    private static final Gson GSON = ResourcesGson.addAdapter(new GsonBuilder())
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            //.setPrettyPrinting() // enable pretty printing for easy editing
            .create();

    private final Path webRoot;
    private Settings settings;

    public WebFilesManager(Path webRoot) {
        this.webRoot = webRoot;
        this.settings = new Settings();
    }

    public Path getSettingsFile() {
        return webRoot.resolve("settings.json");
    }

    public void loadSettings() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(getSettingsFile())) {
            this.settings = GSON.fromJson(reader, Settings.class);
        }
    }

    public void saveSettings() throws IOException {
        FileHelper.createDirectories(getSettingsFile().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(getSettingsFile(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(this.settings, writer);
        }
    }

    public void resetSettings() {
        this.settings = new Settings();
    }

    public void addMap(String mapId) {
        this.settings.maps.add(mapId);
    }

    public void removeMap(String mapId) {
        this.settings.maps.remove(mapId);
    }

    public Set<String> getScripts() {
        return this.settings.scripts;
    }

    public Set<String> getStyles() {
        return this.settings.styles;
    }

    public void setFrom(WebappConfig webappConfig) {
        this.settings.setFrom(webappConfig);
    }

    public void addFrom(WebappConfig webappConfig) {
        this.settings.addFrom(webappConfig);
    }

    public boolean filesNeedUpdate() {
        Path indexHtml = webRoot.resolve("index.html");

        // if there is no existing webapp, we definitely need to extract it
        if (!Files.exists(indexHtml)) return true;

        // compare the timestamp of the bundled webapp.zip with the existing index.html
        URL zippedWebapp = getClass().getResource("/de/bluecolored/bluemap/webapp.zip");
        if (zippedWebapp == null) return false; // no bundled webapp found, assume no update

        try {
            long zipLastModified = zippedWebapp.openConnection().getLastModified();
            if (zipLastModified <= 0) return false; // unknown timestamp, don't force update

            long indexLastModified = Files.getLastModifiedTime(indexHtml).toMillis();
            return indexLastModified < zipLastModified;
        } catch (IOException ex) {
            Logger.global.logDebug("Failed to compare webapp file versions", ex);
            return false;
        }
    }

    public void updateFiles() throws IOException {
        URL zippedWebapp = getClass().getResource("/de/bluecolored/bluemap/webapp.zip");
        if (zippedWebapp == null) throw new IOException("Failed to open bundled webapp.");

        // extract zip to webroot
        Files.createDirectories(webRoot);
        FileHelper.extractZipFile(zippedWebapp, webRoot, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused", "MismatchedQueryAndUpdateOfCollection"})
    private static class Settings {

        private String version = BlueMap.VERSION;

        private boolean useCookies = true;

        private boolean defaultToFlatView = false;

        private String startLocation = null;

        private float resolutionDefault = 1;

        private int minZoomDistance = 5;
        private int maxZoomDistance = 100000;

        private int hiresSliderMax = 500;
        private int hiresSliderDefault = 200;
        private int hiresSliderMin = 50;

        private int lowresSliderMax = 10000;
        private int lowresSliderDefault = 2000;
        private int lowresSliderMin = 500;

        private String mapDataRoot = "maps";
        private String liveDataRoot = "maps";

        private Set<String> maps = new HashSet<>();
        private Set<String> scripts = new HashSet<>();
        private Set<String> styles = new HashSet<>();

        public void setFrom(WebappConfig config) {
            this.useCookies = config.isUseCookies();
            this.defaultToFlatView = config.isDefaultToFlatView();
            this.startLocation = config.getStartLocation().orElse(null);
            this.resolutionDefault = config.getResolutionDefault();

            this.minZoomDistance = config.getMinZoomDistance();
            this.maxZoomDistance = config.getMaxZoomDistance();

            this.hiresSliderMax = config.getHiresSliderMax();
            this.hiresSliderDefault = config.getHiresSliderDefault();
            this.hiresSliderMin = config.getHiresSliderMin();

            this.lowresSliderMax = config.getLowresSliderMax();
            this.lowresSliderDefault = config.getLowresSliderDefault();
            this.lowresSliderMin = config.getLowresSliderMin();

            this.mapDataRoot = config.getMapDataRoot();
            this.liveDataRoot = config.getLiveDataRoot();

            this.styles.clear();
            this.scripts.clear();

            addFrom(config);
        }

        public void addFrom(WebappConfig config) {
            Set<String> scripts = config.getScripts();
            for (String script : scripts) {
                this.scripts.add(script);
                Logger.global.logDebug("Registering script from Webapp Config: " + script);
            }

            Set<String> styles = config.getStyles();
            for (String style : styles) {
                this.styles.add(style);
                Logger.global.logDebug("Registering style from Webapp Config: " + style);
            }
        }

    }

}
