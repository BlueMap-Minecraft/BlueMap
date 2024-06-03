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

import de.bluecolored.bluemap.core.util.stream.OnCloseOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;

public class FileHelper {

    /**
     * Creates an OutputStream that writes to a ".filepart"-file first and then atomically moves (overwrites) to the final target atomically
     * once the stream gets closed.
     */
    public static OutputStream createFilepartOutputStream(final Path file) throws IOException {
        Path folder = file.toAbsolutePath().normalize().getParent();
        final Path partFile = folder.resolve(file.getFileName() + ".filepart");
        FileHelper.createDirectories(folder);
        OutputStream os = Files.newOutputStream(partFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        return new OnCloseOutputStream(os, () -> {
            if (!Files.exists(partFile)) return;
            FileHelper.createDirectories(folder);
            FileHelper.atomicMove(partFile, file);
        });
    }

    /**
     * Tries to move the file atomically, but fallbacks to a normal move operation if moving atomically fails
     */
    public static void atomicMove(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

}
