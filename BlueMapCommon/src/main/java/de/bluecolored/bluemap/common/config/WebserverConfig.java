package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.api.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class WebserverConfig {

    private boolean enabled = true;

    private Path webroot = Path.of("bluemap", "web");

    private String ip = "0.0.0.0";

    private int port = 8100;

    private int maxConnectionCount = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public Path getWebroot() {
        return webroot;
    }

    public String getIp() {
        return ip;
    }

    public InetAddress resolveIp() throws UnknownHostException {
        if (ip.isEmpty() || ip.equals("0.0.0.0") || ip.equals("::0")) {
            return new InetSocketAddress(0).getAddress();
        } else if (ip.equals("#getLocalHost")) {
            return InetAddress.getLocalHost();
        } else {
            return InetAddress.getByName(ip);
        }
    }

    public int getPort() {
        return port;
    }

    public int getMaxConnectionCount() {
        return maxConnectionCount;
    }

}
