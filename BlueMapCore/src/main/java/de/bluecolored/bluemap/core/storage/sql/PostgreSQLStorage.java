package de.bluecolored.bluemap.core.storage.sql;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.sql.dialect.PostgresFactory;
import de.bluecolored.bluemap.core.util.WrappedOutputStream;

import java.io.*;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PostgreSQLStorage extends SQLStorage {
    public PostgreSQLStorage(SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        super(new PostgresFactory(), config);
    }

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(compression.compress(byteOut), () -> {
            int mapFK = getMapFK(mapId);
            int tileCompressionFK = getMapTileCompressionFK(compression);

            recoveringConnection(connection -> {
                byte[] byteData = byteOut.toByteArray();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(byteData);

                PreparedStatement statement = connection.prepareStatement(this.dialect.writeMapTile());
                statement.setInt(1, mapFK);
                statement.setInt(2, lod);
                statement.setInt(3, tile.getX());
                statement.setInt(4, tile.getY());
                statement.setInt(5, tileCompressionFK);
                statement.setBinaryStream(6, inputStream);

                statement.executeUpdate();
            }, 2);
        });
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(byteOut, () -> {
            int mapFK = getMapFK(mapId);
            recoveringConnection(connection -> {
                byte[] byteData = byteOut.toByteArray();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(byteData);

                PreparedStatement statement = connection.prepareStatement(this.dialect.writeMeta());
                statement.setInt(1, mapFK);
                statement.setString(2, name);
                statement.setBinaryStream(3, inputStream);

                statement.executeUpdate();
            }, 2);
        });
    }

    @Override
    public Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;

        try {
            byte[] data = recoveringConnection(connection -> {
                ResultSet result = executeQuery(connection,
                        this.dialect.readMapTile(),
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
                ResultSet result = executeQuery(connection,
                        this.dialect.readMeta(),
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
