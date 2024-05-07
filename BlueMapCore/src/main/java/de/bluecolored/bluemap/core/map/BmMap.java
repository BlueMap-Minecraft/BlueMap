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
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.HiresModelManager;
import de.bluecolored.bluemap.core.map.lowres.LowresTileManager;
import de.bluecolored.bluemap.core.map.renderstate.MapChunkState;
import de.bluecolored.bluemap.core.map.renderstate.MapTileState;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.world.World;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@DebugDump
@Getter
public class BmMap {

    private static final Gson GSON = ResourcesGson.addAdapter(new GsonBuilder())
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .registerTypeAdapter(BmMap.class, new MapSettingsSerializer())
            .create();

    private final String id;
    private final String name;
    private final World world;
    private final MapStorage storage;
    private final MapSettings mapSettings;

    private final ResourcePack resourcePack;
    private final TextureGallery textureGallery;

    private final MapTileState mapTileState;
    private final MapChunkState mapChunkState;

    private final HiresModelManager hiresModelManager;
    private final LowresTileManager lowresTileManager;

    private final ConcurrentHashMap<String, MarkerSet> markerSets;

    @Setter private Predicate<Vector2i> tileFilter;

    @Getter(AccessLevel.NONE) private long renderTimeSumNanos;
    @Getter(AccessLevel.NONE) private long tilesRendered;
    @Getter(AccessLevel.NONE) private long lastSaveTime;

    public BmMap(String id, String name, World world, MapStorage storage, ResourcePack resourcePack, MapSettings settings) throws IOException, InterruptedException {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.world = Objects.requireNonNull(world);
        this.storage = Objects.requireNonNull(storage);
        this.resourcePack = Objects.requireNonNull(resourcePack);
        this.mapSettings = Objects.requireNonNull(settings);

        Logger.global.logDebug("Loading render-state for map '" + id + "'");
        this.mapTileState = new MapTileState(storage.tileState());
        this.mapTileState.load();
        this.mapChunkState = new MapChunkState(storage.chunkState());

        if (Thread.interrupted()) throw new InterruptedException();

        Logger.global.logDebug("Loading textures for map '" + id + "'");
        this.textureGallery = loadTextureGallery();
        this.textureGallery.put(resourcePack);
        saveTextureGallery();

        this.hiresModelManager = new HiresModelManager(
                storage.hiresTiles(),
                this.resourcePack,
                this.textureGallery,
                settings,
                new Grid(settings.getHiresTileSize(), 2)
        );

        this.lowresTileManager = new LowresTileManager(
                storage,
                new Grid(settings.getLowresTileSize()),
                settings.getLodCount(),
                settings.getLodFactor()
        );

        this.tileFilter = t -> true;

        this.markerSets = new ConcurrentHashMap<>();

        this.renderTimeSumNanos = 0;
        this.tilesRendered = 0;
        this.lastSaveTime = -1;

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

    public void unrenderTile(Vector2i tile) {
        hiresModelManager.unrender(tile, lowresTileManager);
    }

    public synchronized boolean save(long minTimeSinceLastSave) {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime < minTimeSinceLastSave)
            return false;

        save();
        return true;
    }

    public synchronized void save() {
        lowresTileManager.save();
        mapTileState.save();
        mapChunkState.save();
        saveMarkerState();
        savePlayerState();
        saveMapSettings();

        // only save texture gallery if not present in storage
        try {
            if (!storage.textures().exists())
                saveTextureGallery();
        } catch (IOException e) {
            Logger.global.logError("Failed to read texture gallery for map '" + getId() + "'!", e);
        }

        lastSaveTime = System.currentTimeMillis();
    }

    private TextureGallery loadTextureGallery() throws IOException {
        try (CompressedInputStream in = storage.textures().read()){
            if (in != null)
                return TextureGallery.readTexturesFile(in.decompress());
        } catch (IOException ex) {
            Logger.global.logError("Failed to load textures for map '" + getId() + "'!", ex);
        }

        return new TextureGallery();
    }

    private void saveTextureGallery() {
        try (OutputStream out = storage.textures().write()) {
            this.textureGallery.writeTexturesFile(out);
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
                OutputStream out = storage.settings().write();
                Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)
        ) {
            GSON.toJson(this, writer);
        } catch (Exception ex) {
            Logger.global.logError("Failed to save settings for map '" + getId() + "'!", ex);
        }
    }

    public synchronized void saveMarkerState() {
        try (
                OutputStream out = storage.markers().write();
                Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)
        ) {
            MarkerGson.INSTANCE.toJson(this.markerSets, writer);
        } catch (Exception ex) {
            Logger.global.logError("Failed to save markers for map '" + getId() + "'!", ex);
        }
    }

    public synchronized void savePlayerState() {
        try (OutputStream out = storage.players().write()) {
            out.write("{}".getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            Logger.global.logError("Failed to save markers for map '" + getId() + "'!", ex);
        }
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
        if (obj instanceof BmMap that)
            return this.id.equals(that.id);
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
