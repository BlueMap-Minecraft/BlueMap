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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

/**
 * Represents a single Server-Sent Events (SSE) connection.
 * <p>
 * Events can be queued via {@link #enqueue(SseEvent)} without blocking.
 * Call {@link #run(OutputStream)} on the thread that owns the connection's output-stream (e.g.
 * the HTTP connection's thread) to deliver queued events to it. This will block the calling thread
 * until the connection is closed.
 */
public class SseConnection implements Closeable {

    public record SseEvent(String type, String data) {}

    // how many messages can be queued up for sending before being dropped
    private static final int QUEUE_CAPACITY = 64;

    // how long to wait for an event before sending a keepalive
    private static final long KEEPALIVE_INTERVAL_SECONDS = 30;

    private final BlockingQueue<SseEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private volatile boolean closed = false;
    private volatile Runnable onClose;
    private volatile Thread runningThread;

    public boolean isClosed() {
        return closed;
    }

    /**
     * Registers a callback that's called when this connection closes.
     * <p>
     * Returns true if the regsitration suceeded, false if the connection was already closed.
     */
    public synchronized boolean setOnClose(Runnable onClose) {
        if (closed) return false;
        this.onClose = onClose;
        return true;
    }

    /**
     * Queues an SSE event to be delivered to this connection.
     * <p>
     * If this connection's queue is full (due to a slowly-reading client), the event is
     * silently dropped and the connection will be closed.
     */
    public void enqueue(SseEvent event) {
        if (closed) return;
        if (!queue.offer(event)) {
            close();
        }
    }

    /**
     * Delivers queued events directly to {@code out}, blocking the calling thread until this
     * connection is closed either explicitly via {@link #close()}, or because writing to
     * {@code out} fails (happens if the client disconnects).
     */
    public void run(OutputStream out) throws IOException {
        runningThread = Thread.currentThread();
        SseEvent event;
        try {
            while (!closed) {
                try {
                    if (KEEPALIVE_INTERVAL_SECONDS > 0){
                        event = queue.poll(KEEPALIVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
                    } else {
                        event = queue.take();
                    }
                } catch (InterruptedException _) {
                    runningThread.interrupt();
                    break;
                }
                send(out, event);
            }
        } finally {
            close();
        }
    }

    @SneakyThrows(IOException.class)  // allows using this function in the forEach below
    private void writeLine(OutputStream out, String line) {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write one SSE event with optional data to the stream and flush it.
     * Will write a comment (:) as a keepalive if {@code event} is {@code null}.
     *
     * @throws IOException if the client has disconnected
     */
    private void send(OutputStream out, SseEvent event) throws IOException {
        if (event == null) {
            writeLine(out, ":");
        } else {
            writeLine(out, "event: " + event.type());
            event.data().lines().forEach(l -> writeLine(out, "data: " + l));
            out.write('\n');
        }
        out.flush();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (runningThread != null) runningThread.interrupt();
        if (onClose != null) onClose.run();
    }

}
