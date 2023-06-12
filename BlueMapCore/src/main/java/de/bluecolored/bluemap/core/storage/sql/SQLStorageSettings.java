package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.Compression;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public interface SQLStorageSettings {

    Optional<URL> getDriverJar() throws MalformedURLException;

    Optional<String> getDriverClass();

    String getConnectionUrl();

    Map<String, String> getConnectionProperties();

    int getMaxConnections();

    Compression getCompression();

}
