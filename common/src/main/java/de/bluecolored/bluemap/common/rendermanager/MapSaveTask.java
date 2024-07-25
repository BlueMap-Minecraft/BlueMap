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

import de.bluecolored.bluemap.core.map.BmMap;

import java.util.concurrent.atomic.AtomicBoolean;

public class MapSaveTask implements RenderTask {

    private final BmMap map;
    private final AtomicBoolean saved;

    public MapSaveTask(BmMap map) {
        this.map = map;
        this.saved = new AtomicBoolean(false);
    }

    @Override
    public void doWork() {
        if (this.saved.compareAndSet(false, true)) {
            map.save();
        }
    }

    @Override
    public boolean hasMoreWork() {
        return !this.saved.get();
    }

    @Override
    public void cancel() {
        this.saved.set(true);
    }

    @Override
    public String getDescription() {
        return "Save map '" + map.getId() + "'";
    }

    @Override
    public boolean contains(RenderTask task) {
        if (this == task) return true;
        if (task == null) return false;
        if (getClass() != task.getClass()) return false;
        MapSaveTask other = (MapSaveTask) task;
        return map.getId().equals(other.map.getId());
    }

}
