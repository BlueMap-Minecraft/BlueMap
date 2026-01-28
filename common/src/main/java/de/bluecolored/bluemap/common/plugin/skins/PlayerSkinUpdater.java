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
package de.bluecolored.bluemap.common.plugin.skins;

import de.bluecolored.bluemap.api.plugin.PlayerIconFactory;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerSkinUpdater implements ServerEventListener {

    private final Plugin plugin;
    private final Map<UUID, Long> skinUpdates;

    private SkinProvider skinProvider;
    private PlayerIconFactory playerMarkerIconFactory;

    public PlayerSkinUpdater(Plugin plugin) {
        this.plugin = plugin;
        this.skinUpdates = new ConcurrentHashMap<>();

        skinProvider = new MojangSkinProvider();
        playerMarkerIconFactory = new DefaultPlayerIconFactory();
    }

    public CompletableFuture<Void> updateSkin(final UUID playerUuid) {

        // only update if last update was longer then an hour ago
        long lastUpdate = skinUpdates.getOrDefault(playerUuid, 0L);
        long now = System.currentTimeMillis();
        if (now - lastUpdate < TimeUnit.HOURS.toMillis(1)) return CompletableFuture.completedFuture(null);
        skinUpdates.put(playerUuid, now);

        // do the update async
        return CompletableFuture.supplyAsync(() -> {
            try {
                return skinProvider.load(playerUuid);
            } catch (IOException e) {
                throw new CompletionException("The skin provider threw an exception while loading the skin for UUID: '" + playerUuid + "'!", e);
            }
        }, BlueMap.THREAD_POOL).thenAcceptAsync(skin -> {
            if (skin == null) {
                Logger.global.logDebug("No player-skin provided for UUID: " + playerUuid);
                return;
            }

            Map<String, BmMap> maps = plugin.getBlueMap().getMaps();
            if (maps == null) {
                Logger.global.logDebug("Could not update skin, since the plugin seems not to be ready.");
                return;
            }

            if (skin.isEmpty()) {
                Logger.global.logDebug("Empty player-skin provided for UUID: " + playerUuid);
                for (BmMap map : maps.values()) {
                    try {
                        map.getStorage().asset("playerheads/" + playerUuid + ".png").delete();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to remove player skin from storage: " + playerUuid, ex);
                    }
                }
                return;
            }

            BufferedImage playerHead = playerMarkerIconFactory.apply(playerUuid, skin.get());

            for (BmMap map : maps.values()) {
                try (OutputStream out = map.getStorage().asset("playerheads/" + playerUuid + ".png").write()) {
                    ImageIO.write(playerHead, "png", out);
                } catch (IOException ex) {
                    Logger.global.logError("Failed to write player skin to storage: " + playerUuid, ex);
                }
            }
        }, BlueMap.THREAD_POOL);
    }

    @Override
    public void onPlayerJoin(UUID playerUuid) {
        updateSkin(playerUuid).exceptionally(ex -> {
            Logger.global.logError("Failed to update player skin: " + playerUuid, ex);
            return null;
        });
    }

    public SkinProvider getSkinProvider() {
        return skinProvider;
    }

    public void setSkinProvider(SkinProvider skinProvider) {
        this.skinProvider = Objects.requireNonNull(skinProvider, "skinProvider can not be null");
    }

    public PlayerIconFactory getPlayerMarkerIconFactory() {
        return playerMarkerIconFactory;
    }

    public void setPlayerMarkerIconFactory(PlayerIconFactory playerMarkerIconFactory) {
        this.playerMarkerIconFactory = Objects.requireNonNull(playerMarkerIconFactory, "playerMarkerIconFactory can not be null");
    }

}
