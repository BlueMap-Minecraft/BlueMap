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
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.HiresModelManager;
import de.bluecolored.bluemap.core.map.hires.HiresTileMeta;
import de.bluecolored.bluemap.core.map.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.MetaType;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.TileType;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.io.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@DebugDump
public class BmMap {

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
    private final LowresModelManager lowresModelManager;

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
                storage.tileStorage(id, TileType.HIRES),
                this.resourcePack,
                this.textureGallery,
                settings,
                new Grid(settings.getHiresTileSize(), 2)
        );

        this.lowresModelManager = new LowresModelManager(
                storage.tileStorage(id, TileType.LOWRES),
                new Vector2i(settings.getLowresPointsPerLowresTile(), settings.getLowresPointsPerLowresTile()),
                new Vector2i(settings.getLowresPointsPerHiresTile(), settings.getLowresPointsPerHiresTile())
        );

        this.tileFilter = t -> true;

        this.renderTimeSumNanos = 0;
        this.tilesRendered = 0;

        saveMapSettings();
    }

    public void renderTile(Vector2i tile) {
        if (!tileFilter.test(tile)) return;

        long start = System.nanoTime();

        HiresTileMeta tileMeta = hiresModelManager.render(world, tile);
        lowresModelManager.render(tileMeta);

        long end = System.nanoTime();
        long delta = end - start;

        renderTimeSumNanos += delta;
        tilesRendered ++;
    }

    public synchronized void save() {
        lowresModelManager.save();
        saveRenderState();
    }

    private void loadRenderState() throws IOException {
        Optional<CompressedInputStream> rstateData = storage.readMeta(id, MetaType.RENDER_STATE);
        if (rstateData.isPresent()) {
            try (InputStream in = rstateData.get().decompress()){
                this.renderState.load(in);
            } catch (IOException ex) {
                Logger.global.logWarning("Failed to load render-state for map '" + getId() + "': " + ex);
            }
        }
    }

    private void saveRenderState() {
        try (OutputStream out = storage.writeMeta(id, MetaType.RENDER_STATE)) {
            this.renderState.save(out);
        } catch (IOException ex){
            Logger.global.logError("Failed to save render-state for map: '" + this.id + "'!", ex);
        }
    }

    private TextureGallery loadTextureGallery() throws IOException {
        TextureGallery gallery = null;
        Optional<CompressedInputStream> texturesData = storage.readMeta(id, MetaType.TEXTURES);
        if (texturesData.isPresent()) {
            try (InputStream in = texturesData.get().decompress()){
                gallery = TextureGallery.readTexturesFile(in);
            } catch (IOException ex) {
                Logger.global.logError("Failed to load textures for map '" + getId() + "'!", ex);
            }
        }
        return gallery != null ? gallery : new TextureGallery();
    }

    private void saveTextureGallery() {
        try (OutputStream out = storage.writeMeta(id, MetaType.TEXTURES)) {
            this.textureGallery.writeTexturesFile(this.resourcePack, out);
        } catch (IOException ex) {
            Logger.global.logError("Failed to save textures for map '" + getId() + "'!", ex);
        }
    }

    private void saveMapSettings() {
        try (
                OutputStream out = storage.writeMeta(id, MetaType.SETTINGS);
                Writer writer = new OutputStreamWriter(out)
        ) {
            ResourcesGson.INSTANCE.toJson(this, writer);
        } catch (Exception ex) {
            Logger.global.logError("Failed to save settings for map '" + getId() + "'!", ex);
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

    public LowresModelManager getLowresModelManager() {
        return lowresModelManager;
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
