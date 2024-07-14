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
package de.bluecolored.bluemap.core.resources.pack.resourcepack;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.resources.BlockPropertiesConfig;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.Pack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockmodel.BlockModel;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockmodel.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.AnimationMeta;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.world.BlockProperties;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ResourcePack extends Pack {
    public static final ResourcePath<BlockState> MISSING_BLOCK_STATE = new ResourcePath<>("bluemap", "missing");
    public static final ResourcePath<BlockModel> MISSING_BLOCK_MODEL = new ResourcePath<>("bluemap", "block/missing");
    public static final ResourcePath<Texture> MISSING_TEXTURE = new ResourcePath<>("bluemap", "block/missing");

    private final Map<ResourcePath<BlockState>, BlockState> blockStates;
    private final Map<ResourcePath<BlockModel>, BlockModel> blockModels;
    private final Map<ResourcePath<Texture>, Texture> textures;

    private final Map<ResourcePath<BufferedImage>, BufferedImage> colormaps;
    private final BlockColorCalculatorFactory colorCalculatorFactory;
    private final BlockPropertiesConfig blockPropertiesConfig;

    private final Map<String, ResourcePath<BlockState>> blockStatePaths;
    private final Map<String, ResourcePath<Texture>> texturePaths;
    private final LoadingCache<de.bluecolored.bluemap.core.world.BlockState, BlockProperties> blockPropertiesCache;

    public ResourcePack(int packVersion) {
        super(packVersion);

        this.blockStatePaths = new HashMap<>();
        this.blockStates = new HashMap<>();
        this.blockModels = new HashMap<>();
        this.texturePaths = new HashMap<>();
        this.textures = new HashMap<>();
        this.colormaps = new HashMap<>();

        this.colorCalculatorFactory = new BlockColorCalculatorFactory();
        this.blockPropertiesConfig = new BlockPropertiesConfig();

        this.blockPropertiesCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .maximumSize(10000)
                .build(this::loadBlockProperties);
    }

    public synchronized void loadResources(Iterable<Path> roots) throws IOException, InterruptedException {
        Logger.global.logInfo("Loading resources...");

        for (Path root : roots) {
            if (Thread.interrupted()) throw new InterruptedException();

            Logger.global.logDebug("Loading resources from: " + root + " ...");
            loadResourcePath(root, this::loadResources);
        }

        Logger.global.logInfo("Loading textures...");
        for (Path root : roots) {
            if (Thread.interrupted()) throw new InterruptedException();

            Logger.global.logDebug("Loading textures from: " + root + " ...");
            loadResourcePath(root, this::loadTextures);
        }

        if (Thread.interrupted()) throw new InterruptedException();

        Logger.global.logInfo("Baking resources...");
        bake();

        Logger.global.logInfo("Resources loaded.");
    }

    private void loadResources(Path root) throws IOException {
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
                                .forEach(file -> loadResource(root, file, 1, 3, key -> {
                                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                                        return ResourcesGson.INSTANCE.fromJson(reader, BlockState.class);
                                    }
                                }, blockStates));
                    }, BlueMap.THREAD_POOL),

                    // load blockmodels
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("models"))
                                .flatMap(ResourcePack::list)
                                .filter(path -> !path.getFileName().toString().equals("item"))
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, 1, 3, key -> {
                                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                                        return ResourcesGson.INSTANCE.fromJson(reader, BlockModel.class);
                                    }
                                }, blockModels));
                    }, BlueMap.THREAD_POOL),

                    // load colormaps
                    CompletableFuture.runAsync(() -> {
                        walk(root.resolve("assets").resolve("minecraft").resolve("textures").resolve("colormap"))
                                .filter(path -> path.getFileName().toString().endsWith(".png"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> loadResource(root, file, 1, 3, key -> {
                                    try (InputStream in = Files.newInputStream(file)) {
                                        return ImageIO.read(in);
                                    }
                                }, colormaps));
                    }, BlueMap.THREAD_POOL),

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
                    }, BlueMap.THREAD_POOL),

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
                    }, BlueMap.THREAD_POOL)

            ).join();

        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause != null) throw new IOException(cause);
            throw new IOException(ex);
        }
    }

    private void loadTextures(Path root) throws IOException {
        try {

            // collect all used textures
            Set<ResourcePath<Texture>> usedTextures = new HashSet<>();
            usedTextures.add(MISSING_TEXTURE);
            for (BlockModel model : blockModels.values()) {
                for (TextureVariable textureVariable : model.getTextures().values()) {
                    if (textureVariable.isReference()) continue;
                    usedTextures.add(textureVariable.getTexturePath());
                }
            }

            // load textures
            list(root.resolve("assets"))
                    .map(path -> path.resolve("textures"))
                    .flatMap(ResourcePack::walk)
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> loadResource(root, file, 1, 3, key -> {
                        if (!usedTextures.contains(key)) return null; // don't load unused textures

                        // load image
                        BufferedImage image;
                        try (InputStream in = Files.newInputStream(file)) {
                            image = ImageIO.read(in);
                        }

                        // load animation
                        AnimationMeta animation = null;
                        Path animationPathFile = file.resolveSibling(file.getFileName() + ".mcmeta");
                        if (Files.exists(animationPathFile)) {
                            try (Reader in = Files.newBufferedReader(animationPathFile, StandardCharsets.UTF_8)) {
                                animation = ResourcesGson.INSTANCE.fromJson(in, AnimationMeta.class);
                            }
                        }

                        return Texture.from(key, image, animation);
                    }, textures));

        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause != null) throw new IOException(cause);
            throw new IOException(ex);
        }
    }

    private void bake() throws IOException, InterruptedException {

        // fill path maps
        blockStates.keySet().forEach(path -> blockStatePaths.put(path.getFormatted(), path));
        textures.keySet().forEach(path -> texturePaths.put(path.getFormatted(), path));

        // optimize references
        for (BlockModel model : blockModels.values()) {
            model.optimize(this);
        }

        if (Thread.interrupted()) throw new InterruptedException();

        // apply model parents
        for (BlockModel model : blockModels.values()) {
            model.applyParent(this);
        }

        if (Thread.interrupted()) throw new InterruptedException();

        // calculate model properties
        for (BlockModel model : blockModels.values()) {
            model.calculateProperties(this);
        }

        BufferedImage foliage = new ResourcePath<BufferedImage>("minecraft:colormap/foliage").getResource(colormaps::get);
        if (foliage == null) throw new IOException("Failed to bake resource-pack: No foliage-colormap found!");
        this.colorCalculatorFactory.setFoliageMap(foliage);

        BufferedImage grass = new ResourcePath<BufferedImage>("minecraft:colormap/grass").getResource(colormaps::get);
        if (grass == null) throw new IOException("Failed to bake resource-pack: No grass-colormap found!");
        this.colorCalculatorFactory.setGrassMap(grass);

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

    @Nullable
    public BlockModel getBlockModel(ResourcePath<BlockModel> path) {
        BlockModel blockModel = blockModels.get(path);
        return blockModel != null ? blockModel : MISSING_BLOCK_MODEL.getResource(blockModels::get);
    }

    @Nullable
    public ResourcePath<Texture> getTexturePath(String formatted) {
        return texturePaths.get(formatted);
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

}
