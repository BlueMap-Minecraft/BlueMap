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
package de.bluecolored.bluemap.common.live;

import de.bluecolored.bluemap.api.plugin.PlayerDisplayNameProvider;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.PluginState;
import de.bluecolored.bluemap.common.serverinterface.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class PluginLivePlayerInfoTransformer implements LivePlayerInfoTransformer {

    private final Plugin plugin;

    @Getter @Setter @NonNull
    private PlayerDisplayNameProvider playerDisplayNameProvider = new DefaultDisplayNameProvider();

    public PluginLivePlayerInfoTransformer(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public LivePlayerInfo apply(Player player) {
        PluginState pluginState = plugin.getPluginState();
        if (pluginState.isPlayerHidden(player.getUuid())) return null;

        BlueMapService blueMapService = plugin.getBlueMap();
        if (blueMapService == null) return null;

        PluginConfig config = blueMapService.getConfig().getPluginConfig();
        if (config.isHideInvisible() && player.isInvisible()) return null;
        if (config.isHideVanished() && player.isVanished()) return null;
        if (config.isHideSneaking() && player.isSneaking()) return null;
        if (config.getHiddenGameModes().contains(player.getGamemode().getId())) return null;
        if (player.getSkyLight() < config.getHideBelowSkyLight() &&
                player.getBlockLight() < config.getHideBelowBlockLight()) return null;

        return new LivePlayerInfo(
                player.getUuid(),
                playerDisplayNameProvider.get(player.getUuid()),
                player.getPosition(),
                player.getRotation()
        );
    }

    private class DefaultDisplayNameProvider implements PlayerDisplayNameProvider {

        @Override
        public String get(UUID playerUUID) {
            Map<UUID, Player> onlinePlayerMap = plugin.getServerInterface().getOnlinePlayers();
            @Nullable Player player = onlinePlayerMap.get(playerUUID);
            if (player != null) return player.getName().toPlainString();
            return playerUUID.toString();
        }

    }

}
