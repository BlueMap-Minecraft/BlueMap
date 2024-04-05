package de.bluecolored.bluemap.core.storage.compression;

import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.*;

@RequiredArgsConstructor
public class BufferedCompression implements Compression {

    @Getter private final Key key;
    @Getter private final String id;
    @Getter private final String fileSuffix;
    private final StreamTransformer<OutputStream> compressor;
    private final StreamTransformer<InputStream> decompressor;

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        return new BufferedOutputStream(compressor.apply(out));
    }

    @Override
    public InputStream decompress(InputStream in) throws IOException {
        return new BufferedInputStream(decompressor.apply(in));
    }

    @FunctionalInterface
    public interface StreamTransformer<T> {
        T apply(T original) throws IOException;
    }

}
