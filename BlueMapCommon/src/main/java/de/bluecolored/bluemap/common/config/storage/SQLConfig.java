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
package de.bluecolored.bluemap.common.config.storage;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.sql.SQLStorageSettings;
import de.bluecolored.bluemap.core.storage.sql.dialect.Dialect;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
public class SQLConfig extends StorageConfig implements SQLStorageSettings {

    @DebugDump private String driverJar = null;
    @DebugDump private String driverClass = null;
    private String connectionUrl = "jdbc:mysql://localhost/bluemap?permitMysqlScheme";

    private Map<String, String> connectionProperties = new HashMap<>();

    @DebugDump private Dialect dialect = Dialect.MariaDB;

    @DebugDump private Compression compression = Compression.GZIP;

    @DebugDump private transient URL driverJarURL = null;

    @DebugDump private int maxConnections = -1;

    @Override
    public Optional<URL> getDriverJar() throws MalformedURLException {
        if (driverJar == null) return Optional.empty();

        if (driverJarURL == null) {
            driverJarURL = Paths.get(driverJar).toUri().toURL();
        }

        return Optional.of(driverJarURL);
    }

    @Override
    public Optional<String> getDriverClass() {
        return Optional.ofNullable(driverClass);
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public Map<String, String> getConnectionProperties() {
        return connectionProperties;
    }

    @Override
    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public Compression getCompression() {
        return compression;
    }

}
