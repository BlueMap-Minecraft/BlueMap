package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.api.debug.DebugDump;
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

    private boolean scanForModResources = true;

    public boolean isAcceptDownload() {
        return acceptDownload;
    }

    public int getRenderThreadCount() {
        return renderThreadCount;
    }

    public int resolveRenderThreadCount() {
        if (renderThreadCount > 0) return renderThreadCount;
        return Math.max(Runtime.getRuntime().availableProcessors() + renderThreadCount, 1);
    }

    public boolean isMetrics() {
        return metrics;
    }

    public Path getData() {
        return data;
    }

    public boolean isScanForModResources() {
        return scanForModResources;
    }

}
