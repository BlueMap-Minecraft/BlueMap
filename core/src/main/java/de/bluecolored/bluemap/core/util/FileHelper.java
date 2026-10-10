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

import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FileHelper {

    /**
     * Tries to move the file atomically, but fallbacks to a normal move operation if moving atomically fails.
     * If the atomic move operation fails, it is retried at most 5 times, before falling back to a normal move.
     */
    public static void atomicMove(Path from, Path to) throws IOException {
        try {
            for (int attempt = 0; ; attempt++) {
                try {
                    Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    return;
                } catch (AccessDeniedException ex) {
                    if (attempt >= 5) throw ex;
                    try {
                        //noinspection BusyWait
                        Thread.sleep(20L << attempt);
                    } catch (InterruptedException interruptedEx) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        } catch (FileNotFoundException | NoSuchFileException ignore) {
        } catch (IOException ex) {
            try {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            } catch (FileNotFoundException | NoSuchFileException ignore) {
            } catch (Throwable t) {
                t.addSuppressed(ex);
                throw t;
            }
        }
    }

    /**
     * Same as {@link Files#createDirectories(Path, FileAttribute[])} but accepts symlinked folders.
     * @see Files#createDirectories(Path, FileAttribute[])
     */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        if (Files.isDirectory(dir)) return dir;
        return Files.createDirectories(dir, attrs);
    }


    /**
     * Extracts the entire zip-file into the given target directory
     */
    public static void extractZipFile(URL zipFile, Path targetDirectory, CopyOption... options) throws IOException {
        Path temp = Files.createTempFile(null, ".zip");
        FileHelper.copy(zipFile, temp);
        FileHelper.extractZipFile(temp, targetDirectory, options);
        Files.deleteIfExists(temp);
    }

    /**
     * Extracts the entire zip-file into the given target directory
     */
    public static void extractZipFile(Path zipFile, Path targetDirectory, CopyOption... options) throws IOException {
        try (FileSystem webappZipFs = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
            CopyingPathVisitor copyAction = new CopyingPathVisitor(targetDirectory, options);
            for (Path root : webappZipFs.getRootDirectories()) {
                Files.walkFileTree(root, copyAction);
            }
        }
    }

    /**
     * Copies from a URL to a target-path
     */
    public static void copy(URL source, Path target) throws IOException {
        try (
                InputStream in = source.openStream();
                OutputStream out = Files.newOutputStream(target)
        ) {
            in.transferTo(out);
        }
    }

    /**
     * Uses file-watchers on the path-parent and manual checks on an interval as a fallback to wait until a specific file or folder exists
     */
    public static boolean awaitExistence(
            Path path,
            long checkInterval, TimeUnit checkIntervalUnit,
            long timeout, TimeUnit timeoutUnit
    ) throws IOException, InterruptedException {
        if (checkInterval <= 0) throw new IllegalArgumentException("checkInterval must be positive");

        long checkIntervalMillis = Math.max(checkIntervalUnit.toMillis(checkInterval), 1);
        long endTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + timeoutUnit.toMillis(timeout);
        return awaitExistence(path, checkIntervalMillis, endTime);
    }

    private static boolean awaitExistence(Path path, long checkIntervalMillis, long endTime) throws IOException, InterruptedException {
        if (Files.exists(path)) return true;

        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("No parent directory exists that can be watched.");
        if (!awaitExistence(parent, checkIntervalMillis, endTime)) return false;

        try (WatchService watchService = createWatchService(parent)) {
            while (!Files.exists(path)) {
                long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
                if (now >= endTime) return false;

                // check manually at set interval, in case file-watchers don't work
                long waitTime = Math.clamp(endTime - now, 1, checkIntervalMillis);

                if (watchService == null) {
                    //noinspection BusyWait
                    Thread.sleep(waitTime);
                    continue;
                }

                WatchKey key = watchService.poll(waitTime, TimeUnit.MILLISECONDS);
                if (key != null) {
                    key.pollEvents();
                    key.reset();
                }
            }
            return true;
        }
    }

    /**
     * Adapted version of {@link Files#walk(Path, int, FileVisitOption...)}.
     * This version ignores NoSuchFileException if they occur while iterating the file-tree.
     */
    public static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) throws IOException {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options);
        try {
            Spliterator<FileTreeWalker.Event> spliterator =
                    Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT);
            return StreamSupport.stream(spliterator, false)
                    .onClose(iterator::close)
                    .map(FileTreeWalker.Event::file);
        } catch (Error|RuntimeException e) {
            iterator.close();
            throw e;
        }
    }

    /**
     * Adapted version of {@link Files#walk(Path, FileVisitOption...)} .
     * This version ignores NoSuchFileException if they occur while iterating the file-tree.
     */
    public static Stream<Path> walk(Path start, FileVisitOption... options) throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    private static @Nullable WatchService createWatchService(Path dir) throws IOException {
        WatchService watchService;
        try {
            watchService = dir.getFileSystem().newWatchService();
        } catch (UnsupportedOperationException ex) {
            return null;
        }
        try {
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            return watchService;
        } catch (UnsupportedOperationException ex) {
            watchService.close();
            return null;
        } catch (IOException | RuntimeException ex) {
            watchService.close();
            throw ex;
        }
    }

}
