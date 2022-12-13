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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.World;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BlueMapAPIImpl extends BlueMapAPI {

    private final Plugin plugin;
    private final LoadingCache<Object, Optional<BlueMapWorld>> worldCache;
    private final LoadingCache<String, Optional<BlueMapMap>> mapCache;

    public BlueMapAPIImpl(Plugin plugin) {
        this.plugin = plugin;
        this.worldCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .build(this::getWorldUncached);
        this.mapCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .build(this::getMapUncached);
    }

    @Override
    public RenderManagerImpl getRenderManager() {
        return new RenderManagerImpl(this, plugin);
    }

    @Override
    public WebAppImpl getWebApp() {
        return new WebAppImpl(plugin);
    }

    @Override
    public de.bluecolored.bluemap.api.plugin.Plugin getPlugin() {
        return new PluginImpl(plugin);
    }

    @Override
    public Collection<BlueMapMap> getMaps() {
        return plugin.getMaps().values().stream()
                .map(map -> {
                    try {
                        return new BlueMapMapImpl(plugin, map);
                    } catch (IOException e) {
                        Logger.global.logError("[API] Failed to create BlueMapMap for map " + map.getId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<BlueMapWorld> getWorlds() {
        return plugin.getWorlds().values().stream()
                .map(world -> getWorld(world).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<BlueMapWorld> getWorld(Object world) {
        return worldCache.get(world);
    }

    public Optional<BlueMapWorld> getWorldUncached(Object world) {

        if (world instanceof UUID) {
            var coreWorld = plugin.getWorlds().get(world.toString());
            if (coreWorld != null) world = coreWorld;
        }

        if (world instanceof String) {
            var coreWorld = plugin.getWorlds().get(world);
            if (coreWorld != null) world = coreWorld;
        }

        if (world instanceof World) {
            var coreWorld = (World) world;
            try {
                return Optional.of(new BlueMapWorldImpl(plugin, coreWorld));
            } catch (IOException e) {
                Logger.global.logError("[API] Failed to create BlueMapWorld for world " + coreWorld.getSaveFolder(), e);
            }
            return Optional.empty();
        }

        var serverWorld = plugin.getServerInterface().getWorld(world).orElse(null);
        if (serverWorld == null) return Optional.empty();

        try {
            String id = plugin.getBlueMap().getWorldId(serverWorld.getSaveFolder());
            var coreWorld = plugin.getWorlds().get(id);
            if (coreWorld == null) return Optional.empty();

            return Optional.of(new BlueMapWorldImpl(plugin, coreWorld));
        } catch (IOException e) {
            Logger.global.logError("[API] Failed to create BlueMapWorld for world " + serverWorld.getSaveFolder(), e);
            return Optional.empty();
        }

    }

    @Override
    public Optional<BlueMapMap> getMap(String id) {
        return mapCache.get(id);
    }

    public Optional<BlueMapMap> getMapUncached(String id) {
        var map = plugin.getMaps().get(id);
        if (map == null) return Optional.empty();

        var world = getWorld(map.getWorld()).orElse(null);
        if (world == null) return Optional.empty();

        return Optional.of(new BlueMapMapImpl(plugin, map, (BlueMapWorldImpl) world));
    }

    @Override
    public String getBlueMapVersion() {
        return BlueMap.VERSION;
    }

    public void register() {
        try {
            BlueMapAPI.registerInstance(this);
        } catch (ExecutionException ex) {
            Logger.global.logError("BlueMapAPI: A BlueMapAPI listener threw an exception (onEnable)!", ex.getCause());
        }
    }

    public void unregister() {
        try {
            BlueMapAPI.unregisterInstance(this);
        } catch (ExecutionException ex) {
            Logger.global.logError("BlueMapAPI: A BlueMapAPI listener threw an exception (onDisable)!", ex.getCause());
        }
    }

}
