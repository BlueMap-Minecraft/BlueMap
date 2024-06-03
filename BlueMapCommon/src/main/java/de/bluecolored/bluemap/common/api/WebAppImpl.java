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
package de.bluecolored.bluemap.common.api;

import de.bluecolored.bluemap.api.WebApp;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.FileHelper;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class WebAppImpl implements WebApp {

    private final BlueMapService blueMapService;
    private final @Nullable Plugin plugin;

    private final Timer timer = new Timer("BlueMap-WebbAppImpl-Timer", true);
    private @Nullable TimerTask scheduledWebAppSettingsUpdate;

    public WebAppImpl(BlueMapService blueMapService, @Nullable Plugin plugin) {
        this.blueMapService = blueMapService;
        this.plugin = plugin;
    }

    public WebAppImpl(Plugin plugin) {
        this.blueMapService = plugin.getBlueMap();
        this.plugin = plugin;
    }

    @Override
    public Path getWebRoot() {
        return blueMapService.getConfig().getWebappConfig().getWebroot();
    }

    @Override
    public void setPlayerVisibility(UUID player, boolean visible) {
        if (plugin == null) return; // fail silently: not supported on non-plugin platforms

        if (visible) {
            plugin.getPluginState().removeHiddenPlayer(player);
        } else {
            plugin.getPluginState().addHiddenPlayer(player);
        }
    }

    @Override
    public boolean getPlayerVisibility(UUID player) {
        if (plugin == null) return false; // fail silently: not supported on non-plugin platforms

        return !plugin.getPluginState().isPlayerHidden(player);
    }

    @Override
    public synchronized void registerScript(String url) {
        Logger.global.logDebug("Registering script from API: " + url);
        blueMapService.getWebFilesManager().getScripts().add(url);
        scheduleUpdateWebAppSettings();
    }

    @Override
    public synchronized void registerStyle(String url) {
        Logger.global.logDebug("Registering style from API: " + url);
        blueMapService.getWebFilesManager().getStyles().add(url);
        scheduleUpdateWebAppSettings();
    }

    /**
     * Save webapp-settings after a short delay, if no other save is already scheduled.
     * (to bulk-save changes in case there is a lot of scripts being registered at once)
     */
    private synchronized void scheduleUpdateWebAppSettings() {
        if (!blueMapService.getConfig().getWebappConfig().isEnabled()) return;
        if (scheduledWebAppSettingsUpdate != null) return;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (WebAppImpl.this) {
                    try {
                        if (blueMapService.getConfig().getWebappConfig().isEnabled())
                            blueMapService.getWebFilesManager().saveSettings();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to update webapp settings", ex);
                    } finally {
                        scheduledWebAppSettingsUpdate = null;
                    }
                }
            }
        }, 1000);
    }

    @Override
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public String createImage(BufferedImage image, String path) throws IOException {
        path = path.replaceAll("[^a-zA-Z0-9_.\\-/]", "_");

        Path webRoot = getWebRoot().toAbsolutePath();
        String separator = webRoot.getFileSystem().getSeparator();

        Path imageRootFolder = webRoot.resolve("data").resolve("images");
        Path imagePath = imageRootFolder.resolve(path.replace("/", separator) + ".png").toAbsolutePath();

        FileHelper.createDirectories(imagePath.getParent());
        Files.deleteIfExists(imagePath);
        Files.createFile(imagePath);

        if (!ImageIO.write(image, "png", imagePath.toFile()))
            throw new IOException("The format 'png' is not supported!");

        return webRoot.relativize(imagePath).toString().replace(separator, "/");
    }

    @Override
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    public Map<String, String> availableImages() throws IOException {
        Path webRoot = getWebRoot().toAbsolutePath();
        String separator = webRoot.getFileSystem().getSeparator();

        Path imageRootPath = webRoot.resolve("data").resolve("images").toAbsolutePath();

        Map<String, String> availableImagesMap = new HashMap<>();

        if (Files.exists(imageRootPath)) {
            try (Stream<Path> fileStream = Files.walk(imageRootPath)) {
                fileStream
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.getFileName().toString().endsWith(".png"))
                        .map(Path::toAbsolutePath)
                        .forEach(p -> {
                            try {
                                String key = imageRootPath.relativize(p).toString();
                                key = key
                                        .substring(0, key.length() - 4) //remove .png
                                        .replace(separator, "/");

                                String value = webRoot.relativize(p).toString()
                                        .replace(separator, "/");

                                availableImagesMap.put(key, value);
                            } catch (IllegalArgumentException ignore) {
                            }
                        });
            }
        }

        return availableImagesMap;
    }

}
