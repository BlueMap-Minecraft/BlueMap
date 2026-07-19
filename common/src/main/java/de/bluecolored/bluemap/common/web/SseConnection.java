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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

import lombok.SneakyThrows;

/**
 * Represents a single Server-Sent Events (SSE) connection.
 *
 * Read the events from the {@link PipedInputStream} returned from {@link #getInputStream()}.
 * Reading from the stream will block until a new event is delivered to it.
 *
 * Events are queued via {@link #enqueue(String, String)} and delivered via a virtual thread
 * owned by this connection so a slow client only blocks its own delivery.
 */
public class SseConnection implements Closeable {

    private static final int PIPE_BUFFER_SIZE = 1024;

    // how many messages can be queued up for sending (in addition to the above buffer)
    // before being dropped
    private static final int QUEUE_CAPACITY = 16;

    private final PipedOutputStream pipeOut;
    private final InputStream pipeIn;
    private final BlockingQueue<String[]> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread sendThread;
    private volatile boolean closed = false;
    private volatile Runnable onClose;

    public SseConnection() throws IOException {
        // add a hook to the pipe to close the conneciton if the stream is closed
        this.pipeOut = new PipedOutputStream();
        this.pipeIn = new FilterInputStream(new PipedInputStream(pipeOut, PIPE_BUFFER_SIZE)) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    SseConnection.this.close();
                }
            }
        };

        this.sendThread = Thread.ofVirtual().name("bluemap-sse-send").start(this::sendLoop);
    }

    /**
     * Returns an {@link InputStream} to read events from.
     * Closing it also closes this connection.
     */
    public InputStream getInputStream() {
        return pipeIn;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Registers a callback that's called when this connection closes.
     *
     * Returns true if the regsitration suceeded, false if the connection was already closed.
     */
    public synchronized boolean setOnClose(Runnable onClose) {
        if (closed) return false;
        this.onClose = onClose;
        return true;
    }

    /**
     * Queues an SSE event to be delivered to this connection.
     *
     * If this connection's queue is full (due to a slowly-reading client), the event is
     * silently dropped.
     */
    public void enqueue(String eventType, String data) {
        if (closed) return;
        // TODO: close the connection if the queue is full to force a reconnect?
        queue.offer(new String[]{eventType, data});
    }

    private void sendLoop() {
        try {
            while (!closed) {
                String[] event = queue.take();
                send(event[0], event[1]);
            }
        } catch (InterruptedException | IOException ignored) {}
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
    private synchronized void send(String eventType, String data) throws IOException {
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
        sendThread.interrupt();
        try { pipeOut.close(); } catch (IOException ignored) {}
        if (onClose != null) onClose.run();
    }

}
