package de.bluecolored.bluemap.core.storage.s3;

import de.bluecolored.bluemap.core.storage.Compression;

import java.util.Optional;

public interface S3StorageSettings {
    Optional<String> getEndpoint();
    String getRegion();
    String getAccessKey();
    String getSecretKey();
    String getBucket();
    Compression getCompression();
}