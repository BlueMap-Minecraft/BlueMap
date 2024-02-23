package de.bluecolored.bluemap.common.config.storage;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.s3.S3StorageSettings;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Optional;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class S3Config extends StorageConfig implements S3StorageSettings {
    private String endpoint = null;
    private String region = "";
    private String accessKey = "";
    private String secretKey = "";
    private String bucket = "";
    private Compression compression = Compression.GZIP;

    @Override
    public Optional<String> getEndpoint() {
        return Optional.ofNullable(endpoint);
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getBucket() {
        return bucket;
    }

    @Override
    public Compression getCompression() {
        return compression;
    }
}
