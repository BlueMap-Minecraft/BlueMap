package de.bluecolored.bluemap.core.resources.resourcepack;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resources.BlockPropertiesConfig;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.biome.BiomeConfig;
import de.bluecolored.bluemap.core.resources.resourcepack.blockmodel.BlockModel;
import de.bluecolored.bluemap.core.resources.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockProperties;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

@DebugDump
public class ResourcePack {
    public static final ResourcePath<BlockState> MISSING_BLOCK_STATE = new ResourcePath<>("bluemap", "missing");
    public static final ResourcePath<BlockModel> MISSING_BLOCK_MODEL = new ResourcePath<>("bluemap", "missing");
    public static final ResourcePath<Texture> MISSING_TEXTURE = new ResourcePath<>("bluemap", "missing");

    private final Map<String, ResourcePath<BlockState>> blockStatePaths;
    private final Map<ResourcePath<BlockState>, BlockState> blockStates;
    private final Map<ResourcePath<BlockModel>, BlockModel> blockModels;
    private final Map<ResourcePath<Texture>, Texture> textures;
    private final Map<ResourcePath<BufferedImage>, BufferedImage> colormaps;

    private final BlockColorCalculatorFactory colorCalculatorFactory;
    private final BiomeConfig biomeConfig;
    private final BlockPropertiesConfig blockPropertiesConfig;

    private final LoadingCache<de.bluecolored.bluemap.core.world.BlockState, BlockProperties> blockPropertiesCache;

    public ResourcePack() {
        this.blockStatePaths = new HashMap<>();
        this.blockStates = new HashMap<>();
        this.blockModels = new HashMap<>();
        this.textures = new HashMap<>();
        this.colormaps = new HashMap<>();

        this.colorCalculatorFactory = new BlockColorCalculatorFactory();
        this.biomeConfig = new BiomeConfig();
        this.blockPropertiesConfig = new BlockPropertiesConfig();

        this.blockPropertiesCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .maximumSize(10000)
                .build(this::loadBlockProperties);
    }

    @Nullable
    public BlockState getBlockState(de.bluecolored.bluemap.core.world.BlockState blockState) {
        ResourcePath<BlockState> path = blockStatePaths.get(blockState.getFormatted());
        return path != null ? path.getResource(this::getBlockState) : MISSING_BLOCK_STATE.getResource(this::getBlockState);
    }

    @Nullable
    public BlockState getBlockState(ResourcePath<BlockState> path) {
        BlockState blockState = blockStates.get(path);
        return blockState != null ? blockState : MISSING_BLOCK_STATE.getResource(blockStates::get);
    }

    public Map<ResourcePath<BlockState>, BlockState> getBlockStates() {
        return blockStates;
    }

    @Nullable
    public BlockModel getBlockModel(ResourcePath<BlockModel> path) {
        BlockModel blockModel = blockModels.get(path);
        return blockModel != null ? blockModel : MISSING_BLOCK_MODEL.getResource(blockModels::get);
    }

    public Map<ResourcePath<BlockModel>, BlockModel> getBlockModels() {
        return blockModels;
    }

    @Nullable
    public Texture getTexture(ResourcePath<Texture> path) {
        Texture texture = textures.get(path);
        return texture != null ? texture : MISSING_TEXTURE.getResource(textures::get);
    }

    public Map<ResourcePath<Texture>, Texture> getTextures() {
        return textures;
    }

    public BlockColorCalculatorFactory getColorCalculatorFactory() {
        return colorCalculatorFactory;
    }

    public Biome getBiome(String formatted) {
        return biomeConfig.getBiome(formatted);
    }

    public BlockProperties getBlockProperties(de.bluecolored.bluemap.core.world.BlockState state) {
        return blockPropertiesCache.get(state);
    }

    private BlockProperties loadBlockProperties(de.bluecolored.bluemap.core.world.BlockState state) {
        BlockProperties.Builder props = blockPropertiesConfig.getBlockProperties(state).toBuilder();

        if (props.isOccluding() == Tristate.UNDEFINED || props.isCulling() == Tristate.UNDEFINED) {
            BlockState resource = getBlockState(state);
            if (resource != null) {
                resource.forEach(state,0, 0, 0, variant -> {
                    BlockModel model = variant.getModel().getResource(this::getBlockModel);
                    if (model != null) {
                        if (props.isOccluding() == Tristate.UNDEFINED) props.occluding(model.isOccluding());
                        if (props.isCulling() == Tristate.UNDEFINED) props.culling(model.isCulling());
                    }
                });
            }
        }

        return props.build();
    }

    public synchronized void loadResources(Path root) throws IOException {
        Logger.global.logInfo("Loading resources from: " + root);

        if (!Files.isDirectory(root)) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(root, (ClassLoader) null)) {
                for (Path fsRoot : fileSystem.getRootDirectories()) {
                    if (!Files.isDirectory(fsRoot)) continue;
                    this.loadResources(fsRoot);
                }
            }
            return;
        }

        try {
            // do those in parallel
            CompletableFuture.allOf(

                    // load blockstates
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("blockstates"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, () -> {
                                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                                        return ResourcesGson.INSTANCE.fromJson(reader, BlockState.class);
                                    }
                                }, blockStates));
                    }),

                    // load blockmodels
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("models").resolve("block"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, () -> {
                                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                                        return ResourcesGson.INSTANCE.fromJson(reader, BlockModel.class);
                                    }
                                }, blockModels));
                    }),

                    // load textures
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("textures").resolve("block"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".png"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, () -> {
                                    ResourcePath<Texture> resourcePath = new ResourcePath<>(root.relativize(file));
                                    try (InputStream in = Files.newInputStream(file)) {
                                        return Texture.from(resourcePath, ImageIO.read(in));
                                    }
                                }, textures));
                    }),

                    // load colormaps
                    CompletableFuture.runAsync(() -> {
                        walk(root.resolve("assets").resolve("minecraft").resolve("textures").resolve("colormap"))
                                .filter(path -> path.getFileName().toString().endsWith(".png"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, () -> {
                                    try (InputStream in = Files.newInputStream(file)) {
                                        return ImageIO.read(in);
                                    }
                                }, colormaps));
                    }),

                    // load block-color configs
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("blockColors.json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> {
                                    try {
                                        colorCalculatorFactory.load(file);
                                    } catch (Exception ex) {
                                        Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
                                    }
                                });
                    }),

                    // load biome configs
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("biomes.json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> {
                                    try {
                                        biomeConfig.load(file);
                                    } catch (Exception ex) {
                                        Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
                                    }
                                });

                        list(root.resolve("data"))
                                .filter(Files::isDirectory)
                                .forEach(namespace -> list(namespace.resolve("worldgen").resolve("biome"))
                                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                                        .filter(Files::isRegularFile)
                                        .forEach(file -> {
                                            try {
                                                biomeConfig.loadDatapackBiome(namespace.getFileName().toString(), file);
                                            } catch (Exception ex) {
                                                Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
                                            }
                                        })
                                );
                    }),

                    // load block-properties configs
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("blockProperties.json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> {
                                    try {
                                        blockPropertiesConfig.load(file);
                                    } catch (Exception ex) {
                                        Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
                                    }
                                });
                    })

            ).join();

        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause != null) throw new IOException(cause);
            throw new IOException(ex);
        }
    }

    public synchronized void bake() throws IOException {
        Logger.global.logInfo("Baking resources...");

        for (ResourcePath<BlockState> path : blockStates.keySet()) {
            blockStatePaths.put(path.getFormatted(), path);
        }

        for (BlockModel model : blockModels.values()) {
            model.applyParent(this);
            model.calculateProperties(this);
        }

        BufferedImage foliage = new ResourcePath<BufferedImage>("minecraft:colormap/foliage").getResource(colormaps::get);
        if (foliage == null) throw new IOException("Failed to bake resource-pack: No foliage-colormap found!");
        this.colorCalculatorFactory.setFoliageMap(foliage);

        BufferedImage grass = new ResourcePath<BufferedImage>("minecraft:colormap/grass").getResource(colormaps::get);
        if (grass == null) throw new IOException("Failed to bake resource-pack: No grass-colormap found!");
        this.colorCalculatorFactory.setGrassMap(grass);
    }

    private <T> void loadResource(Path root, Path file, Loader<T> loader, Map<ResourcePath<T>, T> resultMap) {
        try {
            ResourcePath<T> resourcePath = new ResourcePath<>(root.relativize(file));
            if (resultMap.containsKey(resourcePath)) return; // don't load already present resources

            T resource = loader.load();
            resourcePath.setResource(resource);
            resultMap.put(resourcePath, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
        }
    }

    private static Stream<Path> list(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        try {
            return Files.list(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    private static Stream<Path> walk(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        try {
            return Files.walk(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    private interface Loader<T> {
        T load() throws IOException;
    }

}