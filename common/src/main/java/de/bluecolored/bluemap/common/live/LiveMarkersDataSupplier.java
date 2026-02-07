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
package de.bluecolored.bluemap.common.live;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import de.bluecolored.bluemap.common.rendermanager.CombinedRenderTask;
import de.bluecolored.bluemap.common.rendermanager.MapRenderTask;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.Grid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class LiveMarkersDataSupplier implements Supplier<String> {

    private static final String PENDING_SET_ID = "_bluemap_pending_renders";

    private final BmMap map;
    private final Map<String, MarkerSet> baseMarkerSets;
    private final @Nullable Supplier<@Nullable RenderManager> renderManagerSupplier;

    public LiveMarkersDataSupplier(BmMap map, @Nullable Supplier<@Nullable RenderManager> renderManagerSupplier) {
        this.map = Objects.requireNonNull(map, "map");
        this.baseMarkerSets = map.getMarkerSets();
        this.renderManagerSupplier = renderManagerSupplier;
    }

    @Override
    public String get() {
        Map<String, MarkerSet> mergedSets = new HashMap<>(baseMarkerSets);

        RenderManager renderManager = renderManagerSupplier != null ? renderManagerSupplier.get() : null;
        if (renderManager != null) {
            MarkerSet pending = buildPendingRenderMarkers(renderManager);
            if (!pending.getMarkers().isEmpty()) {
                mergedSets.put(PENDING_SET_ID, pending);
            } else {
                mergedSets.remove(PENDING_SET_ID);
            }
        }

        return MarkerGson.INSTANCE.toJson(mergedSets);
    }

    private MarkerSet buildPendingRenderMarkers(RenderManager renderManager) {
        MarkerSet set = new MarkerSet("Pending renders", true, false);

        Grid regionGrid = map.getWorld().getRegionGrid();

        Set<Vector2i> currentRegions = new HashSet<>();
        Set<Vector2i> scheduledRegions = new HashSet<>();

        RenderTask current = renderManager.getCurrentRenderTask();
        if (current != null)
            collectRegions(current, currentRegions);

        for (RenderTask task : renderManager.getScheduledRenderTasks()) {
            collectRegions(task, scheduledRegions);
        }

        // scheduled-only regions (exclude ones already in current)
        scheduledRegions.removeAll(currentRegions);

        int scheduledCount = scheduledRegions.size();
        int index = 0;

        // current (red) regions
        for (Vector2i region : currentRegions) {
            ShapeMarker marker = createRegionShapeMarker(regionGrid, region, 64f);
            String id = region.getX() + "," + region.getY();
            marker.setLabel("Rendering region " + id);
            marker.setDetail("Currently rendering region " + id);
            marker.setColors(
                    new Color(255, 0, 0, 1f), // solid red outline
                    new Color(255, 0, 0, 0.35f) // semi-transparent red fill
            );
            set.put("current-" + id, marker);
        }

        // queued (blue gradient) regions
        for (Vector2i region : scheduledRegions) {
            float t = scheduledCount <= 1 ? 0.5f : (float) index / (float) (scheduledCount - 1);
            // blue gradient from lighter to deeper blue (0-255 RGB)
            int r = 0;
            int g = (int) Math.round(77 + 51 * (1f - t)); // between ~#4d?? and ~#7f??
            int b = (int) Math.round(153 + 102 * t); // from ~#9999ff to ~#99ffff

            ShapeMarker marker = createRegionShapeMarker(regionGrid, region, 64f);
            String id = region.getX() + "," + region.getY();
            marker.setLabel("Queued region " + id);
            marker.setDetail("Scheduled render for region " + id);
            marker.setColors(
                    new Color(r, g, b, 1f), // solid outline
                    new Color(r, g, b, 0.35f) // semi-transparent fill
            );
            set.put("queued-" + id, marker);

            index++;
        }

        return set;
    }

    private ShapeMarker createRegionShapeMarker(Grid regionGrid, Vector2i region, float y) {
        Vector2i min = regionGrid.getCellMin(region);
        Vector2i max = regionGrid.getCellMax(region);

        Shape shape = Shape.createRect(
                new Vector2d(min.getX(), min.getY()),
                new Vector2d(max.getX() + 1, max.getY() + 1));

        Vector2i halfCell = regionGrid.getGridSize().div(2);
        Vector2i center2d = min.add(halfCell);
        Vector3d position = new Vector3d(center2d.getX(), y, center2d.getY());

        ShapeMarker marker = new ShapeMarker("", position, shape, y);
        marker.setDepthTestEnabled(false);
        marker.setLineWidth(2);
        marker.centerPosition();
        return marker;
    }

    private void collectRegions(RenderTask task, Set<Vector2i> regions) {
        if (task instanceof CombinedRenderTask<?> combined) {
            for (RenderTask sub : combined.getTasks()) {
                collectRegions(sub, regions);
            }
            return;
        }

        if (!(task instanceof MapRenderTask mapTask))
            return;
        if (!mapTask.getMap().equals(map))
            return;

        Vector2i region = mapTask.getRegion();
        if (region != null) {
            regions.add(region);
        }
    }

}
