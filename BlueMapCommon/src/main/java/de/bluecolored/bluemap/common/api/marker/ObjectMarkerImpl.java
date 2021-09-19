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
package de.bluecolored.bluemap.common.api.marker;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.ObjectMarker;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public abstract class ObjectMarkerImpl extends MarkerImpl implements ObjectMarker {

    private String detail;

    private boolean hasUnsavedChanges;

    public ObjectMarkerImpl(String id, BlueMapMap map, Vector3d position) {
        super(id, map, position);

        this.detail = null;

        this.hasUnsavedChanges = true;
    }

    @Override
    public String getDetail() {
        if (detail == null) return getLabel();
        return detail;
    }

    @Override
    public void setDetail(String detail) {
        this.detail = detail;
        this.hasUnsavedChanges = true;
    }

    @Override
    public void load(BlueMapAPI api, ConfigurationNode markerNode, boolean overwriteChanges) throws MarkerFileFormatException {
        super.load(api, markerNode, overwriteChanges);

        if (!overwriteChanges && hasUnsavedChanges) return;
        this.hasUnsavedChanges = false;

        this.detail = markerNode.node("detail").getString();
    }

    @Override
    public void save(ConfigurationNode markerNode) throws SerializationException {
        super.save(markerNode);

        if (this.detail != null) markerNode.node("detail").set(this.detail);

        hasUnsavedChanges = false;
    }

}
