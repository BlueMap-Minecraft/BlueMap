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
package de.bluecolored.bluemap.core.storage;

import io.airlift.compress.zstd.ZstdInputStream;
import io.airlift.compress.zstd.ZstdOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum Compression {

    NONE("none", "", out -> out, in -> in),
    GZIP("gzip", ".gz", GZIPOutputStream::new, GZIPInputStream::new),
    ZLIB("zlib", ".zlib", DeflaterOutputStream::new, DeflaterInputStream::new),
    ZSTD("zstd", ".zst", ZstdOutputStream::new, ZstdInputStream::new);

    private final String typeId;
    private final String fileSuffix;
    private final StreamTransformer<OutputStream> compressor;
    private final StreamTransformer<InputStream> decompressor;

    Compression(String typeId, String fileSuffix,
                StreamTransformer<OutputStream> compressor,
                StreamTransformer<InputStream> decompressor) {
        this.fileSuffix = fileSuffix;
        this.typeId = typeId;
        this.compressor = compressor;
        this.decompressor = decompressor;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public OutputStream compress(OutputStream out) throws IOException {
        return compressor.apply(out);
    }

    public InputStream decompress(InputStream in) throws IOException {
        return decompressor.apply(in);
    }

    public static Compression forTypeId(String id) {
        for (Compression compression : values()) {
            if (compression.typeId.equals(id)) return compression;
        }

        throw new NoSuchElementException("There is no Compression with type-id: " + id);
    }

    @FunctionalInterface
    private interface StreamTransformer<T> {
        T apply(T original) throws IOException;
    }

}
