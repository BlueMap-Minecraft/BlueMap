package de.bluecolored.bluemap.core.storage.b2;

import de.bluecolored.bluemap.core.storage.Compression;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public interface B2StorageSettings {
    String getApplicationKeyId();
    String getApplicationKey();
    String getBucket();
    Compression getCompression();
}