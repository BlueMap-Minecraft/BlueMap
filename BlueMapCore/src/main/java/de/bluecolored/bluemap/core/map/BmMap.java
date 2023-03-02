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
package de.bluecolored.bluemap.core.map;

import com.flowpowered.math.vector.Vector2i;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.HiresModelManager;
import de.bluecolored.bluemap.core.map.lowres.LowresTileManager;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@DebugDump
public class BmMap {

    public static final String META_FILE_SETTINGS = "settings.json";
    public static final String META_FILE_TEXTURES = "textures.json";
    public static final String META_FILE_RENDER_STATE = ".rstate";
    public static final String META_FILE_MARKERS = "live/markers.json";
    public static final String META_FILE_PLAYERS = "live/players.json";

    private final String id;
    private final String name;
    private final String worldId;
    private final World world;
    private final Storage storage;
    private final MapSettings mapSettings;

    private final ResourcePack resourcePack;
    private final MapRenderState renderState;
    private final TextureGallery textureGallery;

    private final HiresModelManager hiresModelManager;
    private final LowresTileManager lowresTileManager;

    private final ConcurrentHashMap<String, MarkerSet> markerSets;

    private Predicate<Vector2i> tileFilter;

    private long renderTimeSumNanos;
    private long tilesRendered;

    public BmMap(String id, String name, String worldId, World world, Storage storage, ResourcePack resourcePack, MapSettings settings) throws IOException {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.worldId = Objects.requireNonNull(worldId);
        this.world = Objects.requireNonNull(world);
        this.storage = Objects.requireNonNull(storage);
        this.resourcePack = Objects.requireNonNull(resourcePack);
        this.mapSettings = Objects.requireNonNull(settings);

        this.renderState = new MapRenderState();
        loadRenderState();

        this.textureGallery = loadTextureGallery();
        this.textureGallery.put(resourcePack);
        saveTextureGallery();

        this.hiresModelManager = new HiresModelManager(
                storage.tileStorage(id, 0),
                this.resourcePack,
                this.textureGallery,
                settings,
                new Grid(settings.getHiresTileSize(), 2)
        );

        this.lowresTileManager = new LowresTileManager(
                storage.mapStorage(id),
                new Grid(settings.getLowresTileSize()),
                settings.getLodCount(),
                settings.getLodFactor()
        );

        this.tileFilter = t -> true;

        this.markerSets = new ConcurrentHashMap<>();

        this.renderTimeSumNanos = 0;
        this.tilesRendered = 0;

        saveMapSettings();
    }

    public void renderTile(Vector2i tile) {
        if (!tileFilter.test(tile)) return;

        long start = System.nanoTime();

        hiresModelManager.render(world, tile, lowresTileManager, mapSettings.isSaveHiresLayer());

        long end = System.nanoTime();
        long delta = end - start;

        renderTimeSumNanos += delta;
        tilesRendered ++;
    }

    public synchronized void save() {
        lowresTileManager.save();
        saveRenderState();
        saveMarkerState();
        savePlayerState();
        saveMapSettings();

        // only save texture gallery if not present in storage
        try {
            if (storage.readMetaInfo(id, META_FILE_TEXTURES).isEmpty())
                saveTextureGallery();
        } catch (IOException e) {
            Logger.global.logError("Failed to read texture gallery", e);
        }
    }

    private void loadRenderState() throws IOException {
        Optional<InputStream> rstateData = storage.readMeta(id, META_FILE_RENDER_STATE);
        if (rstateData.isPresent()) {
            try (InputStream in = rstateData.get()){
                this.renderState.load(in);
            } catch (IOException ex) {
                Logger.global.logWarning("Failed to load render-state for map '" + getId() + "': " + ex);
            }
        }
    }

    public synchronized void saveRenderState() {
        try (OutputStream out = storage.writeMeta(id, META_FILE_RENDER_STATE)) {
            this.renderState.save(out);
        } catch (IOException ex){
            Logger.global.logError("Failed to save render-state for map: '" + this.id + "'!", ex);
        }
    }

    private TextureGallery loadTextureGallery() throws IOException {
        TextureGallery gallery = null;
        Optional<InputStream> texturesData = storage.readMeta(id, META_FILE_TEXTURES);
        if (texturesData.isPresent()) {
            try (InputStream in = texturesData.get()){
                gallery = TextureGallery.readTexturesFile(in);
            } catch (IOException ex) {
                Logger.global.logError("Failed to load textures for map '" + getId() + "'!", ex);
            }
        }
        return gallery != null ? gallery : new TextureGallery();
    }

    private void saveTextureGallery() {
        try (OutputStream out = storage.writeMeta(id, META_FILE_TEXTURES)) {
            this.textureGallery.writeTexturesFile(this.resourcePack, out);
        } catch (IOException ex) {
            Logger.global.logError("Failed to save textures for map '" + getId() + "'!", ex);
        }
    }

    public synchronized void resetTextureGallery() {
        this.textureGallery.clear();
        this.textureGallery.put(this.resourcePack);
    }

    private void saveMapSettings() {
        try (
                OutputStream out = storage.writeMeta(id, META_FILE_SETTINGS);
                Writer writer = new OutputStreamWriter(out)
        ) {
            ResourcesGson.addAdapter(new GsonBuilder())
                    .registerTypeAdapter(BmMap.class, new MapSettingsSerializer())
                    .create()
                    .toJson(this, writer);
        } catch (Exception ex) {
            Logger.global.logError("Failed to save settings for map '" + getId() + "'!", ex);
        }
    }

    public synchronized void saveMarkerState() {
        try (
                OutputStream out = storage.writeMeta(id, META_FILE_MARKERS);
                Writer writer = new OutputStreamWriter(out)
        ) {
            MarkerGson.INSTANCE.toJson(this.markerSets, writer);
        } catch (Exception ex) {
            Logger.global.logError("Failed to save markers for map '" + getId() + "'!", ex);
        }
    }

    public synchronized void savePlayerState() {
        try (
                OutputStream out = storage.writeMeta(id, META_FILE_PLAYERS);
        ) {
            out.write("{}".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            Logger.global.logError("Failed to save markers for map '" + getId() + "'!", ex);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorldId() {
        return worldId;
    }

    public World getWorld() {
        return world;
    }

    public Storage getStorage() {
        return storage;
    }

    public MapSettings getMapSettings() {
        return mapSettings;
    }

    public MapRenderState getRenderState() {
        return renderState;
    }

    public HiresModelManager getHiresModelManager() {
        return hiresModelManager;
    }

    public LowresTileManager getLowresTileManager() {
        return lowresTileManager;
    }

    public Map<String, MarkerSet> getMarkerSets() {
        return markerSets;
    }

    public Predicate<Vector2i> getTileFilter() {
        return tileFilter;
    }

    public void setTileFilter(Predicate<Vector2i> tileFilter) {
        this.tileFilter = tileFilter;
    }

    public long getAverageNanosPerTile() {
        return renderTimeSumNanos / tilesRendered;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BmMap) {
            BmMap that = (BmMap) obj;

            return this.id.equals(that.id);
        }

        return false;
    }

    @Override
    public String toString() {
        return "BmMap{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", world=" + world +
               ", storage=" + storage +
               '}';
    }

}
