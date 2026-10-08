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

import de.bluecolored.bluemap.core.util.FileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * An {@link OutputStream} that writes to a temporary part-file first and then atomically moves (overwrites) it to the
 * final target once the stream gets closed.<br>
 * Use {@link #abort()} instead of {@link #close()} to discard the written data and leave the target untouched.
 */
public class FilepartOutputStream extends DelegateOutputStream {

    private static final int MAX_SLOTS = 10;
    private static final long STALE_AFTER_MILLIS = TimeUnit.HOURS.toMillis(1);

    private final Path folder;
    private final Path file;
    private final Path partFile;
    private boolean closed = false;

    private FilepartOutputStream(Path folder, Path file, Path partFile, OutputStream out) {
        super(out);
        this.folder = folder;
        this.file = file;
        this.partFile = partFile;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;

        try {
            super.close();
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(partFile);
            } catch (IOException deleteEx) {
                ex.addSuppressed(deleteEx);
            }
            throw ex;
        }

        if (!Files.exists(partFile))
            throw new NoSuchFileException(partFile.toString(), null, "Part-file was deleted before it could be moved to the target");

        try {
            FileHelper.createDirectories(folder);
            FileHelper.atomicMove(partFile, file);
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(partFile);
            } catch (IOException deleteEx) {
                ex.addSuppressed(deleteEx);
            }
            throw ex;
        }
    }

    public synchronized void abort() throws IOException {
        if (closed) return;
        closed = true;

        IOException ioException = null;

        try {
            super.close();
        } catch (IOException ex) {
            ioException = ex;
        }

        try {
            Files.deleteIfExists(partFile);
        } catch (IOException ex) {
            if (ioException == null) ioException = ex;
            else ioException.addSuppressed(ex);
        }

        if (ioException != null) throw ioException;
    }

    public static FilepartOutputStream create(@NotNull Path file) throws IOException {
        Path folder = file.toAbsolutePath().normalize().getParent();
        if (folder == null) throw new IOException("File has no parent!");

        FileHelper.createDirectories(folder);

        String name = file.getFileName().toString();
        for (int i = 0; i < MAX_SLOTS; i++) {
            Path filepart = folder.resolve(i == 0 ? name + ".filepart" : name + "." + i + ".filepart");

            OutputStream out = tryCreate(filepart);
            if (out == null && deleteIfStale(filepart)) out = tryCreate(filepart);
            if (out != null) return new FilepartOutputStream(folder, file, filepart, out);
        }

        throw new IOException("No free part-file slot for '" + file + "' (" + MAX_SLOTS + " slots are in use)");
    }

    private static @Nullable OutputStream tryCreate(Path path) throws IOException {
        try {
            return Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ex) {
            return null;
        }
    }

    private static boolean deleteIfStale(Path path) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            if (System.currentTimeMillis() - lastModified.toMillis() < STALE_AFTER_MILLIS)
                return false;

            Files.delete(path);
            return true;
        } catch (NoSuchFileException ex) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

}
