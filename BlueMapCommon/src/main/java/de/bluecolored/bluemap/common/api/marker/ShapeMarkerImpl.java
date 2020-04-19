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

import java.awt.Color;
import java.util.List;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.Shape;
import de.bluecolored.bluemap.api.marker.ShapeMarker;
import de.bluecolored.bluemap.api.renderer.BlueMapMap;
import ninja.leaping.configurate.ConfigurationNode;

public class ShapeMarkerImpl extends MarkerImpl implements ShapeMarker {
	public static final String MARKER_TYPE = "shape";

	private Shape shape;
	private float height;
	private Color borderColor, fillColor;

	private boolean hasUnsavedChanges;
	
	public ShapeMarkerImpl(String id, BlueMapMap map, Vector3d position, Shape shape, float height) {
		super(id, map, position);

		Preconditions.checkNotNull(shape);
		
		this.shape = shape;
		this.height = height;

		this.hasUnsavedChanges = true;
	}
	
	@Override
	public String getType() {
		return MARKER_TYPE;
	}

	@Override
	public Shape getShape() {
		return this.shape;
	}

	@Override
	public float getHeight() {
		return this.height;
	}

	@Override
	public synchronized void setShape(Shape shape, float height) {
		Preconditions.checkNotNull(shape);
		
		this.shape = shape;
		this.height = height;
		this.hasUnsavedChanges = true;
	}

	@Override
	public Color getBorderColor() {
		return this.borderColor;
	}

	@Override
	public synchronized void setBorderColor(Color color) {
		Preconditions.checkNotNull(color);
		
		this.borderColor = color;
		this.hasUnsavedChanges = true;
	}

	@Override
	public Color getFillColor() {
		return this.fillColor;
	}

	@Override
	public synchronized void setFillColor(Color color) {
		Preconditions.checkNotNull(color);
		
		this.fillColor = color;
		this.hasUnsavedChanges = true;
	}
	
	@Override
	public void load(BlueMapAPI api, ConfigurationNode markerNode) throws MarkerFileFormatException {
		super.load(api, markerNode);

		this.shape = readShape(markerNode.getNode("icon"));
		this.height = markerNode.getNode("height").getFloat(64);
		this.borderColor = readColor(markerNode.getNode("borderColor"));
		this.fillColor = readColor(markerNode.getNode("fillColor"));
	}
	
	@Override
	public void save(ConfigurationNode markerNode, boolean force) {
		super.save(markerNode, force);
		
		if (!force && !hasUnsavedChanges) return;

		writeShape(markerNode.getNode("shape"), this.shape);
		markerNode.getNode("height").setValue(Math.round(height * 1000f) / 1000f);
		writeColor(markerNode.getNode("borderColor"), this.borderColor);
		writeColor(markerNode.getNode("fillColor"), this.fillColor);
		
		hasUnsavedChanges = false;
	}
	
	private Shape readShape(ConfigurationNode node) throws MarkerFileFormatException {
		List<? extends ConfigurationNode> posNodes = node.getChildrenList();
		
		if (posNodes.size() < 3) throw new MarkerFileFormatException("Failed to read shape: point-list has fewer than 3 entries!");
		
		Vector2d[] positions = new Vector2d[posNodes.size()];
		for (int i = 0; i < positions.length; i++) {
			positions[i] = readShapePos(posNodes.get(i));
		}
		
		return new Shape(positions);
	}
	
	private static Vector2d readShapePos(ConfigurationNode node) throws MarkerFileFormatException {
		ConfigurationNode nx, nz;
		nx = node.getNode("x");
		nz = node.getNode("z");
		
		if (nx.isVirtual() || nz.isVirtual()) throw new MarkerFileFormatException("Failed to read shape position: Node x or z is not set!");
		
		return new Vector2d(
				nx.getDouble(),
				nz.getDouble()
			);
	}
	
	private static Color readColor(ConfigurationNode node) throws MarkerFileFormatException {
		ConfigurationNode nr, ng, nb, na;
		nr = node.getNode("r");
		ng = node.getNode("g");
		nb = node.getNode("b");
		na = node.getNode("a");
		
		if (nr.isVirtual() || ng.isVirtual() || nb.isVirtual()) throw new MarkerFileFormatException("Failed to read color: Node r,g or b is not set!");
		
		float alpha = na.getFloat(1);
		if (alpha < 0 || alpha > 1) throw new MarkerFileFormatException("Failed to read color: alpha value out of range (0-1)!");
		
		try {
			return new Color(nr.getInt(), ng.getInt(), nb.getInt(), (int)(alpha * 255));
		} catch (IllegalArgumentException ex) {
			throw new MarkerFileFormatException("Failed to read color: " + ex.getMessage(), ex);
		}
	}
	
	private static void writeShape(ConfigurationNode node, Shape shape) {
		for (int i = 0; i < shape.getPointCount(); i++) {
			ConfigurationNode pointNode = node.getAppendedNode();
			Vector2d point = shape.getPoint(i);
			pointNode.getNode("x").setValue(Math.round(point.getX() * 1000d) / 1000d);
			pointNode.getNode("z").setValue(Math.round(point.getY() * 1000d) / 1000d);
		}
	}
	
	private static void writeColor(ConfigurationNode node, Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		float a = color.getAlpha() / 255f;
		
		node.getNode("r").setValue(r);
		node.getNode("g").setValue(g);
		node.getNode("b").setValue(b);
		node.getNode("a").setValue(a);
	}

}
