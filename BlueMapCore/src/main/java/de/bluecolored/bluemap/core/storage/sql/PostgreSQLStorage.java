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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.sql.dialect.Dialect;
import de.bluecolored.bluemap.core.storage.sql.dialect.PostgresDialect;
import de.bluecolored.bluemap.core.util.WrappedOutputStream;

import java.io.*;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PostgreSQLStorage extends SQLStorage {

    public PostgreSQLStorage(SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(PostgresDialect.INSTANCE, config);
    }

    public PostgreSQLStorage(Dialect dialect, SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(dialect, config);
    }

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(compression.compress(byteOut), () -> {
            int mapFK = getMapFK(mapId);
            int tileCompressionFK = getMapTileCompressionFK(compression);

            recoveringConnection(connection -> {
                executeUpdate(connection, this.dialect.writeMapTile(),
                        mapFK,
                        lod,
                        tile.getX(),
                        tile.getY(),
                        tileCompressionFK,
                        byteOut.toByteArray()
                );
            }, 2);
        });
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(byteOut, () -> {
            int mapFK = getMapFK(mapId);
            recoveringConnection(connection -> {
                executeUpdate(connection, this.dialect.writeMeta(),
                    mapFK,
                    name,
                    byteOut.toByteArray()
                );
            }, 2);
        });
    }

    @Override
    public Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;

        try {
            byte[] data = recoveringConnection(connection -> {
                ResultSet result = executeQuery(connection, this.dialect.readMapTile(),
                        mapId,
                        lod,
                        tile.getX(),
                        tile.getY(),
                        compression.getTypeId()
                );

                if (result.next()) {
                    return result.getBytes(1);
                } else {
                    return null;
                }
            }, 2);

            if (data == null) {
                return Optional.empty();
            }

            InputStream inputStream = new ByteArrayInputStream(data);
            return Optional.of(new CompressedInputStream(inputStream, compression));
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Optional<InputStream> readMeta(String mapId, String name) throws IOException {
        try {
            byte[] data = recoveringConnection(connection -> {
                ResultSet result = executeQuery(connection, this.dialect.readMeta(),
                        mapId,
                        escapeMetaName(name)
                );
                if (result.next()) {
                    return result.getBytes(1);
                } else {
                    return null;
                }
            }, 2);

            if (data == null) {
                return Optional.empty();
            }

            InputStream inputStream = new ByteArrayInputStream(data);
            return Optional.of(inputStream);
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

}
