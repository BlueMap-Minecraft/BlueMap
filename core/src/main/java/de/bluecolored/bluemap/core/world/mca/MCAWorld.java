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
package de.bluecolored.bluemap.core.world.mca;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.util.WatchService;
import de.bluecolored.bluemap.core.world.*;
import de.bluecolored.bluemap.core.world.mca.chunk.MCAChunkLoader;
import de.bluecolored.bluemap.core.world.mca.data.DimensionTypeDeserializer;
import de.bluecolored.bluemap.core.world.mca.data.LevelData;
import de.bluecolored.bluemap.core.world.mca.region.RegionType;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.TypeToken;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
@ToString
public class MCAWorld implements World {

    private static final Grid CHUNK_GRID = new Grid(16);
    private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

    private final String id;
    private final Path worldFolder;
    private final Key dimension;
    private final DataPack dataPack;
    private final LevelData levelData;

    private final DimensionType dimensionType;
    private final Vector3i spawnPoint;
    private final Path dimensionFolder;

    private final ChunkGrid<Chunk> blockChunkGrid;

    private MCAWorld(Path worldFolder, Key dimension, DataPack dataPack, LevelData levelData) {
        this.id = World.id(worldFolder, dimension);
        this.worldFolder = worldFolder;
        this.dimension = dimension;
        this.dataPack = dataPack;
        this.levelData = levelData;

        LevelData.Dimension dimensionData = levelData.getData().getWorldGenSettings().getDimensions().get(dimension.getFormatted());
        if (dimensionData == null) {
            if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) dimensionData = new LevelData.Dimension(DimensionType.OVERWORLD);
            else if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) dimensionData = new LevelData.Dimension(DimensionType.NETHER);
            else if (DataPack.DIMENSION_THE_END.equals(dimension)) dimensionData = new LevelData.Dimension(DimensionType.END);
            else {
                Logger.global.logWarning("The level-data does not contain any dimension with the id '" + dimension +
                        "', using fallback.");
                dimensionData = new LevelData.Dimension();
            }
        }

        this.dimensionType = dimensionData.getType();
        this.spawnPoint = new Vector3i(
                levelData.getData().getSpawnX(),
                levelData.getData().getSpawnY(),
                levelData.getData().getSpawnZ()
        );
        this.dimensionFolder = resolveDimensionFolder(worldFolder, dimension);

        this.blockChunkGrid = new ChunkGrid<>(new MCAChunkLoader(this), dimensionFolder.resolve("region"));

    }

    @Override
    public String getName() {
        return levelData.getData().getLevelName();
    }

    @Override
    public Grid getChunkGrid() {
        return CHUNK_GRID;
    }

    @Override
    public Grid getRegionGrid() {
        return REGION_GRID;
    }

    @Override
    public Chunk getChunkAtBlock(int x, int z) {
        return getChunk(x >> 4, z >> 4);
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return blockChunkGrid.getChunk(x, z);
    }

    @Override
    public Region<Chunk> getRegion(int x, int z) {
        return blockChunkGrid.getRegion(x, z);
    }

    @Override
    public Collection<Vector2i> listRegions() {
        return blockChunkGrid.listRegions();
    }

    @Override
    public WatchService<Vector2i> createRegionWatchService() throws IOException {
        return blockChunkGrid.createRegionWatchService();
    }

    @Override
    public void preloadRegionChunks(int x, int z, Predicate<Vector2i> chunkFilter) {
        blockChunkGrid.preloadRegionChunks(x, z, chunkFilter);
    }

    @Override
    public void invalidateChunkCache() {
        blockChunkGrid.invalidateChunkCache();
    }

    @Override
    public void invalidateChunkCache(int x, int z) {
        blockChunkGrid.invalidateChunkCache(x, z);
    }

    @Override
    public void iterateEntities(int minX, int minZ, int maxX, int maxZ, Consumer<Entity> entityConsumer) {
        //TODO
    }

    public static MCAWorld load(Path worldFolder, Key dimension, DataPack dataPack) throws IOException, InterruptedException {

        // load level.dat
        Path levelFile = worldFolder.resolve("level.dat");
        BlueNBT blueNBT = createBlueNBTForDataPack(dataPack);
        LevelData levelData;
        try (InputStream levelFileIn = Compression.GZIP.decompress(Files.newInputStream(levelFile))) {
            levelData = blueNBT.read(levelFileIn, LevelData.class);
        }

        // create world
        return new MCAWorld(worldFolder, dimension, dataPack, levelData);
    }

    public static Path resolveDimensionFolder(Path worldFolder, Key dimension) {
        if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) return worldFolder;
        if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) return worldFolder.resolve("DIM-1");
        if (DataPack.DIMENSION_THE_END.equals(dimension)) return worldFolder.resolve("DIM1");
        return worldFolder.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getValue());
    }

    private static BlueNBT createBlueNBTForDataPack(DataPack dataPack) {
        BlueNBT blueNBT = MCAUtil.addCommonNbtSettings(new BlueNBT());
        blueNBT.register(TypeToken.of(DimensionType.class), new DimensionTypeDeserializer(blueNBT, dataPack));
        return blueNBT;
    }

}
