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
package de.bluecolored.bluemap.common.plugin;

import de.bluecolored.bluemap.core.map.BmMap;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
public class PluginState {

    private boolean renderThreadsEnabled = true;
    private Map<String, MapState> maps = new HashMap<>();

    public boolean isRenderThreadsEnabled() {
        return renderThreadsEnabled;
    }

    public void setRenderThreadsEnabled(boolean renderThreadsEnabled) {
        this.renderThreadsEnabled = renderThreadsEnabled;
    }

    public MapState getMapState(BmMap map) {
        return maps.computeIfAbsent(map.getId(), k -> new MapState());
    }

    @ConfigSerializable
    public static class MapState {

        private boolean updateEnabled = true;

        public boolean isUpdateEnabled() {
            return updateEnabled;
        }

        public void setUpdateEnabled(boolean update) {
            this.updateEnabled = update;
        }

    }

}
