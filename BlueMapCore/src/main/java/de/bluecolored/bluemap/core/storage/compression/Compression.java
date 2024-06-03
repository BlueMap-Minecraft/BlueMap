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
package de.bluecolored.bluemap.core.storage.compression;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import io.airlift.compress.zstd.ZstdInputStream;
import io.airlift.compress.zstd.ZstdOutputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public interface Compression extends Keyed {

    Compression NONE = new NoCompression(Key.bluemap("none"), "none", "");
    Compression GZIP = new BufferedCompression(Key.bluemap("gzip"), "gzip", ".gz", GZIPOutputStream::new, GZIPInputStream::new);
    Compression DEFLATE = new BufferedCompression(Key.bluemap("deflate"), "deflate", ".deflate", DeflaterOutputStream::new, InflaterInputStream::new);
    Compression ZSTD = new BufferedCompression(Key.bluemap("zstd"), "zstd", ".zst", ZstdOutputStream::new, ZstdInputStream::new);
    Compression LZ4 = new BufferedCompression(Key.bluemap("lz4"), "lz4", ".lz4", LZ4BlockOutputStream::new, LZ4BlockInputStream::new);

    Registry<Compression> REGISTRY = new Registry<>(
            NONE,
            GZIP,
            DEFLATE,
            ZSTD,
            LZ4
    );

    String getId();

    String getFileSuffix();

    OutputStream compress(OutputStream out) throws IOException;

    InputStream decompress(InputStream in) throws IOException;

}
