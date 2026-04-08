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
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.WatchService;
import de.bluecolored.bluemap.core.world.*;
import de.bluecolored.bluemap.core.world.mca.chunk.MCAChunkLoader;
import de.bluecolored.bluemap.core.world.mca.data.DimensionSettings;
import de.bluecolored.bluemap.core.world.mca.data.DimensionTypeDeserializer;
import de.bluecolored.bluemap.core.world.mca.data.LevelData;
import de.bluecolored.bluemap.core.world.mca.data.WorldGenSettings;
import de.bluecolored.bluemap.core.world.mca.entity.chunk.MCAEntityChunk;
import de.bluecolored.bluemap.core.world.mca.entity.chunk.MCAEntityChunkLoader;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.TypeToken;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
@ToString
public class MCAWorld implements World {

    private final String id;
    private final Path worldFolder;
    private final Key dimension;
    private final DataPack dataPack;

    private final DimensionType dimensionType;
    private final Path dimensionFolder;

    private final ChunkGrid<Chunk> blockChunkGrid;
    private final ChunkGrid<MCAEntityChunk> entityChunkGrid;

    private MCAWorld(Path worldFolder, Key dimension, DimensionType dimensionType, Path dimensionFolder, DataPack dataPack) {
        this.id = World.id(worldFolder, dimension);
        this.worldFolder = worldFolder;
        this.dimension = dimension;
        this.dimensionType = dimensionType;
        this.dimensionFolder = dimensionFolder;
        this.dataPack = dataPack;


        this.blockChunkGrid = new ChunkGrid<>(new MCAChunkLoader(this), dimensionFolder.resolve("region"));
        this.entityChunkGrid = new ChunkGrid<>(new MCAEntityChunkLoader(), dimensionFolder.resolve("entities"));

    }

    @Override
    public Grid getChunkGrid() {
        return blockChunkGrid.getChunkGrid();
    }

    @Override
    public Grid getRegionGrid() {
        return blockChunkGrid.getRegionGrid();
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
        entityChunkGrid.preloadRegionChunks(x, z, chunkFilter);
    }

    @Override
    public void invalidateChunkCache() {
        blockChunkGrid.invalidateChunkCache();
        entityChunkGrid.invalidateChunkCache();
    }

    @Override
    public void invalidateChunkCache(int x, int z) {
        blockChunkGrid.invalidateChunkCache(x, z);
        entityChunkGrid.invalidateChunkCache(x, z);
    }

    @Override
    public void iterateEntities(int minX, int minZ, int maxX, int maxZ, Consumer<Entity> entityConsumer) {
        int minChunkX = minX >> 4, minChunkZ = minZ >> 4;
        int maxChunkX = maxX >> 4, maxChunkZ = maxZ >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                Entity[] entities = entityChunkGrid.getChunk(x, z).getEntities();
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < entities.length; i++) {
                    Entity entity = entities[i];
                    Vector3d pos = entity.getPos();
                    int pX = pos.getFloorX();
                    int pZ = pos.getFloorZ();

                    if (
                            pX >= minX && pX <= maxX &&
                            pZ >= minZ && pZ <= maxZ
                    ) {
                        entityConsumer.accept(entities[i]);
                    }
                }
            }
        }
    }

    public static MCAWorld load(Path worldFolder, Key dimension, DataPack dataPack) throws IOException, InterruptedException {
        DimensionType dimensionType = loadDimensionType(worldFolder, dimension, dataPack);
        Path dimensionFolder = resolveDimensionFolder(worldFolder, dimension);
        return new MCAWorld(worldFolder, dimension, dimensionType, dimensionFolder, dataPack);
    }

    public static Path resolveDimensionFolder(Path worldFolder, Key dimension) {
        Path dimensionFolder = worldFolder.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getValue());
        if (Files.isDirectory(dimensionFolder)) return dimensionFolder;

        // try legacy format
        Path legacyDimensionFolder = legacyDimensionFolder(worldFolder, dimension);
        if (Files.isDirectory(legacyDimensionFolder.resolve("region"))) return legacyDimensionFolder;

        // might exist later
        return dimensionFolder;
    }

    private static Path legacyDimensionFolder(Path worldFolder, Key dimension) {
        if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) return worldFolder;
        if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) return worldFolder.resolve("DIM-1");
        if (DataPack.DIMENSION_THE_END.equals(dimension)) return worldFolder.resolve("DIM1");
        return worldFolder.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getValue());
    }

    public static DimensionType loadDimensionType(Path worldFolder, Key dimension, DataPack dataPack) throws IOException {
        BlueNBT blueNBT = createBlueNBTForDataPack(dataPack);
        DimensionSettings dimensionSettings = null;

        WorldGenSettings worldGenSettings = load(WorldGenSettings.class, worldFolder.resolve("data/minecraft/world_gen_settings.dat"), blueNBT);
        if (worldGenSettings != null) {
            dimensionSettings = worldGenSettings.getData().getDimensions().get(dimension.getFormatted());
        }

        if (dimensionSettings == null) {
            // try loading from the level.dat instead (old world format)
            LevelData levelData = load(LevelData.class, worldFolder.resolve("level.dat"), blueNBT);
            if (levelData != null) {
                dimensionSettings = levelData.getData().getWorldGenSettings().getDimensions().get(dimension.getFormatted());
            }
        }

        if (dimensionSettings != null) return dimensionSettings.getType();

        if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) return DimensionType.OVERWORLD;
        else if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) return DimensionType.NETHER;
        else if (DataPack.DIMENSION_THE_END.equals(dimension)) return DimensionType.END;

        Logger.global.logWarning("The world-data does not contain any info about a dimension with the id '" + dimension +
                "', using fallback.");
        return DimensionType.OVERWORLD;
    }

    private static BlueNBT createBlueNBTForDataPack(DataPack dataPack) {
        BlueNBT blueNBT = MCAUtil.addCommonNbtSettings(new BlueNBT());
        blueNBT.register(TypeToken.of(DimensionType.class), new DimensionTypeDeserializer(blueNBT, dataPack));
        return blueNBT;
    }

    private static <T> @Nullable T load(Class<T> type, Path path, BlueNBT blueNBT) throws IOException {
        if (!Files.exists(path)) return null;
        try (InputStream fileIn = Compression.GZIP.decompress(Files.newInputStream(path))) {
            return blueNBT.read(fileIn, type);
        }
    }

}
