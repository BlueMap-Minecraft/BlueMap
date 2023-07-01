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

    private LogConfig log = new LogConfig();

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

    public LogConfig getLog() {
        return log;
    }

    @DebugDump
    @ConfigSerializable
    public static class LogConfig {

        private String file = null;
        private boolean append = false;
        private String format = "%1$s \"%3$s %4$s %5$s\" %6$s %7$s";

        public String getFile() {
            return file;
        }

        public boolean isAppend() {
            return append;
        }

        public String getFormat() {
            return format;
        }

    }

}
