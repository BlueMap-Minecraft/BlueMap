package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.sql.dialect.Dialect;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public interface SQLStorageSettings {

    Optional<URL> getDriverJar() throws MalformedURLException;

    Optional<String> getDriverClass();

    Dialect getDialect();

    String getConnectionUrl();

    Map<String, String> getConnectionProperties();

    int getMaxConnections();

    Compression getCompression();

}
