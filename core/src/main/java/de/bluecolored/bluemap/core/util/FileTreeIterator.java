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
package de.bluecolored.bluemap.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Adapted version of java.nio.file.FileTreeIterator.
 * This version ignores NoSuchFileException if they occur while iterating the file-tree.
 * Required to implement FileHelper#walk
 */
class FileTreeIterator implements Iterator<FileTreeWalker.Event>, Closeable {
    private final FileTreeWalker walker;
    private FileTreeWalker.Event next;

    /**
     * Creates a new iterator to walk the file tree starting at the given file.
     *
     * @throws  IllegalArgumentException
     *          if {@code maxDepth} is negative
     * @throws  IOException
     *          if an I/O errors occurs opening the starting file
     * @throws  SecurityException
     *          if the security manager denies access to the starting file
     * @throws  NullPointerException
     *          if {@code start} or {@code options} is {@code null} or
     *          the options array contains a {@code null} element
     */
    FileTreeIterator(Path start, int maxDepth, FileVisitOption... options)
            throws IOException
    {
        this.walker = new FileTreeWalker(List.of(options), maxDepth);
        this.next = walker.walk(start);
        assert next.type() == FileTreeWalker.EventType.ENTRY ||
                next.type() == FileTreeWalker.EventType.START_DIRECTORY;

        // IOException if there is a problem accessing the starting file
        IOException ioe = next.ioeException();
        if (ioe != null)
            throw ioe;
    }

    private void fetchNextIfNeeded() {
        if (next == null) {
            FileTreeWalker.Event ev = walker.next();
            while (ev != null) {
                IOException ioe = ev.ioeException();
                if (ioe != null) {
                    if (ioe instanceof NoSuchFileException)
                        continue; // ignore NoSuchFileExceptions, and just continue iterating
                    throw new UncheckedIOException(ioe);
                }

                // END_DIRECTORY events are ignored
                if (ev.type() != FileTreeWalker.EventType.END_DIRECTORY) {
                    next = ev;
                    return;
                }
                ev = walker.next();
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        return next != null;
    }

    @Override
    public FileTreeWalker.Event next() {
        if (!walker.isOpen())
            throw new IllegalStateException();
        fetchNextIfNeeded();
        if (next == null)
            throw new NoSuchElementException();
        FileTreeWalker.Event result = next;
        next = null;
        return result;
    }

    @Override
    public void close() {
        walker.close();
    }

}
