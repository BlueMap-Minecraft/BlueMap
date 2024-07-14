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
