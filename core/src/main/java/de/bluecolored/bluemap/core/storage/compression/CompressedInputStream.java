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

import de.bluecolored.bluemap.core.util.stream.DelegateInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream that is aware of the {@link Compression} that it's data is compressed with.
 */
public class CompressedInputStream extends DelegateInputStream {

    private final Compression compression;

    /**
     * Creates a new CompressedInputStream with {@link Compression#NONE} from an (uncompressed) {@link InputStream}.
     * This does <b>not</b> compress the provided InputStream.
     */
    public CompressedInputStream(InputStream in) {
        this(in, Compression.NONE);
    }

    /**
     * Creates a new CompressedInputStream from an <b>already compressed</b> {@link InputStream} and the {@link Compression}
     * it is compressed with.
     * This does <b>not</b> compress the provided InputStream.
     */
    public CompressedInputStream(InputStream in, Compression compression) {
        super(in);
        this.compression = compression;
    }

    /**
     * Returns the decompressed {@link InputStream}
     */
    public InputStream decompress() throws IOException {
        return compression.decompress(in);
    }

    /**
     * Returns the {@link Compression} this InputStream's data is compressed with
     */
    public Compression getCompression() {
        return compression;
    }

}
