package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

public class ChunkLoader {

    private final MCAWorld world;

    public ChunkLoader(MCAWorld world) {
        this.world = world;
    }

    // sorted list of chunk-versions, loaders at the start of the list are preferred over loaders at the end
    private static final List<ChunkVersionLoader<?>> CHUNK_VERSION_LOADERS = List.of(
            new ChunkVersionLoader<>(Chunk_1_18.Data.class, Chunk_1_18::new, 2844),
            new ChunkVersionLoader<>(Chunk_1_16.Data.class, Chunk_1_16::new, 2500),
            new ChunkVersionLoader<>(Chunk_1_15.Data.class, Chunk_1_15::new, 2200),
            new ChunkVersionLoader<>(Chunk_1_13.Data.class, Chunk_1_13::new, 0)
    );

    private ChunkVersionLoader<?> lastUsedLoader = CHUNK_VERSION_LOADERS.get(0);

    public MCAChunk load(byte[] data, int offset, int length, Compression compression) throws IOException {
        InputStream in = new ByteArrayInputStream(data, offset, length);
        in.mark(-1);

        // try last used version
        ChunkVersionLoader<?> usedLoader = lastUsedLoader;
        MCAChunk chunk;
        try (InputStream decompressedIn = new BufferedInputStream(compression.decompress(in))) {
            chunk = usedLoader.load(world, decompressedIn);
        }

        // check version and reload chunk if the wrong loader has been used and a better one has been found
        ChunkVersionLoader<?> actualLoader = findBestLoaderForVersion(chunk.getDataVersion());
        if (actualLoader != null && usedLoader != actualLoader) {
            in.reset(); // reset read position
            try (InputStream decompressedIn = new BufferedInputStream(compression.decompress(in))) {
                chunk = actualLoader.load(world, decompressedIn);
            }
            lastUsedLoader = actualLoader;
        }

        return chunk;
    }

    private @Nullable ChunkVersionLoader<?> findBestLoaderForVersion(int version) {
        for (ChunkVersionLoader<?> loader : CHUNK_VERSION_LOADERS) {
            if (loader.mightSupport(version)) return loader;
        }
        return null;
    }

    @RequiredArgsConstructor
    @Getter
    private static class ChunkVersionLoader<D extends MCAChunk.Data> {

        private final Class<D> dataType;
        private final BiFunction<MCAWorld, D, MCAChunk> constructor;
        private final int dataVersion;

        public MCAChunk load(MCAWorld world, InputStream in) throws IOException {
            D data = MCAUtil.BLUENBT.read(in, dataType);
            return mightSupport(data.getDataVersion()) ? constructor.apply(world, data) : new MCAChunk(world, data) {};
        }

        public boolean mightSupport(int dataVersion) {
            return dataVersion >= this.dataVersion;
        }

    }

}
