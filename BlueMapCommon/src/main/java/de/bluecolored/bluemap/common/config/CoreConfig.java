package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class CoreConfig {

    private boolean acceptDownload = false;

    private int renderThreadCount = 1;

    private boolean metrics = true;

    private Path data = Path.of("bluemap");

    public boolean isAcceptDownload() {
        return acceptDownload;
    }

    public int getRenderThreadCount() {
        return renderThreadCount;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public Path getData() {
        return data;
    }

}
