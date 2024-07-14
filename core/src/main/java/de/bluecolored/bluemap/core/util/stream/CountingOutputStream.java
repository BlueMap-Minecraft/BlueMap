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
package de.bluecolored.bluemap.core.util.stream;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends DelegateOutputStream {

    private long count;

    public CountingOutputStream(OutputStream out) {
        this(out, 0);
    }

    public CountingOutputStream(OutputStream out, int initialCount) {
        super(out);
        this.count = initialCount;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        count ++;
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        out.write(b);
        count += b.length;
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }

    public long getCount() {
        return count;
    }

    @Override
    public void close() throws IOException {
        count = 0;
        super.close();
    }

}
