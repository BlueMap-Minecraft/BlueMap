package de.bluecolored.bluemap.core.storage.compression;

import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class NoCompression implements Compression {

    @Getter private final Key key;
    @Getter private final String id;
    @Getter private final String fileSuffix;

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        return out;
    }

    @Override
    public InputStream decompress(InputStream in) throws IOException {
        return in;
    }

}
