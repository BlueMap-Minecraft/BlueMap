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

import de.bluecolored.bluemap.common.config.WebappConfig;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class WebFilesManager {

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
            this.settings = ResourcesGson.INSTANCE.fromJson(reader, Settings.class);
        }
    }

    public void saveSettings() throws IOException {
        Files.createDirectories(getSettingsFile().getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(getSettingsFile(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ResourcesGson.INSTANCE.toJson(this.settings, writer);
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

    public void setFrom(WebappConfig webappConfig) {
        this.settings.setFrom(webappConfig);
    }

    public boolean filesNeedUpdate() {
        return !Files.exists(webRoot.resolve("index.html"));
    }

    public void updateFiles() throws IOException {
        URL fileResource = getClass().getResource("/de/bluecolored/bluemap/webapp.zip");
        File tempFile = File.createTempFile("bluemap_webroot_extraction", null);

        if (fileResource == null) throw new IOException("Failed to open bundled webapp.");

        try {
            FileUtils.copyURLToFile(fileResource, tempFile, 10000, 10000);
            try (ZipFile zipFile = new ZipFile(tempFile)){
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry.isDirectory()) {
                        File dir = webRoot.resolve(zipEntry.getName()).toFile();
                        FileUtils.forceMkdir(dir);
                    } else {
                        File target = webRoot.resolve(zipEntry.getName()).toFile();
                        FileUtils.forceMkdirParent(target);
                        FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), target);
                    }
                }
            }
        } finally {
            if (!tempFile.delete()) {
                Logger.global.logWarning("Failed to delete file: " + tempFile);
            }
        }
    }

    @SuppressWarnings("all")
    private static class Settings {

        private boolean useCookies = true;

        private boolean enableFreeFlight = true;

        private String startLocation = null;

        private float resolutionDefault = 1;

        private int hiresSliderMax = 500;
        private int hiresSliderDefault = 200;
        private int hiresSliderMin = 50;

        private int lowresSliderMax = 10000;
        private int lowresSliderDefault = 2000;
        private int lowresSliderMin = 500;

        private Set<String> maps = new HashSet<>();

        public void setFrom(WebappConfig config) {
            this.useCookies = config.isUseCookies();
            this.enableFreeFlight = config.isEnableFreeFlight();
            this.startLocation = config.getStartLocation().orElse(null);
            this.resolutionDefault = config.getResolutionDefault();
            
            this.hiresSliderMax = config.getHiresSliderMax();
            this.hiresSliderDefault = config.getLowresSliderDefault();
            this.hiresSliderMin = config.getHiresSliderMin();
            
            this.lowresSliderMax = config.getLowresSliderMax();
            this.lowresSliderDefault = config.getLowresSliderDefault();
            this.lowresSliderMin = config.getLowresSliderMin();
        }
        
    }

}
