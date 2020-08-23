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
package de.bluecolored.bluemap.core.config;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import ninja.leaping.configurate.ConfigurationNode;

public class WebServerConfig {

	private boolean enabled = true;
	private File webRoot = new File("web");

	private InetAddress bindAdress = null;
	private int port = 8100;
	private int maxConnections = 100;

	public WebServerConfig(ConfigurationNode node) throws IOException {
		
		//enabled
		enabled = node.getNode("enabled").getBoolean(false);

		if (enabled) {
			//webroot
			String webRootString = node.getNode("webroot").getString();
			if (webRootString == null) throw new IOException("Invalid configuration: Node webroot is not defined");
			webRoot = ConfigManager.toFolder(webRootString);
			
			//ip
			String bindAdressString = node.getNode("ip").getString("");
			if (bindAdressString.isEmpty() || bindAdressString.equals("0.0.0.0") || bindAdressString.equals("::0")) {
				bindAdress = new InetSocketAddress(0).getAddress(); // 0.0.0.0
			} else if (bindAdressString.equals("#getLocalHost")) {
				bindAdress = InetAddress.getLocalHost();
			} else {
				bindAdress = InetAddress.getByName(bindAdressString);
			}
			
			//port
			port = node.getNode("port").getInt(8100);
			
			//maxConnectionCount
			maxConnections = node.getNode("maxConnectionCount").getInt(100);
		}
		
	}
	
	public boolean isWebserverEnabled() {
		return enabled;
	}
	
	public File getWebRoot() {
		return webRoot;
	}

	public InetAddress getWebserverBindAdress() {
		return bindAdress;
	}

	public int getWebserverPort() {
		return port;
	}

	public int getWebserverMaxConnections() {
		return maxConnections;
	}
	
}
