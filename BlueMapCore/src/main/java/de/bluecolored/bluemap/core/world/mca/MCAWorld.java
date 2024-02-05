package de.bluecolored.bluemap.core.world.mca;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.chunk.ChunkLoader;
import de.bluecolored.bluemap.core.world.mca.data.LevelData;
import de.bluecolored.bluemap.core.world.mca.region.RegionType;
import lombok.Getter;
import lombok.ToString;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Getter
@ToString
@DebugDump
public class MCAWorld implements World {

    private static final Grid CHUNK_GRID = new Grid(16);
    private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

    private static final Vector2iCache VECTOR_2_I_CACHE = new Vector2iCache();

    private final String id;
    private final Path worldFolder;
    private final Key dimension;
    private final LevelData levelData;
    private final DataPack dataPack;

    private final DimensionType dimensionType;
    private final Vector3i spawnPoint;
    private final Path dimensionFolder;
    private final Path regionFolder;

    private final ChunkLoader chunkLoader = new ChunkLoader();
    private final LoadingCache<Vector2i, Region> regionCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .maximumSize(64)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(this::loadRegion);
    private final LoadingCache<Vector2i, Chunk> chunkCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .maximumSize(10240) // 10 regions worth of chunks
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(this::loadChunk);

    private MCAWorld(Path worldFolder, Key dimension, LevelData levelData, DataPack dataPack) {
        this.id = id(worldFolder, dimension);
        this.worldFolder = worldFolder;
        this.dimension = dimension;
        this.levelData = levelData;
        this.dataPack = dataPack;

        LevelData.Dimension dimensionData = levelData.getData().getWorldGenSettings().getDimensions().get(dimension.getFormatted());
        if (dimensionData == null) {
            if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) dimensionData = new LevelData.Dimension(DataPack.DIMENSION_TYPE_OVERWORLD.getFormatted());
            else if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) dimensionData = new LevelData.Dimension(DataPack.DIMENSION_TYPE_THE_NETHER.getFormatted());
            else if (DataPack.DIMENSION_THE_END.equals(dimension)) dimensionData = new LevelData.Dimension(DataPack.DIMENSION_TYPE_THE_END.getFormatted());
            else {
                Logger.global.logWarning("The level-data does not contain any dimension with the id '" + dimension +
                        "', using fallback.");
                dimensionData = new LevelData.Dimension();
            }
        }

        DimensionType dimensionType = dataPack.getDimensionType(new Key(dimensionData.getType()));
        if (dimensionType == null) {
            Logger.global.logWarning("The data-pack for world '" + worldFolder +
                    "' does not contain any dimension-type with the id '" + dimensionData.getType() + "', using fallback.");
            dimensionType = DimensionType.OVERWORLD;
        }

        this.dimensionType = dimensionType;
        this.spawnPoint = new Vector3i(
                levelData.getData().getSpawnX(),
                levelData.getData().getSpawnY(),
                levelData.getData().getSpawnZ()
        );
        this.dimensionFolder = resolveDimensionFolder(worldFolder, dimension);
        this.regionFolder = dimensionFolder.resolve("region");
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
        return getChunk(VECTOR_2_I_CACHE.get(x, z));
    }

    private Chunk getChunk(Vector2i pos) {
        return chunkCache.get(pos);
    }

    @Override
    public Region getRegion(int x, int z) {
        return getRegion(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region getRegion(Vector2i pos) {
        return regionCache.get(pos);
    }

    @Override
    public Collection<Vector2i> listRegions() {
        File[] regionFiles = getRegionFolder().toFile().listFiles();
        if (regionFiles == null) return Collections.emptyList();

        List<Vector2i> regions = new ArrayList<>(regionFiles.length);

        for (File file : regionFiles) {
            if (RegionType.forFileName(file.getName()) == null) continue;
            if (file.length() <= 0) continue;

            try {
                String[] filenameParts = file.getName().split("\\.");
                int rX = Integer.parseInt(filenameParts[1]);
                int rZ = Integer.parseInt(filenameParts[2]);

                regions.add(new Vector2i(rX, rZ));
            } catch (NumberFormatException ignore) {}
        }

        return regions;
    }

    @Override
    public void preloadRegionChunks(int x, int z) {
        try {
            getRegion(x, z).iterateAllChunks((cx, cz, chunk) -> {
                Vector2i chunkPos = VECTOR_2_I_CACHE.get(cx, cz);
                chunkCache.put(chunkPos, chunk);
            });
        } catch (IOException ex) {
            Logger.global.logDebug("Unexpected exception trying to load preload region (x:" + x + ", z:" + z + "):" + ex);
        }
    }

    @Override
    public void invalidateChunkCache() {
        chunkCache.invalidateAll();
    }

    @Override
    public void invalidateChunkCache(int x, int z) {
        chunkCache.invalidate(VECTOR_2_I_CACHE.get(x, z));
    }

    @Override
    public void cleanUpChunkCache() {
        chunkCache.cleanUp();
    }

    private Region loadRegion(Vector2i regionPos) {
        return loadRegion(regionPos.getX(), regionPos.getY());
    }

    private Region loadRegion(int x, int z) {
        return RegionType.loadRegion(this, getRegionFolder(), x, z);
    }

    private Chunk loadChunk(Vector2i chunkPos) {
        return loadChunk(chunkPos.getX(), chunkPos.getY());
    }

    private Chunk loadChunk(int x, int z) {
        final int tries = 3;
        final int tryInterval = 1000;

        Exception loadException = null;
        for (int i = 0; i < tries; i++) {
            try {
                return getRegion(x >> 5, z >> 5)
                        .loadChunk(x, z);
            } catch (IOException | RuntimeException e) {
                if (loadException != null) e.addSuppressed(loadException);
                loadException = e;

                if (i + 1 < tries) {
                    try {
                        Thread.sleep(tryInterval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        Logger.global.logDebug("Unexpected exception trying to load chunk (x:" + x + ", z:" + z + "):" + loadException);
        return Chunk.EMPTY_CHUNK;
    }

    public static MCAWorld load(Path worldFolder, Key dimension) throws IOException, InterruptedException {
        // load level.dat
        Path levelFile = worldFolder.resolve("level.dat");
        InputStream levelFileIn = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(levelFile)));
        LevelData levelData = MCAUtil.BLUENBT.read(levelFileIn, LevelData.class);

        // load datapacks
        DataPack dataPack = new DataPack();
        Path dataPackFolder = worldFolder.resolve("datapacks");
        if (Files.exists(dataPackFolder)) {
            List<Path> roots;
            try (var stream = Files.list(dataPackFolder)) {
                roots = stream
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());
            }
            for (Path root : roots) {
                dataPack.load(root);
            }
        }
        dataPack.bake();

        // create world
        return new MCAWorld(worldFolder, dimension, levelData, dataPack);
    }

    public static String id(Path worldFolder, Key dimension) {
        worldFolder = worldFolder.toAbsolutePath().normalize();

        Path workingDir = Path.of("").toAbsolutePath().normalize();
        if (worldFolder.startsWith(workingDir))
            worldFolder = workingDir.relativize(worldFolder);

        return "MCA#" + worldFolder + "#" + dimension.getFormatted();
    }

    public static Path resolveDimensionFolder(Path worldFolder, Key dimension) {
        if (DataPack.DIMENSION_OVERWORLD.equals(dimension)) return worldFolder;
        if (DataPack.DIMENSION_THE_NETHER.equals(dimension)) return worldFolder.resolve("DIM-1");
        if (DataPack.DIMENSION_THE_END.equals(dimension)) return worldFolder.resolve("DIM1");
        return worldFolder.resolve("dimensions").resolve(dimension.getNamespace()).resolve(dimension.getValue());
    }

}
