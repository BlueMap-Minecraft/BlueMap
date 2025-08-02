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
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.core.map.MapSettings;
import de.bluecolored.bluemap.core.map.mask.CombinedMask;
import de.bluecolored.bluemap.core.util.Key;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;

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

    private CombinedMask renderMask = new CombinedMask();

    private long minInhabitedTime = 0;
    private int minInhabitedTimeRadius = 0;

    private boolean renderEdges = true;

    private boolean enablePerspectiveView = true;
    private boolean enableFlatView = true;
    private boolean enableFreeFlightView = true;
    private boolean enableHires = true;

    private boolean checkForRemovedRegions = true;

    private String storage = "file";

    private boolean ignoreMissingLightData = false;

    @Nullable private ConfigurationNode markerSets = null;

    // hidden config fields
    private int hiresTileSize = 32;
    private int lowresTileSize = 500;
    private int lodCount = 3;
    private int lodFactor = 5;

    /**
     * parse marker-config by converting it first from hocon to json and then loading it with MarkerGson
     */
    public Map<String, MarkerSet> parseMarkerSets() throws ConfigurationException {
        if (markerSets == null || markerSets.empty()) return Map.of();
        try {
            String markerJson = GsonConfigurationLoader.builder()
                    .headerMode(HeaderMode.NONE)
                    .lenient(false)
                    .indent(0)
                    .buildAndSaveString(markerSets);
            Gson gson = MarkerGson.addAdapters(new GsonBuilder())
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                    .create();
            Type markerSetType = new TypeToken<Map<String, MarkerSet>>() {}.getType();
            return gson.fromJson(markerJson, markerSetType);
        } catch (ConfigurateException | JsonParseException ex) {
            throw new ConfigurationException("Failed to parse marker-sets." +
                    "Make sure your marker-configuration for this map is valid.", ex);
        }
    }

    // ## legacy check ##
    @SuppressWarnings("unused")
    @Getter(AccessLevel.NONE)
    private Integer minX, maxX, minZ, maxZ, minY, maxY;
    public void checkLegacy() throws ConfigurationException {
        if (
                minX != null || maxX != null ||
                minZ != null || maxZ != null ||
                minY != null || maxY != null
        ) throw new ConfigurationException("""
                Your map-configuration is outdated!
                Looks like you updated BlueMap but did not follow the upgrade-instructions correctly.
                To fix your config, make sure to follow all relevant upgrade-instructions from BlueMap's changelogs.
                You can find them here: https://github.com/BlueMap-Minecraft/BlueMap/releases
                """.trim());
    }

}
