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
package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.stream.OnCloseOutputStream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class SQLGridStorage implements GridStorage {

    private final CommandSet sql;
    private final String map;
    private final Key storage;
    private final Compression compression;

    @Override
    public OutputStream write(int x, int z) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        return new OnCloseOutputStream(compression.compress(bytes),
                () -> sql.writeGridItem(map, storage, x, z, compression, bytes.toByteArray())
        );
    }

    @Override
    public @Nullable CompressedInputStream read(int x, int z) throws IOException {
        byte[] data = sql.readGridItem(map, storage, x, z, compression);
        if (data == null) return null;
        return new CompressedInputStream(new ByteArrayInputStream(data), compression);
    }

    @Override
    public void delete(int x, int z) throws IOException {
        sql.deleteGridItem(map, storage, x, z);
    }

    @Override
    public boolean exists(int x, int z) throws IOException {
        return sql.hasGridItem(map, storage, x, z, compression);
    }

    @Override
    public ItemStorage cell(int x, int z) {
        return new GridStorageCell(this, x, z);
    }

    @Override
    public Stream<Cell> stream() throws IOException {
        return StreamSupport.stream(
                new PageSpliterator<>(page -> {
                    try {
                        return sql.listGridItems(map, storage, compression, page * 1000, 1000);
                    } catch (IOException ex) { throw new RuntimeException(ex); }
                }),
                false
        ).map(tilePosition -> new GridStorageCell(this, tilePosition.x(), tilePosition.z()));
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
