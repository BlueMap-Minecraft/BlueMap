package de.bluecolored.bluemap.core.world.mca.entity.chunk;

import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.world.mca.ChunkLoader;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MCAEntityChunkLoader implements ChunkLoader<MCAEntityChunk> {

    @Override
    public MCAEntityChunk load(byte[] data, int offset, int length, Compression compression) throws IOException {
        try (
                InputStream in = new ByteArrayInputStream(data, offset, length);
                InputStream decompressedIn = compression.decompress(in)
        ) {
            return MCAUtil.BLUENBT.read(decompressedIn, MCAEntityChunk.class);
        }
    }

    @Override
    public MCAEntityChunk emptyChunk() {
        return MCAEntityChunk.EMPTY_CHUNK;
    }

    @Override
    public MCAEntityChunk erroredChunk() {
        return MCAEntityChunk.ERRORED_CHUNK;
    }

}
