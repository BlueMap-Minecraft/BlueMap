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

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.Caches;
import de.bluecolored.bluemap.core.world.World;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BlueMapAPIImpl extends BlueMapAPI {

    private final BlueMapService blueMapService;
    private final @Nullable Plugin plugin;

    private final WebAppImpl webAppImpl;
    private final @Nullable RenderManagerImpl renderManagerImpl;
    private final @Nullable PluginImpl pluginImpl;

    private final LoadingCache<Object, Optional<BlueMapWorld>> worldCache;
    private final LoadingCache<String, Optional<BlueMapMap>> mapCache;

    public BlueMapAPIImpl(Plugin plugin) {
        this(plugin.getBlueMap(), plugin);
    }

    public BlueMapAPIImpl(BlueMapService blueMapService, @Nullable Plugin plugin) {
        this.blueMapService = blueMapService;
        this.plugin = plugin;

        this.renderManagerImpl = plugin != null ? new RenderManagerImpl(this, plugin) : null;
        this.webAppImpl = new WebAppImpl(blueMapService, plugin);
        this.pluginImpl = plugin != null ? new PluginImpl(plugin) : null;

        this.worldCache = Caches.with()
                .weakKeys()
                .build(this::getWorldUncached);
        this.mapCache = Caches.with()
                .weakKeys()
                .build(this::getMapUncached);
    }

    @Override
    public Collection<BlueMapMap> getMaps() {
        Map<String, BmMap> maps = blueMapService.getMaps();
        return maps.keySet().stream()
                .map(this::getMap)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<BlueMapWorld> getWorlds() {
        Map<String, World> worlds = blueMapService.getWorlds();
        return worlds.keySet().stream()
                .map(this::getWorld)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<BlueMapWorld> getWorld(@NonNull Object world) {
        return worldCache.get(world);
    }

    public Optional<BlueMapWorld> getWorldUncached(Object world) {

        if (world instanceof String) {
            var coreWorld = blueMapService.getWorlds().get(world);
            if (coreWorld != null) world = coreWorld;
        }

        if (world instanceof World coreWorld) {
            return Optional.of(new BlueMapWorldImpl(coreWorld, blueMapService, plugin));
        }

        if (plugin == null) return Optional.empty();

        ServerWorld serverWorld = plugin.getServerInterface().getServerWorld(world).orElse(null);
        if (serverWorld == null) return Optional.empty();

        World coreWorld = plugin.getWorld(serverWorld);
        if (coreWorld == null) return Optional.empty();

        return Optional.of(new BlueMapWorldImpl(coreWorld, blueMapService, plugin));

    }

    @Override
    public Optional<BlueMapMap> getMap(@NonNull String id) {
        return mapCache.get(id);
    }

    public Optional<BlueMapMap> getMapUncached(String id) {
        var maps = blueMapService.getMaps();

        var map = maps.get(id);
        if (map == null) return Optional.empty();

        var world = getWorld(map.getWorld()).orElse(null);
        if (world == null) return Optional.empty();

        return Optional.of(new BlueMapMapImpl(map, (BlueMapWorldImpl) world, plugin));
    }

    @Override
    public String getBlueMapVersion() {
        return BlueMap.VERSION;
    }

    @Override
    public WebAppImpl getWebApp() {
        return webAppImpl;
    }

    @Override
    public RenderManagerImpl getRenderManager() {
        if (renderManagerImpl == null) throw new UnsupportedOperationException("RenderManager API is not supported on this platform");
        return renderManagerImpl;
    }

    @Override
    public de.bluecolored.bluemap.api.plugin.Plugin getPlugin() {
        if (pluginImpl == null) throw new UnsupportedOperationException("Plugin API is not supported on this platform");
        return pluginImpl;
    }

    public void register() {
        try {
            BlueMapAPI.registerInstance(this);
        } catch (Exception ex) {
            Logger.global.logError("BlueMapAPI: A BlueMapAPI listener threw an exception (onEnable)!", ex);
        }
    }

    public void unregister() {
        try {
            BlueMapAPI.unregisterInstance(this);
        } catch (Exception ex) {
            Logger.global.logError("BlueMapAPI: A BlueMapAPI listener threw an exception (onDisable)!", ex);
        }
    }

    /**
     * Easy-access method for addons depending on BlueMapCommon:<br>
     * <blockquote><pre>
     *     BlueMapService bluemap = ((BlueMapAPIImpl) blueMapAPI).blueMapService();
     * </pre></blockquote>
     */
    @SuppressWarnings("unused")
    public BlueMapService blueMapService() {
        return blueMapService;
    }

    /**
     * Easy-access method for addons depending on BlueMapCommon:<br>
     * <blockquote><pre>
     *     Plugin plugin = ((BlueMapAPIImpl) blueMapAPI).plugin();
     * </pre></blockquote>
     */
    @SuppressWarnings("unused")
    public @Nullable Plugin plugin() {
        return plugin;
    }

}
