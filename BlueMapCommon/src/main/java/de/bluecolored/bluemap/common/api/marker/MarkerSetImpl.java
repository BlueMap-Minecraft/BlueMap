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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Sets;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.Marker;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.core.logger.Logger;
import ninja.leaping.configurate.ConfigurationNode;

public class MarkerSetImpl implements MarkerSet {

	private final String id;
	private String label;
	private boolean toggleable;
	private boolean isDefaultHidden;
	private Map<String, MarkerImpl> markers;
	
	private Set<String> removedMarkers; 
	
	private boolean hasUnsavedChanges;
	
	public MarkerSetImpl(String id) {
		this.id = id;
		this.label = id;
		this.toggleable = true;
		this.isDefaultHidden = false;
		this.markers = new ConcurrentHashMap<>();

		this.removedMarkers = Sets.newConcurrentHashSet();
		
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
	public synchronized ShapeMarkerImpl createShapeMarker(String id, BlueMapMap map, Vector3d position, Shape shape, float height) {
		removeMarker(id);
		
		ShapeMarkerImpl marker = new ShapeMarkerImpl(id, map, position, shape, height);
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
		
		Set<String> externallyRemovedMarkers = new HashSet<>(this.markers.keySet());
		for (ConfigurationNode markerNode : node.getNode("marker").getChildrenList()) {
			String id = markerNode.getNode("id").getString();
			String type = markerNode.getNode("type").getString();
			
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
					case POIMarkerImpl.MARKER_TYPE:
						marker = new POIMarkerImpl(id, dummyMap, Vector3d.ZERO);
						break;
					case ShapeMarkerImpl.MARKER_TYPE:
						marker = new ShapeMarkerImpl(id, dummyMap, Vector3d.ZERO, dummyShape, 0f);
						break;
					default:
						Logger.global.logDebug("Marker-API: Failed to load marker '" + id + "' in the set '" + this.id + "': Unknown marker-type '" + type + "'!");
						continue;
					}

					marker.load(api, markerNode, true);
				} else {
					marker.load(api, markerNode, overwriteChanges);					
				}
				markers.put(id, marker);
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
		
		this.label = node.getNode("label").getString(id);
		this.toggleable = node.getNode("toggleable").getBoolean(true);
		this.isDefaultHidden = node.getNode("defaultHide").getBoolean(false);
	}
	
	public synchronized void save(ConfigurationNode node) {
		node.getNode("id").setValue(this.id);
		node.getNode("label").setValue(this.label);
		node.getNode("toggleable").setValue(this.toggleable);
		node.getNode("defaultHide").setValue(this.isDefaultHidden);
		
		for (MarkerImpl marker : markers.values()) {
			marker.save(node.getNode("marker").appendListNode());
		}

		removedMarkers.clear();
		this.hasUnsavedChanges = false;
	}

}
