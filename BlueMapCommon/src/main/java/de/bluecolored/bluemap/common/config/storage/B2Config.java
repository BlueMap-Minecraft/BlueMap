package de.bluecolored.bluemap.common.config.storage;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.Compression;
import de.bluecolored.bluemap.core.storage.b2.B2StorageSettings;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class B2Config extends StorageConfig implements B2StorageSettings {
    private String applicationKeyId = "";
    private String applicationKey = "";
    private String bucket = "";
    private Compression compression = Compression.GZIP;

    @Override
    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    @Override
    public String getApplicationKey() {
        return applicationKey;
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
