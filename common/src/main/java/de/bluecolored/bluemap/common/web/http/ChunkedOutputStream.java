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
package de.bluecolored.bluemap.common.web.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Wraps an {@link OutputStream}, buffering writes and framing them as HTTP/1.1 chunks.
 * <p>
 * {@link #endChunk()} ends the current chunk without flushing the wrapped stream.
 * {@link #flush()} ends the current chunk *and* flushes the wrapped stream.
 * <p>
 * Closing this stream ends the current chunk and writes the terminating zero-length chunk, but
 * doesn't close the wrapped stream since it's expected to outlive an individual chunked response.
 * <p>
 * Any write made after this stream has been closed throws an {@link IOException} to avoid bytes
 * being written to the wrapped stream outside the required chunk framing.
 */
public class ChunkedOutputStream extends OutputStream {

    private static final int AUTO_FLUSH_CHUNK_SIZE = 1024;
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private final OutputStream out;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean closed = false;

    public ChunkedOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpen();
        buffer.write(b);
        if (buffer.size() >= AUTO_FLUSH_CHUNK_SIZE) endChunk();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureOpen();
        buffer.write(b, off, len);
        if (buffer.size() >= AUTO_FLUSH_CHUNK_SIZE) endChunk();
    }

    /**
     * Writes out any currently buffered bytes as one HTTP chunk.
     */
    public void endChunk() throws IOException {
        ensureOpen();
        if (buffer.size() > 0) {
            writeChunkHeader(buffer.size());
            buffer.writeTo(out);
            out.write(CRLF);
            buffer.reset();
        }
    }

    /**
     * Writes {@code len} bytes from {@code b} starting at {@code off} as single chunk.
     * This avoids the buffering overhead incurred by using {@code write(...)}.
     * <p>
     * Any currently buffered bytes are written with {@link #endChunk()} first.
     */
    public void writeChunk(byte[] b, int off, int len) throws IOException {
        endChunk();
        if (len > 0) {
            writeChunkHeader(len);
            out.write(b, off, len);
            out.write(CRLF);
        }
    }

    private void writeChunkHeader(int len) throws IOException {
        out.write(Integer.toHexString(len).getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }

    /**
     * Ends the current chunk and flushes the wrapped stream to push all the buffered
     * data to the client.
     */
    @Override
    public void flush() throws IOException {
        endChunk();
        out.flush();
    }

    /**
     * Ends the current chunk, writes the terminating zero-length chunk, and flushes the
     * wrapped stream (without closing it).
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        endChunk();
        closed = true;
        out.write('0');
        out.write(CRLF);
        out.write(CRLF);
        out.flush();
    }

    /**
     * @throws IOException if this stream has already been closed.
     */
    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("stream closed");
    }

}
