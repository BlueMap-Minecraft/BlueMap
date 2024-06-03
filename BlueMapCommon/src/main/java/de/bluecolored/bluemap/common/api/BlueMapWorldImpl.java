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

import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlueMapWorldImpl implements BlueMapWorld {

    private final String id;
    private final WeakReference<World> world;
    private final WeakReference<BlueMapService> blueMapService;
    private final WeakReference<Plugin> plugin;

    public BlueMapWorldImpl(World world, BlueMapService blueMapService, @Nullable Plugin plugin) {
        this.id = world.getId();
        this.world = new WeakReference<>(world);
        this.blueMapService = new WeakReference<>(blueMapService);
        this.plugin = new WeakReference<>(plugin);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    @Deprecated
    public Path getSaveFolder() {
        World world = unpack(this.world);
        if (world instanceof MCAWorld) {
            return ((MCAWorld) world).getDimensionFolder();
        } else {
            throw new UnsupportedOperationException("This world-type has no save-folder.");
        }
    }

    @Override
    public Collection<BlueMapMap> getMaps() {
        World world = unpack(this.world);
        return unpack(blueMapService).getMaps().values().stream()
                .filter(map -> map.getWorld().equals(world))
                .map(map -> new BlueMapMapImpl(map, this, plugin.get()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlueMapWorldImpl that = (BlueMapWorldImpl) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private <T> T unpack(WeakReference<T> ref) {
        return Objects.requireNonNull(ref.get(), "Reference lost to delegate object. Most likely BlueMap got reloaded and this instance is no longer valid.");
    }

    /**
     * Easy-access method for addons depending on BlueMapCore:<br>
     * <blockquote><pre>
     *     World world = ((BlueMapWorldImpl) blueMapWorld).world();
     * </pre></blockquote>
     */
    public World world() {
        return unpack(world);
    }

}
