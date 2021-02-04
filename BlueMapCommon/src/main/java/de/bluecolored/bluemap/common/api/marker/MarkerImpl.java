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
import com.google.common.base.Preconditions;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.Marker;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.Objects;
import java.util.Optional;

public abstract class MarkerImpl implements Marker {

	private final String id;
	private BlueMapMap map;
	private Vector3d postition;
	private double minDistance, maxDistance;
	private String label, link;
	private boolean newTab;
	
	private boolean hasUnsavedChanges;
	
	public MarkerImpl(String id, BlueMapMap map, Vector3d position) {
		Preconditions.checkNotNull(id);
		Preconditions.checkNotNull(map);
		Preconditions.checkNotNull(position);
		
		this.id = id;
		this.map = map;
		this.postition = position;
		this.minDistance = 0;
		this.maxDistance = 100000;
		this.label = id;
		this.link = null;
		this.newTab = true;
		
		this.hasUnsavedChanges = true;
	}
	
	@Override
	public String getId() {
		return this.id;
	}
	
	public abstract String getType();

	@Override
	public BlueMapMap getMap() {
		return this.map;
	}

	@Override
	public synchronized void setMap(BlueMapMap map) {
		this.map = map;
		this.hasUnsavedChanges = true;
	}

	@Override
	public Vector3d getPosition() {
		return this.postition;
	}

	@Override
	public synchronized void setPosition(Vector3d position) {
		this.postition = position;
		this.hasUnsavedChanges = true;
	}

	@Override
	public double getMinDistance() {
		return this.minDistance;
	}

	@Override
	public synchronized void setMinDistance(double minDistance) {
		this.minDistance = minDistance;
		this.hasUnsavedChanges = true;
	}

	@Override
	public double getMaxDistance() {
		return this.maxDistance;
	}

	@Override
	public synchronized void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
		this.hasUnsavedChanges = true;
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
	public Optional<String> getLink() {
		return Optional.ofNullable(this.link);
	}

	@Override
	public boolean isNewTab() {
		return this.newTab;
	}

	@Override
	public synchronized void setLink(String link, boolean newTab) {
		this.link = link;
		this.newTab = newTab;
		this.hasUnsavedChanges = true;
	}
	
	@Override
	public synchronized void removeLink() {
		this.link = null;
		this.hasUnsavedChanges = true;
	}
	
	public synchronized void load(BlueMapAPI api, ConfigurationNode markerNode, boolean overwriteChanges) throws MarkerFileFormatException {
		if (!overwriteChanges && hasUnsavedChanges) return;
		hasUnsavedChanges = false;
		
		//map
		String mapId = markerNode.getNode("map").getString();
		if (mapId == null) throw new MarkerFileFormatException("There is no map defined!");
		this.map = api.getMap(mapId).orElseThrow(() -> new MarkerFileFormatException("Could not resolve map with id: " + mapId));
		
		//position
		this.postition = readPos(markerNode.getNode("position"));
		
		//minmaxDistance
		this.minDistance = markerNode.getNode("minDistance").getDouble(0);
		this.maxDistance = markerNode.getNode("maxDistance").getDouble(100000);
		
		//label
		this.label = markerNode.getNode("label").getString(this.id);
		
		//link
		this.link = markerNode.getNode("link").getString();
		this.newTab = markerNode.getNode("newTab").getBoolean(true);
	}
	
	public synchronized void save(ConfigurationNode markerNode) {		
		markerNode.getNode("id").setValue(this.id);
		markerNode.getNode("type").setValue(this.getType());
		markerNode.getNode("map").setValue(this.map.getId());
		writePos(markerNode.getNode("position"), this.postition);
		markerNode.getNode("minDistance").setValue(Math.round(this.minDistance * 1000d) / 1000d);
		markerNode.getNode("maxDistance").setValue(Math.round(this.maxDistance * 1000d) / 1000d);
		markerNode.getNode("label").setValue(this.label);
		markerNode.getNode("link").setValue(this.link);
		markerNode.getNode("newTab").setValue(this.newTab);

		hasUnsavedChanges = false;
	}

	private static Vector3d readPos(ConfigurationNode node) throws MarkerFileFormatException {
		ConfigurationNode nx, ny, nz;
		nx = node.getNode("x");
		ny = node.getNode("y");
		nz = node.getNode("z");
		
		if (nx.isVirtual() || ny.isVirtual() || nz.isVirtual()) throw new MarkerFileFormatException("Failed to read position: One of the nodes x,y or z is missing!");
		
		return new Vector3d(
				nx.getDouble(),
				ny.getDouble(),
				nz.getDouble()
			);
	}
	
	private static void writePos(ConfigurationNode node, Vector3d pos) {
		node.getNode("x").setValue(Math.round(pos.getX() * 1000d) / 1000d);
		node.getNode("y").setValue(Math.round(pos.getY() * 1000d) / 1000d);
		node.getNode("z").setValue(Math.round(pos.getZ() * 1000d) / 1000d);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MarkerImpl marker = (MarkerImpl) o;
		return id.equals(marker.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

}
