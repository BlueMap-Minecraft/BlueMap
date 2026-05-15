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
package de.bluecolored.bluemap.common.web;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;

/**
 * Represents a single Server-Sent Events (SSE) connection.
 *
 * Read the events from the {@link PipedInputStream} returned from {@link #getInputStream()}.
 * Reading from the stream will block until a new event is sent to it via {@link #send(String, String)}.
 * Sending an event will flush the stream, ensuring that all events can be read immediately.
 */
public class SseConnection implements Closeable {

    private static final int PIPE_BUFFER_SIZE = 4096;

    private final PipedOutputStream pipeOut;
    private final PipedInputStream pipeIn;
    private volatile boolean closed = false;

    public SseConnection() throws IOException {
        this.pipeOut = new PipedOutputStream();
        this.pipeIn = new PipedInputStream(pipeOut, PIPE_BUFFER_SIZE);
    }

    public InputStream getInputStream() {
        return pipeIn;
    }

    public boolean isClosed() {
        return closed;
    }

    @SneakyThrows(IOException.class)  // allows using this function in the forEach below
    private void writeLine(String line){
        pipeOut.write((line + "\n").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write one SSE event with optional data to the stream and flush it.
     *
     * @throws IOException if the connection is closed or the client has disconnected
     */
    public synchronized void send(String eventType, String data) throws IOException {
        if (closed) throw new IOException("SSE connection is closed");
        try {
            writeLine("event: " + eventType);
            data.lines().forEach(l -> writeLine("data: " + l));
            pipeOut.write('\n');
            pipeOut.flush();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try { pipeOut.close(); } catch (IOException ignored) {}
    }

}
