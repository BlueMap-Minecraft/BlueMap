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

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.SingleItemStorage;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.stream.OnCloseOutputStream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.*;

@RequiredArgsConstructor
public class SQLMetaItemStorage implements SingleItemStorage {

    private final CommandSet sql;
    private final String mapId;
    private final String itemName;
    private final Compression compression;

    @Override
    public OutputStream write() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        return new OnCloseOutputStream(compression.compress(bytes),
                () -> sql.writeMapMeta(mapId, itemName, bytes.toByteArray())
        );
    }

    @Override
    public @Nullable CompressedInputStream read() throws IOException {
        byte[] data = sql.readMapMeta(mapId, itemName);
        if (data == null) return null;
        return new CompressedInputStream(new ByteArrayInputStream(data), compression);
    }

    @Override
    public void delete() throws IOException {
        sql.deleteMapMeta(mapId, itemName);
    }

    @Override
    public boolean exists() throws IOException {
        return sql.hasMapMeta(mapId, itemName);
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
