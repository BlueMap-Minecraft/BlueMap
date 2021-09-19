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
import de.bluecolored.bluemap.api.marker.LineMarker;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class LineMarkerImpl extends ObjectMarkerImpl implements LineMarker {
    public static final String MARKER_TYPE = "line";

    private Line line;
    private boolean depthTest;
    private int lineWidth;
    private Color lineColor;

    private boolean hasUnsavedChanges;

    public LineMarkerImpl(String id, BlueMapMap map, Vector3d position, Line line) {
        super(id, map, position);

        Objects.requireNonNull(line);

        this.line = line;
        this.lineWidth = 2;
        this.lineColor = new Color(255, 0, 0, 200);

        this.hasUnsavedChanges = true;
    }

    @Override
    public String getType() {
        return MARKER_TYPE;
    }

    @Override
    public Line getLine() {
        return line;
    }

    @Override
    public synchronized void setLine(Line line) {
        Objects.requireNonNull(line);

        this.line = line;
        this.hasUnsavedChanges = true;
    }

    @Override
    public boolean isDepthTestEnabled() {
        return this.depthTest;
    }

    @Override
    public void setDepthTestEnabled(boolean enabled) {
        this.depthTest = enabled;
        this.hasUnsavedChanges = true;
    }

    @Override
    public int getLineWidth() {
        return lineWidth;
    }

    @Override
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
        this.hasUnsavedChanges = true;
    }

    @Override
    public Color getLineColor() {
        return this.lineColor;
    }

    @Override
    public synchronized void setLineColor(Color color) {
        Objects.requireNonNull(color);

        this.lineColor = color;
        this.hasUnsavedChanges = true;
    }

    @Override
    public void load(BlueMapAPI api, ConfigurationNode markerNode, boolean overwriteChanges) throws MarkerFileFormatException {
        super.load(api, markerNode, overwriteChanges);

        if (!overwriteChanges && hasUnsavedChanges) return;
        this.hasUnsavedChanges = false;

        this.line = readLine(markerNode.node("line"));
        this.depthTest = markerNode.node("depthTest").getBoolean(true);
        this.lineWidth = markerNode.node("lineWidth").getInt(2);
        this.lineColor = readColor(markerNode.node("lineColor"));
    }

    @Override
    public void save(ConfigurationNode markerNode) throws SerializationException {
        super.save(markerNode);

        writeLine(markerNode.node("line"), this.line);
        markerNode.node("depthTest").set(this.depthTest);
        markerNode.node("lineWidth").set(this.lineWidth);
        writeColor(markerNode.node("lineColor"), this.lineColor);

        hasUnsavedChanges = false;
    }

    private Line readLine(ConfigurationNode node) throws MarkerFileFormatException {
        List<? extends ConfigurationNode> posNodes = node.childrenList();

        if (posNodes.size() < 3) throw new MarkerFileFormatException("Failed to read line: point-list has fewer than 2 entries!");

        Vector3d[] positions = new Vector3d[posNodes.size()];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = readLinePos(posNodes.get(i));
        }

        return new Line(positions);
    }

    private static Vector3d readLinePos(ConfigurationNode node) throws MarkerFileFormatException {
        ConfigurationNode nx, ny, nz;
        nx = node.node("x");
        ny = node.node("y");
        nz = node.node("z");

        if (nx.virtual() || ny.virtual() || nz.virtual()) throw new MarkerFileFormatException("Failed to read line position: Node x, y or z is not set!");

        return new Vector3d(
                nx.getDouble(),
                ny.getDouble(),
                nz.getDouble()
            );
    }

    private static Color readColor(ConfigurationNode node) throws MarkerFileFormatException {
        ConfigurationNode nr, ng, nb, na;
        nr = node.node("r");
        ng = node.node("g");
        nb = node.node("b");
        na = node.node("a");

        if (nr.virtual() || ng.virtual() || nb.virtual()) throw new MarkerFileFormatException("Failed to read color: Node r,g or b is not set!");

        float alpha = (float) na.getDouble(1);
        if (alpha < 0 || alpha > 1) throw new MarkerFileFormatException("Failed to read color: alpha value out of range (0-1)!");

        try {
            return new Color(nr.getInt(), ng.getInt(), nb.getInt(), (int)(alpha * 255));
        } catch (IllegalArgumentException ex) {
            throw new MarkerFileFormatException("Failed to read color: " + ex.getMessage(), ex);
        }
    }

    private static void writeLine(ConfigurationNode node, Line line) throws SerializationException {
        for (int i = 0; i < line.getPointCount(); i++) {
            ConfigurationNode pointNode = node.appendListNode();
            Vector3d point = line.getPoint(i);
            pointNode.node("x").set(Math.round(point.getX() * 1000d) / 1000d);
            pointNode.node("y").set(Math.round(point.getY() * 1000d) / 1000d);
            pointNode.node("z").set(Math.round(point.getZ() * 1000d) / 1000d);
        }
    }

    private static void writeColor(ConfigurationNode node, Color color) throws SerializationException {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        float a = color.getAlpha() / 255f;

        node.node("r").set(r);
        node.node("g").set(g);
        node.node("b").set(b);
        node.node("a").set(a);
    }

}
