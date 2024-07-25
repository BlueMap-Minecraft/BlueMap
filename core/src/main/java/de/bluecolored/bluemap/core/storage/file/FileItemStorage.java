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
package de.bluecolored.bluemap.core.storage.file;

import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@RequiredArgsConstructor
public class FileItemStorage implements ItemStorage {

    private final Path file;
    private final Compression compression;
    private final boolean atomic;

    @Override
    public OutputStream write() throws IOException {
        if (atomic)
            return compression.compress(FileHelper.createFilepartOutputStream(file));

        Path folder = file.toAbsolutePath().normalize().getParent();
        FileHelper.createDirectories(folder);
        return compression.compress(Files.newOutputStream(file,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE));
    }

    @Override
    public @Nullable CompressedInputStream read() throws IOException {
        if (!Files.exists(file)) return null;
        try {
            return new CompressedInputStream(Files.newInputStream(file), compression);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            return null;
        }
    }

    @Override
    public void delete() throws IOException {
        if (Files.exists(file)) Files.delete(file);
    }

    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

}
