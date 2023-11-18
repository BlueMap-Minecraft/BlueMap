package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.region.MCARegion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

public class ChunkLoader {

    // sorted list of chunk-versions, loaders at the start of the list are preferred over loaders at the end
    private static final List<ChunkVersionLoader<?>> CHUNK_VERSION_LOADERS = List.of(
            new ChunkVersionLoader<>(Chunk_1_18.Data.class, Chunk_1_18::new, 0)
    );

    private ChunkVersionLoader<?> lastUsedLoader = CHUNK_VERSION_LOADERS.get(0);

    public MCAChunk load(MCARegion region, byte[] data, int offset, int length, Compression compression) throws IOException {
        InputStream in = new ByteArrayInputStream(data, offset, length);
        in.mark(-1);

        // try last used version
        ChunkVersionLoader<?> usedLoader = lastUsedLoader;
        MCAChunk chunk = usedLoader.load(region, compression.decompress(in));

        // check version and reload chunk if the wrong loader has been used and a better one has been found
        ChunkVersionLoader<?> actualLoader = findBestLoaderForVersion(chunk.getDataVersion());
        if (actualLoader != null && usedLoader != actualLoader) {
            in.reset(); // reset read position
            chunk = actualLoader.load(region, compression.decompress(in));
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
        private final BiFunction<MCARegion, D, MCAChunk> constructor;
        private final int dataVersion;

        public MCAChunk load(MCARegion region, InputStream in) throws IOException {
            D data = MCAUtil.BLUENBT.read(in, dataType);
            return mightSupport(data.getDataVersion()) ? constructor.apply(region, data) : new MCAChunk(region, data) {};
        }

        public boolean mightSupport(int dataVersion) {
            return dataVersion >= this.dataVersion;
        }

    }

}