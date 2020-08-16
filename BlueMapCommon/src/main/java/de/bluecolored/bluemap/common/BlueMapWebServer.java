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
package de.bluecolored.bluemap.common;

import java.io.IOException;

import de.bluecolored.bluemap.common.live.LiveAPIRequestHandler;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.config.LiveAPISettings;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.web.FileRequestHandler;
import de.bluecolored.bluemap.core.web.WebFilesManager;
import de.bluecolored.bluemap.core.web.WebServerConfig;
import de.bluecolored.bluemap.core.webserver.WebServer;

public class BlueMapWebServer extends WebServer {
	
	private WebFilesManager webFilesManager;
	

	public BlueMapWebServer(WebServerConfig config) {
		super(
			config.getWebserverPort(), 
			config.getWebserverMaxConnections(), 
			config.getWebserverBindAdress(), 
			new FileRequestHandler(config.getWebRoot(), "BlueMap/Webserver")
		);
		
		this.webFilesManager = new WebFilesManager(config.getWebRoot());
	}
	
	public BlueMapWebServer(WebServerConfig config, LiveAPISettings liveSettings, ServerInterface server) {
		super(
			config.getWebserverPort(), 
			config.getWebserverMaxConnections(), 
			config.getWebserverBindAdress(), 
			new LiveAPIRequestHandler(server, liveSettings, new FileRequestHandler(config.getWebRoot(), "BlueMap/Webserver"))
		);
		
		this.webFilesManager = new WebFilesManager(config.getWebRoot());
	}
	
	public void updateWebfiles() throws IOException {
		if (webFilesManager.needsUpdate()) {
			Logger.global.logInfo("Webfiles are missing or outdated, updating...");
			webFilesManager.updateFiles();
		}
	}
	
}
