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


import de.bluecolored.bluemap.api.plugin.PlayerIconFactory;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import de.bluecolored.bluemap.common.plugin.Plugin;
import lombok.NonNull;

public class PluginImpl implements de.bluecolored.bluemap.api.plugin.Plugin {

    private final Plugin plugin;

    public PluginImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SkinProvider getSkinProvider() {
        return plugin.getSkinUpdater().getSkinProvider();
    }

    @Override
    public void setSkinProvider(@NonNull SkinProvider skinProvider) {
        plugin.getSkinUpdater().setSkinProvider(skinProvider);
    }

    @Override
    public PlayerIconFactory getPlayerMarkerIconFactory() {
        return plugin.getSkinUpdater().getPlayerMarkerIconFactory();
    }

    @Override
    public void setPlayerMarkerIconFactory(PlayerIconFactory playerMarkerIconFactory) {
        plugin.getSkinUpdater().setPlayerMarkerIconFactory(playerMarkerIconFactory);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
