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
package de.bluecolored.bluemap.common.rendermanager;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.rendermanager.serialization.SerializableRenderTask;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public final class MapUpdateTask extends CombinedRenderTask implements MapRenderTask, SerializableRenderTask<MapUpdateTask, MapUpdateTask.Serialized> {

    @Getter private final BmMap map;

    public MapUpdateTask(BmMap map, Collection<Vector2i> regions, TileUpdateStrategy force) {
        this(map, Stream.concat(
                regions.stream().<RenderTask>map(region -> new WorldRegionUpdateTask(map, region, force)),
                Stream.of(new MapSaveTask(map))
        ).toList());
    }

    MapUpdateTask(BmMap map, Collection<RenderTask> tasks) {
        this(map, tasks, 0);
    }

    private MapUpdateTask(BmMap map, Collection<RenderTask> tasks, int currentTaskIndex) {
        super("updating map '%s'".formatted(map.getId()), tasks, currentTaskIndex);
        this.map = map;
    }

    @Override
    public Serialized serialize() {
        return new Serialized(map, getTasks(), getCurrentTaskIndex());
    }

    @AllArgsConstructor
    public static class Serialized implements SerializableRenderTask.Serialized<MapUpdateTask> {

        private BmMap map;
        private List<RenderTask> tasks;
        private int currentTaskIndex;

        public MapUpdateTask deserialize() {
            return new MapUpdateTask(map, tasks, currentTaskIndex);
        }

    }

}
