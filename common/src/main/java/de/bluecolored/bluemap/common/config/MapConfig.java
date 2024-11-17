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
package de.bluecolored.bluemap.common.config;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.map.MapSettings;
import de.bluecolored.bluemap.core.util.Key;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
@Getter
public class MapConfig implements MapSettings {

    @Nullable private Path world = null;
    @Nullable private Key dimension = null;

    @Nullable private String name = null;

    private int sorting = 0;

    @Nullable private Vector2i startPos = null;

    private String skyColor = "#7dabff";
    private String voidColor = "#000000";

    private float ambientLight = 0;
    private float skyLight = 1;

    private int removeCavesBelowY = 55;
    private int caveDetectionOceanFloor = 10000;
    private boolean caveDetectionUsesBlockLight = false;

    @Getter(AccessLevel.NONE) private int minX = Integer.MIN_VALUE;
    @Getter(AccessLevel.NONE) private int maxX = Integer.MAX_VALUE;
    @Getter(AccessLevel.NONE) private int minZ = Integer.MIN_VALUE;
    @Getter(AccessLevel.NONE) private int maxZ = Integer.MAX_VALUE;
    @Getter(AccessLevel.NONE) private int minY = Integer.MIN_VALUE;
    @Getter(AccessLevel.NONE) private int maxY = Integer.MAX_VALUE;

    private transient Vector3i min = null;
    private transient Vector3i max = null;

    private long minInhabitedTime = 0;
    private int minInhabitedTimeRadius = 0;

    private boolean renderEdges = true;

    private boolean enablePerspectiveView = true;
    private boolean enableFlatView = true;
    private boolean enableFreeFlightView = true;
    private boolean enableHires = true;

    private String storage = "file";

    private boolean ignoreMissingLightData = false;

    @Nullable private ConfigurationNode markerSets = null;

    // hidden config fields
    private int hiresTileSize = 32;
    private int lowresTileSize = 500;
    private int lodCount = 3;
    private int lodFactor = 5;

    public Vector3i getMinPos() {
        if (min == null) min = new Vector3i(minX, minY, minZ);
        return min;
    }

    public Vector3i getMaxPos() {
        if (max == null) max = new Vector3i(maxX, maxY, maxZ);
        return max;
    }

}
