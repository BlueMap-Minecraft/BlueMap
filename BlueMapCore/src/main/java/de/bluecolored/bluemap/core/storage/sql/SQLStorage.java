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
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.core.storage.sql.dialect.DialectType;
import de.bluecolored.bluemap.core.storage.sql.dialect.Dialect;
import de.bluecolored.bluemap.core.util.WrappedOutputStream;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

public abstract class SQLStorage extends Storage {

    private final DataSource dataSource;

    protected final Dialect dialect;
    protected final Compression hiresCompression;

    private final LoadingCache<String, Integer> mapFKs = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .build(this::loadMapFK);
    private final LoadingCache<Compression, Integer> mapTileCompressionFKs = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .build(this::loadMapTileCompressionFK);

    private volatile boolean closed;

    public SQLStorage(Dialect dialect, SQLStorageSettings config) throws MalformedURLException, SQLDriverException {
        this.dialect = dialect;
        this.closed = false;
        try {
            if (config.getDriverClass().isPresent()) {
                if (config.getDriverJar().isPresent()) {
                    ClassLoader classLoader = new URLClassLoader(new URL[]{config.getDriverJar().get()});
                    Class<?> driverClass = Class.forName(config.getDriverClass().get(), true, classLoader);

                    Driver driver;
                    try {
                        driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new SQLDriverException("Failed to create an instance of the driver-class", ex);
                        /*throw new ConfigurationException(
                                "BlueMap is not able to create an instance of the configured Driver-Class.\n" +
                                "This means that BlueMap can not load this Driver at runtime.\n" +
                                "Instead you'll need to add your driver-jar to the classpath when starting your server," +
                                "e.g. using the '-classpath' command-line argument", ex);*/
                    }
                    this.dataSource = createDataSource(config.getConnectionUrl(), config.getConnectionProperties(), config.getMaxConnections(), driver);
                } else {
                    Class.forName(config.getDriverClass().get());
                    this.dataSource = createDataSource(config.getConnectionUrl(), config.getConnectionProperties(), config.getMaxConnections());
                }
            } else {
                this.dataSource = createDataSource(config.getConnectionUrl(), config.getConnectionProperties(), config.getMaxConnections());
            }
        } catch (ClassNotFoundException ex) {
            throw new SQLDriverException("The driver-class does not exist.", ex);
            //throw new ConfigurationException("The path to your driver-jar is invalid. Check your sql-storage-config!", ex);
            //throw new ConfigurationException("The driver-class does not exist. Check your sql-storage-config!", ex);
        }

        this.hiresCompression = config.getCompression();
    }

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(compression.compress(byteOut), () -> {
            int mapFK = getMapFK(mapId);
            int tileCompressionFK = getMapTileCompressionFK(compression);

            recoveringConnection(connection -> {
                Blob dataBlob = connection.createBlob();
                try {
                    try (OutputStream blobOut = dataBlob.setBinaryStream(1)) {
                        byteOut.writeTo(blobOut);
                    }

                    executeUpdate(connection,this.dialect.writeMapTile(),
                            mapFK,
                            lod,
                            tile.getX(),
                            tile.getY(),
                            tileCompressionFK,
                            dataBlob
                    );
                } finally {
                    dataBlob.free();
                }
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
                        Blob dataBlob = result.getBlob("data");
                        return dataBlob.getBytes(1, (int) dataBlob.length());
                    } else {
                        return null;
                    }
            }, 2);

            if (data == null) return Optional.empty();
            return Optional.of(new CompressedInputStream(new ByteArrayInputStream(data), compression));
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Optional<TileInfo> readMapTileInfo(final String mapId, int lod, final Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;

        try {
            TileInfo tileInfo = recoveringConnection(connection -> {
                ResultSet result = executeQuery(connection,
                        this.dialect.readMapTileInfo(),
                        mapId,
                        lod,
                        tile.getX(),
                        tile.getY(),
                        compression.getTypeId()
                );

                if (result.next()) {
                    final long lastModified = result.getTimestamp("changed").getTime();
                    final long size = result.getLong("size");

                    return new TileInfo() {
                        @Override
                        public CompressedInputStream readMapTile() throws IOException {
                            return SQLStorage.this.readMapTile(mapId, lod, tile)
                                    .orElseThrow(() -> new IOException("Tile no longer present!"));
                        }

                        @Override
                        public Compression getCompression() {
                            return compression;
                        }

                        @Override
                        public long getSize() {
                            return size;
                        }

                        @Override
                        public long getLastModified() {
                            return lastModified;
                        }
                    };
                } else {
                    return null;
                }
            }, 2);

            return Optional.ofNullable(tileInfo);
        } catch (SQLException | NoSuchElementException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        try {
            recoveringConnection(connection ->
                executeUpdate(connection,this.dialect.deleteMapTile(),
                        mapId,
                        lod,
                        tile.getX(),
                        tile.getY()
                ), 2);
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new WrappedOutputStream(byteOut, () -> {
            int mapFK = getMapFK(mapId);

            recoveringConnection(connection -> {
                Blob dataBlob = connection.createBlob();
                try {
                    try (OutputStream blobOut = dataBlob.setBinaryStream(1)) {
                        byteOut.writeTo(blobOut);
                    }

                    executeUpdate(connection,
                            this.dialect.writeMeta(),
                            mapFK,
                            escapeMetaName(name),
                            dataBlob
                    );
                } finally {
                    dataBlob.free();
                }
            }, 2);
        });
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
                    Blob dataBlob = result.getBlob("value");
                    return dataBlob.getBytes(1, (int) dataBlob.length());
                } else {
                    return null;
                }
            }, 2);

            if (data == null) return Optional.empty();
            return Optional.of(new CompressedInputStream(new ByteArrayInputStream(data), Compression.NONE));
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException {
        try {
            MetaInfo tileInfo = recoveringConnection(connection -> {
                ResultSet result = executeQuery(connection,
                        this.dialect.readMetaSize(),
                        mapId,
                        escapeMetaName(name)
                );

                if (result.next()) {
                    final long size = result.getLong("size");

                    return new MetaInfo() {
                        @Override
                        public InputStream readMeta() throws IOException {
                            return SQLStorage.this.readMeta(mapId, name)
                                    .orElseThrow(() -> new IOException("Tile no longer present!"));
                        }

                        @Override
                        public long getSize() {
                            return size;
                        }

                    };
                } else {
                    return null;
                }
            }, 2);

            return Optional.ofNullable(tileInfo);
        } catch (SQLException | NoSuchElementException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteMeta(String mapId, String name) throws IOException {
        try {
            recoveringConnection(connection ->
                    executeUpdate(connection,
                            this.dialect.purgeMeta(),
                            mapId,
                            escapeMetaName(name)
                    ), 2);
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException {
        synchronized (mapFKs) {
            try {
                recoveringConnection(connection -> {
                    executeUpdate(connection,
                            this.dialect.purgeMapTile(),
                            mapId
                    );

                    executeUpdate(connection,
                            this.dialect.purgeMapMeta(),
                            mapId
                    );


                    executeUpdate(connection,
                            this.dialect.purgeMap(),
                            mapId
                    );
                }, 2);

                mapFKs.invalidate(mapId);

            } catch (SQLException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public Collection<String> collectMapIds() throws IOException {
        try {
            return recoveringConnection(connection -> {
                    ResultSet result = executeQuery(connection,
                            this.dialect.selectMapIds()
                    );
                    Collection<String> mapIds = new ArrayList<>();
                    while (result.next()) {
                        mapIds.add(result.getString("map_id"));
                    }
                    return mapIds;
            }, 2);
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @SuppressWarnings("UnusedAssignment")
    public void initialize() throws IOException {
        try {

            // initialize and get schema-version
            String schemaVersionString = recoveringConnection(connection -> {
                connection.createStatement().executeUpdate(
                        this.dialect.initializeStorageMeta());

                ResultSet result = executeQuery(connection,
                        this.dialect.selectStorageMeta(),
                        "schema_version"
                );

                if (result.next()) {
                    return result.getString("value");
                } else {
                    executeUpdate(connection,
                            this.dialect.insertStorageMeta(),
                            "schema_version", "0"
                    );
                    return "0";
                }
            }, 2);

            int schemaVersion;
            try {
                schemaVersion = Integer.parseInt(schemaVersionString);
            } catch (NumberFormatException ex) {
                throw new IOException("Invalid schema-version number: " + schemaVersionString, ex);
            }

            // validate schema version
            if (schemaVersion < 0 || schemaVersion > 3)
                throw new IOException("Unknown schema-version: " + schemaVersion);

            // update schema to current version
            if (schemaVersion == 0) {
                Logger.global.logInfo("Initializing database-schema...");

                recoveringConnection(connection -> {

                    connection.createStatement().executeUpdate(
                            this.dialect.initializeMap()
                    );

                    connection.createStatement().executeUpdate(
                            this.dialect.initializeMapTileCompression()
                    );

                    connection.createStatement().executeUpdate(
                            this.dialect.initializeMapMeta());

                    connection.createStatement().executeUpdate(
                            this.dialect.initializeMapTile()
                    );

                    executeUpdate(connection,
                            this.dialect.updateStorageMeta(),
                            "3", "schema_version"
                    );
                }, 2);

                schemaVersion = 3;
            }

            if (schemaVersion == 1)
                throw new IOException("Outdated database schema: " + schemaVersion +
                        " (Cannot automatically update, reset your database and reload bluemap to fix this)");

            if (schemaVersion == 2) {
                Logger.global.logInfo("Updating database schema: Renaming bluemap_map_meta keys to new format...");
                recoveringConnection(connection -> {

                    // delete potential files that are already in the new format to avoid constraint-issues
                    executeUpdate(connection,
                            this.dialect.deleteMapMeta(),
                    "settings.json", "textures.json", ".rstate"
                    );

                    // rename files
                    executeUpdate(connection,
                            this.dialect.updateMapMeta(),
                    "settings.json", "settings"
                    );
                    executeUpdate(connection,
                            this.dialect.updateMapMeta(),
                            "textures.json", "textures"
                    );
                    executeUpdate(connection,
                            this.dialect.updateMapMeta(),
                            ".rstate", "render_state"
                    );

                    // update schemaVersion
                    executeUpdate(connection,
                            this.dialect.updateStorageMeta(),
                            "3", "schema_version"
                    );
                }, 2);

                schemaVersion = 3;
            }

        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ex) {
                throw new IOException("Failed to close datasource!", ex);
            }
        }
    }

   protected ResultSet executeQuery(Connection connection, @Language("sql") String sql, Object... parameters) throws SQLException {
        // we only use this prepared statement once, but the DB-Driver caches those and reuses them
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i+1, parameters[i]);
        }
        return statement.executeQuery();
    }

    @SuppressWarnings("UnusedReturnValue")
    protected int executeUpdate(Connection connection, @Language("sql") String sql, Object... parameters) throws SQLException {
        // we only use this prepared statement once, but the DB-Driver caches those and reuses them
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i+1, parameters[i]);
        }
        return statement.executeUpdate();
    }

    @SuppressWarnings("SameParameterValue")
    protected void recoveringConnection(ConnectionConsumer action, int tries) throws SQLException, IOException {
        recoveringConnection((ConnectionFunction<Void>) action, tries);
    }

    @SuppressWarnings("SameParameterValue")
    protected <R> R recoveringConnection(ConnectionFunction<R> action, int tries) throws SQLException, IOException {
        SQLException sqlException = null;

        try {
            for (int i = 0; i < tries; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    try {
                        R result = action.apply(connection);
                        connection.commit();
                        return result;
                    } catch (SQLRecoverableException ex) {
                        if (sqlException == null) {
                            sqlException = ex;
                        } else {
                            sqlException.addSuppressed(ex);
                        }
                    }
                }
            }
        } catch (SQLException | IOException | RuntimeException ex) {
            if (sqlException != null)
                ex.addSuppressed(sqlException);
            throw ex;
        }

        assert sqlException != null; // should never be null if we end up here
        throw sqlException;
    }

    protected int getMapFK(String mapId) throws SQLException {
        try {
            return Objects.requireNonNull(mapFKs.get(mapId));
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof SQLException)
                throw (SQLException) cause;

            throw ex;
        }
    }

    int getMapTileCompressionFK(Compression compression) throws SQLException {
        try {
            return Objects.requireNonNull(mapTileCompressionFKs.get(compression));
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof SQLException)
                throw (SQLException) cause;

            throw ex;
        }
    }

    private int loadMapFK(String mapId) throws SQLException, IOException {
        synchronized (mapFKs) {
            return lookupFK("bluemap_map", "id", "map_id", mapId);
        }
    }

    private int loadMapTileCompressionFK(Compression compression) throws SQLException, IOException {
        return lookupFK("bluemap_map_tile_compression", "id", "compression", compression.getTypeId());
    }

    @SuppressWarnings({"SameParameterValue", "SqlResolve"})
    private int lookupFK(String table, String idField, String valueField, String value) throws SQLException, IOException {
        return recoveringConnection(connection -> {
            int key;
            ResultSet result = executeQuery(connection,
                    this.dialect.lookupFK(table,idField,valueField),
                    value
            );

            if (result.next()) {
                key = result.getInt("id");
            } else {
                PreparedStatement statement = connection.prepareStatement(
                        this.dialect.insertFK(table,valueField),
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setString(1, value);
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if (!keys.next()) throw new IllegalStateException("No generated key returned!");
                key = keys.getInt(1);
            }

            return key;
        }, 2);
    }

    private DataSource createDataSource(String dbUrl, Map<String, String> properties, int maxPoolSize) {
        Properties props = new Properties();
        props.putAll(properties);

        return createDataSource(new DriverManagerConnectionFactory(dbUrl, props), maxPoolSize);
    }

    private DataSource createDataSource(String dbUrl, Map<String, String> properties, int maxPoolSize, Driver driver) {
        Properties props = new Properties();
        props.putAll(properties);

        ConnectionFactory connectionFactory = new DriverConnectionFactory(
                driver,
                dbUrl,
                props
        );

        return createDataSource(connectionFactory, maxPoolSize);
    }

    private DataSource createDataSource(ConnectionFactory connectionFactory, int maxPoolSize) {
        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(() -> {
                    Logger.global.logDebug("Creating new SQL-Connection...");
                    return connectionFactory.createConnection();
                }, null);
        poolableConnectionFactory.setPoolStatements(true);
        poolableConnectionFactory.setMaxOpenPreparedStatements(20);
        poolableConnectionFactory.setDefaultAutoCommit(false);
        poolableConnectionFactory.setAutoCommitOnReturn(false);
        poolableConnectionFactory.setRollbackOnReturn(true);
        poolableConnectionFactory.setFastFailValidation(true);

        GenericObjectPoolConfig<PoolableConnection> objectPoolConfig = new GenericObjectPoolConfig<>();
        objectPoolConfig.setTestWhileIdle(true);
        objectPoolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(10));
        objectPoolConfig.setNumTestsPerEvictionRun(3);
        objectPoolConfig.setBlockWhenExhausted(true);
        objectPoolConfig.setMinIdle(1);
        objectPoolConfig.setMaxIdle(Runtime.getRuntime().availableProcessors());
        objectPoolConfig.setMaxTotal(maxPoolSize);
        objectPoolConfig.setMaxWaitMillis(Duration.ofSeconds(30).toMillis());

        ObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory, objectPoolConfig);
        poolableConnectionFactory.setPool(connectionPool);

        return new PoolingDataSource<>(connectionPool);
    }

    public static SQLStorage create(SQLStorageSettings settings) throws Exception {
        String dbUrl = settings.getConnectionUrl();
        String provider = dbUrl.strip().split(":", 3)[1];
        return DialectType.getStorage(provider,settings);
    }

    @FunctionalInterface
    public interface ConnectionConsumer extends ConnectionFunction<Void> {

        void accept(Connection connection) throws SQLException, IOException;

        @Override
        default Void apply(Connection connection) throws SQLException, IOException {
            accept(connection);
            return null;
        }

    }

    @FunctionalInterface
    public interface ConnectionFunction<R>  {

        R apply(Connection connection) throws SQLException, IOException;

    }

}
