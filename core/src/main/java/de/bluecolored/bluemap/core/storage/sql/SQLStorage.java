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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class SQLStorage implements Storage {

    private final CommandSet sql;
    private final Compression compression;
    private final LoadingCache<String, SQLMapStorage> mapStorages = Caffeine.newBuilder()
            .build(this::create);

    @Override
    public void initialize() throws IOException {
        sql.initializeTables();
    }

    private SQLMapStorage create(String mapId) {
        return new SQLMapStorage(mapId, sql, compression);
    }

    @Override
    public MapStorage map(String mapId) {
        return mapStorages.get(mapId);
    }

    @Override
    public Stream<String> mapIds() {
        return StreamSupport.stream(
                new PageSpliterator<>(page -> {
                    try {
                        return sql.listMapIds(page * 1000, 1000);
                    } catch (IOException ex) { throw new RuntimeException(ex); }
                }),
                false
        );
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

    @Override
    public void close() throws IOException {
        sql.close();
    }

}
