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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class CopyingPathVisitor extends SimpleFileVisitor<Path> {

    private final Path targetPath;
    private final CopyOption[] options;

    @Nullable private Path sourcePath;

    public CopyingPathVisitor(Path target, CopyOption... options) {
        this.targetPath = target;
        this.options = options;
    }

    public CopyingPathVisitor(@Nullable Path source, Path target, CopyOption... options) {
        this.sourcePath = source;
        this.targetPath = target;
        this.options = options;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path source, BasicFileAttributes attributes) throws IOException {
        // the first directory we visit will be the sourcePath
        if (sourcePath == null) sourcePath = source;

        Path target = resolveTarget(source);
        if (Files.notExists(target)) Files.createDirectory(target);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path source, BasicFileAttributes attributes) throws IOException {
        Path target = resolveTarget(source);
        Files.copy(source, target, options);

        return FileVisitResult.CONTINUE;
    }

    /**
     * Resolves the target file or directory using Path#toString() to make it compatible across different file-systems
     */
    private Path resolveTarget(Path source) {
        if (sourcePath == null) return targetPath;
        return targetPath.resolve(sourcePath.relativize(source).toString());
    }

}
