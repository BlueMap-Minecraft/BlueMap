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
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.Database;
import de.bluecolored.bluemap.core.storage.sql.SQLStorage;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.Key;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
@Getter
public class SQLConfig extends StorageConfig {

    private static final Pattern URL_DIALECT_PATTERN = Pattern.compile("jdbc:([^:]*)://.*");

    private String connectionUrl = "jdbc:mysql://localhost/bluemap?permitMysqlScheme";
    private Map<String, String> connectionProperties = new HashMap<>();
    @DebugDump private String dialect = null;

    @DebugDump private String driverJar = null;
    @DebugDump private String driverClass = null;
    @DebugDump private int maxConnections = -1;

    @DebugDump private String compression = Compression.GZIP.getKey().getFormatted();

    @DebugDump
    @Getter(AccessLevel.NONE)
    private transient URL driverJarURL = null;

    public Optional<URL> getDriverJar() throws ConfigurationException {
        try {
            if (driverJar == null) return Optional.empty();

            if (driverJarURL == null) {
                driverJarURL = Paths.get(driverJar).toUri().toURL();
            }

            return Optional.of(driverJarURL);
        } catch (MalformedURLException ex) {
            throw new ConfigurationException("""
            The configured driver-jar path is not formatted correctly!
            Please check your 'driver-jar' setting in your configuration and make sure you have the correct path configured.
            """.strip(), ex);
        }
    }

    @SuppressWarnings("unused")
    public Optional<String> getDriverClass() {
        return Optional.ofNullable(driverClass);
    }

    public Compression getCompression() throws ConfigurationException {
        return parseKey(Compression.REGISTRY, compression, "compression");
    }

    public Dialect getDialect() throws ConfigurationException {
        String key = dialect;

        // default from connection-url
        if (key == null) {
            Matcher matcher = URL_DIALECT_PATTERN.matcher(connectionUrl);
            if (!matcher.find()) return Dialect.MYSQL;
            key = Key.bluemap(matcher.group(1)).getFormatted();
        }

        return parseKey(Dialect.REGISTRY, key, "dialect");
    }

    @Override
    public SQLStorage createStorage() throws ConfigurationException {
        Driver driver = createDriver();
        Database database;
        if (driver != null) {
            database = new Database(getConnectionUrl(), getConnectionProperties(), getMaxConnections(), driver);
        } else {
            database = new Database(getConnectionUrl(), getConnectionProperties(), getMaxConnections());
        }
        CommandSet commandSet = getDialect().createCommandSet(database);
        return new SQLStorage(commandSet, getCompression());
    }

    private @Nullable Driver createDriver() throws ConfigurationException {
        if (driverClass == null) return null;

        try {
            // load driver class
            Class<?> driverClazz;
            URL driverJarUrl = getDriverJar().orElse(null);
            if (driverJarUrl != null) {

                // sanity-check if file exists
                if (!Files.exists(Path.of(driverJarUrl.toURI()))) {
                    throw new ConfigurationException("""
                    The configured driver-jar was not found!
                    Please check your 'driver-jar' setting in your configuration and make sure you have the correct path configured.
                    """.strip());
                }

                ClassLoader classLoader = new URLClassLoader(new URL[]{driverJarUrl});
                driverClazz = Class.forName(driverClass, true, classLoader);
            } else {
                driverClazz = Class.forName(driverClass);
            }

            // create driver
            return (Driver) driverClazz.getDeclaredConstructor().newInstance();
        } catch (ClassCastException ex) {
            throw new ConfigurationException("""
            The configured driver-class was found but is not of the correct class-type!
            Please check your 'driver-class' setting in your configuration and make sure you have the correct class configured.
            """.strip(), ex);
        } catch (ClassNotFoundException ex) {
            throw new ConfigurationException("""
            The configured driver-class was not found!
            Please check your 'driver-class' setting in your configuration and make sure you have the correct class configured.
            """.strip(), ex);
        } catch (ConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConfigurationException("""
            BlueMap failed to load the configured SQL-Driver!
            Please check your 'driver-jar' and 'driver-class' settings in your configuration.
            """.strip(), ex);
        }
    }

}
