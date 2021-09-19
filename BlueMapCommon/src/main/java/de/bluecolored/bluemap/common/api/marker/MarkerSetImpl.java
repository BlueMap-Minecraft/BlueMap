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
import de.bluecolored.bluemap.api.marker.Line;
import de.bluecolored.bluemap.api.marker.Marker;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.core.logger.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarkerSetImpl implements MarkerSet {

    private final String id;
    private String label;
    private boolean toggleable;
    private boolean isDefaultHidden;
    private final Map<String, MarkerImpl> markers;

    private final Set<String> removedMarkers;

    private boolean hasUnsavedChanges;

    public MarkerSetImpl(String id) {
        this.id = id;
        this.label = id;
        this.toggleable = true;
        this.isDefaultHidden = false;
        this.markers = new ConcurrentHashMap<>();

        this.removedMarkers = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.hasUnsavedChanges = true;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public synchronized void setLabel(String label) {
        this.label = label;
        this.hasUnsavedChanges = true;
    }

    @Override
    public boolean isToggleable() {
        return this.toggleable;
    }

    @Override
    public synchronized void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
        this.hasUnsavedChanges = true;
    }

    @Override
    public boolean isDefaultHidden() {
        return this.isDefaultHidden;
    }

    @Override
    public synchronized void setDefaultHidden(boolean defaultHide) {
        this.isDefaultHidden = defaultHide;
        this.hasUnsavedChanges = true;
    }

    @Override
    public Collection<Marker> getMarkers() {
        return Collections.unmodifiableCollection(markers.values());
    }

    @Override
    public Optional<Marker> getMarker(String id) {
        return Optional.ofNullable(markers.get(id));
    }

    @Override
    public synchronized POIMarkerImpl createPOIMarker(String id, BlueMapMap map, Vector3d position) {
        removeMarker(id);

        POIMarkerImpl marker = new POIMarkerImpl(id, map, position);
        markers.put(id, marker);

        return marker;
    }

    @Override
    public HtmlMarkerImpl createHtmlMarker(String id, BlueMapMap map, Vector3d position, String html) {
        removeMarker(id);

        HtmlMarkerImpl marker = new HtmlMarkerImpl(id, map, position, html);
        markers.put(id, marker);

        return marker;
    }

    @Override
    public synchronized ShapeMarkerImpl createShapeMarker(String id, BlueMapMap map, Vector3d position, Shape shape, float y) {
        removeMarker(id);

        ShapeMarkerImpl marker = new ShapeMarkerImpl(id, map, position, shape, y);
        markers.put(id, marker);

        return marker;
    }

    @Override
    public ExtrudeMarkerImpl createExtrudeMarker(String id, BlueMapMap map, Vector3d position, Shape shape, float minY, float maxY) {
        removeMarker(id);

        ExtrudeMarkerImpl marker = new ExtrudeMarkerImpl(id, map, position, shape, minY, maxY);
        markers.put(id, marker);

        return marker;
    }

    @Override
    public LineMarkerImpl createLineMarker(String id, BlueMapMap map, Vector3d position, Line line) {
        removeMarker(id);

        LineMarkerImpl marker = new LineMarkerImpl(id, map, position, line);
        markers.put(id, marker);

        return marker;
    }

    @Override
    public synchronized boolean removeMarker(String id) {
        if (markers.remove(id) != null) {
            removedMarkers.add(id);
            return true;
        }
        return false;
    }

    public synchronized void load(BlueMapAPI api, ConfigurationNode node, boolean overwriteChanges) throws MarkerFileFormatException {
        BlueMapMap dummyMap = api.getMaps().iterator().next();
        Shape dummyShape = Shape.createRect(0d, 0d, 1d, 1d);
        Line dummyLine = new Line(Vector3d.ZERO, Vector3d.ONE);

        Set<String> externallyRemovedMarkers = new HashSet<>(this.markers.keySet());
        for (ConfigurationNode markerNode : node.node("marker").childrenList()) {
            String id = markerNode.node("id").getString();
            String type = markerNode.node("type").getString();

            if (id == null || type == null) {
                Logger.global.logDebug("Marker-API: Failed to load a marker in the set '" + this.id + "': No id or type defined!");
                continue;
            }

            externallyRemovedMarkers.remove(id);
            if (!overwriteChanges && removedMarkers.contains(id)) continue;

            MarkerImpl marker = markers.get(id);

            try {
                if (marker == null || !marker.getType().equals(type)) {
                    switch (type) {
                        case HtmlMarkerImpl.MARKER_TYPE :
                            marker = new HtmlMarkerImpl(id, dummyMap, Vector3d.ZERO, "");
                            break;
                        case POIMarkerImpl.MARKER_TYPE:
                            marker = new POIMarkerImpl(id, dummyMap, Vector3d.ZERO);
                            break;
                        case ShapeMarkerImpl.MARKER_TYPE:
                            marker = new ShapeMarkerImpl(id, dummyMap, Vector3d.ZERO, dummyShape, 0f);
                            break;
                        case ExtrudeMarkerImpl.MARKER_TYPE:
                            marker = new ExtrudeMarkerImpl(id, dummyMap, Vector3d.ZERO, dummyShape, 0f, 1f);
                            break;
                        case LineMarkerImpl.MARKER_TYPE:
                            marker = new LineMarkerImpl(id, dummyMap, Vector3d.ZERO, dummyLine);
                            break;
                        default:
                            Logger.global.logDebug("Marker-API: Failed to load marker '" + id + "' in the set '" + this.id + "': Unknown marker-type '" + type + "'!");
                            continue;
                    }

                    marker.load(api, markerNode, true);
                } else {
                    marker.load(api, markerNode, overwriteChanges);
                }

                if (overwriteChanges) {
                    markers.put(id, marker);
                } else {
                    markers.putIfAbsent(id, marker);
                }
            } catch (MarkerFileFormatException ex) {
                Logger.global.logDebug("Marker-API: Failed to load marker '" + id + "' in the set '" + this.id + "': " + ex);
            }
        }

        if (overwriteChanges) {
            for (String id : externallyRemovedMarkers) {
                markers.remove(id);
            }

            this.removedMarkers.clear();
        }

        if (!overwriteChanges && hasUnsavedChanges) return;
        hasUnsavedChanges = false;

        this.label = node.node("label").getString(id);
        this.toggleable = node.node("toggleable").getBoolean(true);
        this.isDefaultHidden = node.node("defaultHide").getBoolean(false);
    }

    public synchronized void save(ConfigurationNode node) throws SerializationException {
        node.node("id").set(this.id);
        node.node("label").set(this.label);
        node.node("toggleable").set(this.toggleable);
        node.node("defaultHide").set(this.isDefaultHidden);

        for (MarkerImpl marker : markers.values()) {
            marker.save(node.node("marker").appendListNode());
        }

        removedMarkers.clear();
        this.hasUnsavedChanges = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarkerSetImpl markerSet = (MarkerSetImpl) o;
        return id.equals(markerSet.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
