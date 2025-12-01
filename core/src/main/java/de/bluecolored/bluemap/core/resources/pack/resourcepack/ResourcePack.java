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
import de.bluecolored.bluemap.core.resources.pack.PackVersion;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas.Atlas;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.EntityState;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.world.BlockProperties;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ResourcePack extends Pack {

    public interface Extension<T extends ResourcePackExtension> extends Keyed {
        Registry<Extension<?>> REGISTRY = new Registry<>();
        T create(ResourcePack pack);
    }

    public static final ResourcePath<BlockState> MISSING_BLOCK_STATE = new ResourcePath<>("bluemap", "missing");
    public static final ResourcePath<EntityState> MISSING_ENTITY_STATE = new ResourcePath<>("bluemap", "missing");
    public static final ResourcePath<Model> MISSING_BLOCK_MODEL = new ResourcePath<>("bluemap", "block/missing");
    public static final ResourcePath<Model> MISSING_ENTITY_MODEL = new ResourcePath<>("bluemap", "entity/missing");
    public static final ResourcePath<Texture> MISSING_TEXTURE = new ResourcePath<>("bluemap", "block/missing");

    private static final Key BLOCKS_ATLAS = Key.minecraft("blocks");

    @Getter private final ResourcePool<Atlas> atlases;
    @Getter private final ResourcePool<BlockState> blockStates;
    @Getter private final ResourcePool<EntityState> entityStates;
    @Getter private final ResourcePool<Model> models;
    @Getter private final ResourcePool<Texture> textures;
    @Getter private final ResourcePool<BufferedImage> colormaps;

    @Getter private final BlockColorCalculatorFactory colorCalculatorFactory;
    private final BlockPropertiesConfig blockPropertiesConfig;

    private final LoadingCache<de.bluecolored.bluemap.core.world.BlockState, BlockState> blockStateCache;
    private final LoadingCache<de.bluecolored.bluemap.core.world.BlockState, BlockProperties> blockPropertiesCache;

    private final Map<Extension<?>, ResourcePackExtension> extensions;

    public ResourcePack(PackVersion packVersion) {
        super(packVersion);

        this.atlases = new ResourcePool<>();
        this.blockStates = new ResourcePool<>();
        this.entityStates = new ResourcePool<>();
        this.models = new ResourcePool<>();
        this.textures = new ResourcePool<>();
        this.colormaps = new ResourcePool<>();

        this.colorCalculatorFactory = new BlockColorCalculatorFactory();
        this.blockPropertiesConfig = new BlockPropertiesConfig();

        this.blockStateCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .maximumSize(10000)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(this::loadBlockState);
        this.blockPropertiesCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .maximumSize(10000)
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(this::loadBlockProperties);

        this.extensions = new HashMap<>();
        for (Extension<?> extensionType : Extension.REGISTRY.values())
            extensions.put(extensionType, extensionType.create(this));
    }

    @Override
    public synchronized void loadResources(Iterable<Path> roots) throws IOException, InterruptedException {
        Logger.global.logInfo("Loading resources...");

        // resources
        for (Path root : roots) {
            if (Thread.interrupted()) throw new InterruptedException();
            Logger.global.logDebug("Loading resources from: " + root + " ...");
            loadResourcePath(root, this::loadResources);
        }

        // extensions
        for (var extension : extensions.entrySet()) {
            if (Thread.interrupted()) throw new InterruptedException();
            Logger.global.logDebug("Loading extension: " + extension.getKey().getKey());
            extension.getValue().loadResources(roots);
        }

        // texture filter
        Logger.global.logDebug("Collecting texture-keys...");
        Set<Key> usedTextureKeys = collectUsedTextureKeys();
        Logger.global.logDebug("Found " +  usedTextureKeys.size() + " texture-keys.");

        // textures
        for (Path root : roots) {
            if (Thread.interrupted()) throw new InterruptedException();
            Logger.global.logDebug("Loading textures from: " + root + " ...");
            loadResourcePath(root, path -> getBlocksAtlas().load(path, textures, usedTextureKeys::contains));
        }

        // bake
        if (Thread.interrupted()) throw new InterruptedException();
        Logger.global.logDebug("Baking resources...");
        bake(usedTextureKeys::contains);

        // bake extensions
        for (var extension : extensions.entrySet()) {
            if (Thread.interrupted()) throw new InterruptedException();
            Logger.global.logDebug("Baking extension: " + extension.getKey().getKey());
            extension.getValue().bake();
        }

        Logger.global.logInfo("Resources loaded.");
    }

    private void loadResources(Path root) throws IOException {
        try {
            // do those in parallel
            CompletableFuture.allOf(

                    // load atlases
                    CompletableFuture.runAsync(() -> {
                    list(root.resolve("assets"))
                            .map(path -> path.resolve("atlases"))
                            .filter(Files::isDirectory)
                            .flatMap(Pack::walk)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .filter(Files::isRegularFile)
                            .forEach(file -> atlases.load(
                                    new ResourcePath<>(root.relativize(file), 1, 3),
                                    key -> {
                                        try (BufferedReader reader = Files.newBufferedReader(file)) {
                                            return ResourcesGson.INSTANCE.fromJson(reader, Atlas.class);
                                        }
                                    },
                                    Atlas::add
                            ));
                    }, BlueMap.THREAD_POOL),

                    // load blockstates
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("blockstates"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> blockStates.load(
                                        new ResourcePath<>(root.relativize(file), 1, 3),
                                        key -> {
                                            try (BufferedReader reader = Files.newBufferedReader(file)) {
                                                return ResourcesGson.INSTANCE.fromJson(reader, BlockState.class);
                                            }
                                        }
                                ));
                    }, BlueMap.THREAD_POOL),

                    // load entitystates
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("entitystates"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> entityStates.load(
                                        new ResourcePath<>(root.relativize(file), 1, 3),
                                        key -> {
                                            try (BufferedReader reader = Files.newBufferedReader(file)) {
                                                return ResourcesGson.INSTANCE.fromJson(reader, EntityState.class);
                                            }
                                        }
                                ));
                    }, BlueMap.THREAD_POOL),

                    // load models
                    CompletableFuture.runAsync(() -> {
                        list(root.resolve("assets"))
                                .map(path -> path.resolve("models"))
                                .filter(Files::isDirectory)
                                .flatMap(ResourcePack::walk)
                                .filter(path -> path.getFileName().toString().endsWith(".json"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> models.load(
                                        new ResourcePath<>(root.relativize(file), 1, 3),
                                        key -> {
                                            try (BufferedReader reader = Files.newBufferedReader(file)) {
                                                return ResourcesGson.INSTANCE.fromJson(reader, Model.class);
                                            }
                                        }
                                ));
                    }, BlueMap.THREAD_POOL),

                    // load colormaps
                    CompletableFuture.runAsync(() -> {
                        walk(root.resolve("assets").resolve("minecraft").resolve("textures").resolve("colormap"))
                                .filter(path -> path.getFileName().toString().endsWith(".png"))
                                .filter(Files::isRegularFile)
                                .forEach(file -> colormaps.load(
                                        new ResourcePath<>(root.relativize(file), 1, 3),
                                        key -> {
                                            try (InputStream in = Files.newInputStream(file)) {
                                                return ImageIO.read(in);
                                            }
                                        }
                                ));
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
            if (cause instanceof IOException ioEx) throw ioEx;
            if (cause != null) throw new IOException(cause);
            throw new IOException(ex);
        }

    }

    private void bake(Predicate<Key> textureFilter) throws IOException, InterruptedException {

        // bake textures
        getBlocksAtlas().bake(textures, textureFilter);

        if (Thread.interrupted()) throw new InterruptedException();

        // optimize references
        for (Model model : models.values()) {
            model.optimize(textures);
        }

        if (Thread.interrupted()) throw new InterruptedException();

        // apply model parents
        for (Model model : models.values()) {
            model.applyParent(models);
        }

        if (Thread.interrupted()) throw new InterruptedException();

        // calculate model properties
        for (Model model : models.values()) {
            model.calculateProperties(textures);
        }

        BufferedImage foliage = new ResourcePath<BufferedImage>("minecraft:colormap/foliage").getResource(colormaps::get);
        if (foliage != null)
            this.colorCalculatorFactory.setFoliageMap(foliage);

        BufferedImage dryFoliage = new ResourcePath<BufferedImage>("minecraft:colormap/dry_foliage").getResource(colormaps::get);
        if (dryFoliage != null)
            this.colorCalculatorFactory.setDryFoliageMap(dryFoliage);

        BufferedImage grass = new ResourcePath<BufferedImage>("minecraft:colormap/grass").getResource(colormaps::get);
        if (grass != null)
            this.colorCalculatorFactory.setGrassMap(grass);

    }

    private Set<Key> collectUsedTextureKeys() {
        Set<Key> usedTextures = new HashSet<>();
        usedTextures.add(MISSING_TEXTURE);
        for (Model model : models.values()) {
            for (TextureVariable textureVariable : model.getTextures().values()) {
                if (textureVariable.isReference()) continue;
                usedTextures.add(textureVariable.getTexturePath());
            }
        }
        for (ResourcePackExtension extension : extensions.values()) {
            usedTextures.addAll(extension.collectUsedTextureKeys());
        }
        return usedTextures;
    }

    private Atlas getBlocksAtlas() {
        Atlas blocksAtlas = atlases.get(BLOCKS_ATLAS);
        if (blocksAtlas != null) return blocksAtlas;

        Logger.global.noFloodWarning("blocks-atlas-missing", "Atlas " + BLOCKS_ATLAS + " is missing or got accessed before loaded!");
        return new Atlas();
    }

    public BlockState getBlockState(de.bluecolored.bluemap.core.world.BlockState blockState) {
        return blockStateCache.get(blockState);
    }

    private BlockState loadBlockState(de.bluecolored.bluemap.core.world.BlockState blockState) {
        Key key = blockState.getId();
        for (ResourcePackExtension extension : extensions.values()) {
            key = extension.getBlockStateKey(key);
        }
        return blockStates.get(key);
    }

    public BlockProperties getBlockProperties(de.bluecolored.bluemap.core.world.BlockState state) {
        return blockPropertiesCache.get(state);
    }

    private BlockProperties loadBlockProperties(de.bluecolored.bluemap.core.world.BlockState state) {
        BlockProperties.Builder props = BlockProperties.builder();

        // collect properties from extensions
        for (ResourcePackExtension extension : extensions.values()) {
            extension.getBlockProperties(state, props);
        }

        // explicitly configured properties always have priority -> overwrite
        props.from(blockPropertiesConfig.getBlockProperties(state));

        // calculate culling and occlusion from model if UNDEFINED
        if (props.isOccluding() == Tristate.UNDEFINED || props.isCulling() == Tristate.UNDEFINED) {
            BlockState resource = getBlockState(state);
            if (resource != null) {
                resource.forEach(state,0, 0, 0, variant -> {
                    Model model = variant.getModel().getResource(models::get);
                    if (model != null) {
                        if (props.isOccluding() == Tristate.UNDEFINED) props.occluding(model.isOccluding());
                        if (props.isCulling() == Tristate.UNDEFINED) props.culling(model.isCulling());
                    }
                });
            }
        }

        return props.build();
    }

    @SuppressWarnings({"unchecked", "unused"})
    public <T extends ResourcePackExtension> T getExtension(Extension<T> extensionType) {
        return (T) extensions.get(extensionType);
    }

}
