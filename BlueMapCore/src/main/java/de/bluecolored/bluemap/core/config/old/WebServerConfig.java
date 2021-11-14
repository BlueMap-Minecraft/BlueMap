/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.config.old;

import de.bluecolored.bluemap.core.config.ConfigurationException;
import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@DebugDump
public class WebServerConfig {

    private boolean enabled = true;
    private File webRoot = new File("web");

    private InetAddress bindAddress = null;
    private int port = 8100;
    private int maxConnections = 100;

    public WebServerConfig(ConfigurationNode node) throws ConfigurationException {

        //enabled
        enabled = node.node("enabled").getBoolean(false);

        if (enabled) {
            //webroot
            String webRootString = node.node("webroot").getString();
            if (webRootString == null) throw new ConfigurationException("Invalid configuration: Node webroot is not defined");
            webRoot = ConfigManager.toFolder(webRootString);

            //ip
            String bindAddressString = node.node("ip").getString("");
            try {
                if (bindAddressString.isEmpty() || bindAddressString.equals("0.0.0.0") ||
                    bindAddressString.equals("::0")) {
                    bindAddress = new InetSocketAddress(0).getAddress(); // 0.0.0.0
                } else if (bindAddressString.equals("#getLocalHost")) {
                    bindAddress = InetAddress.getLocalHost();
                } else {
                    bindAddress = InetAddress.getByName(bindAddressString);
                }
            } catch (IOException ex) {
                throw new ConfigurationException("Failed to parse ip: '" + bindAddressString + "'", ex);
            }

            //port
            port = node.node("port").getInt(8100);

            //maxConnectionCount
            maxConnections = node.node("maxConnectionCount").getInt(100);
        }

    }

    public boolean isWebserverEnabled() {
        return enabled;
    }

    public File getWebRoot() {
        return webRoot;
    }

    public InetAddress getWebserverBindAddress() {
        return bindAddress;
    }

    public int getWebserverPort() {
        return port;
    }

    public int getWebserverMaxConnections() {
        return maxConnections;
    }

}
