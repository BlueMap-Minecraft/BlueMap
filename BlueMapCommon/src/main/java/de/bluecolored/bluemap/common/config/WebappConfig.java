package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class WebappConfig {

    private boolean enabled = true;

    private Path webroot = Path.of("bluemap", "web");

    private boolean useCookies = true;

    private boolean enableFreeFlight = true;

    public boolean isEnabled() {
        return enabled;
    }

    public Path getWebroot() {
        return webroot;
    }

    public boolean isUseCookies() {
        return useCookies;
    }

    public boolean isEnableFreeFlight() {
        return enableFreeFlight;
    }

}
