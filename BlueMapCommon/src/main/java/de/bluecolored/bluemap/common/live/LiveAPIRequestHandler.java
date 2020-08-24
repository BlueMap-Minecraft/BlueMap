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
package de.bluecolored.bluemap.common.live;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.stream.JsonWriter;

import de.bluecolored.bluemap.common.plugin.PluginConfig;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.webserver.HttpRequest;
import de.bluecolored.bluemap.core.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.core.webserver.HttpResponse;
import de.bluecolored.bluemap.core.webserver.HttpStatusCode;

public class LiveAPIRequestHandler implements HttpRequestHandler {

	private HttpRequestHandler notFoundHandler;
	private Map<String, HttpRequestHandler> liveAPIRequests;
	private ServerInterface server;
	
	private PluginConfig config;
	
	public LiveAPIRequestHandler(ServerInterface server, PluginConfig config, HttpRequestHandler notFoundHandler) {
		this.server = server;
		this.notFoundHandler = notFoundHandler;
		
		this.liveAPIRequests = new HashMap<>();

		this.liveAPIRequests.put("live", this::handleLivePingRequest);
		this.liveAPIRequests.put("live/players", this::handlePlayersRequest);
		
		this.config = config;
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		if (!config.isLiveUpdatesEnabled()) return this.notFoundHandler.handle(request);
		
		String path = request.getPath();
		
		//normalize path
		if (path.startsWith("/")) path = path.substring(1);
		if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
		
		HttpRequestHandler handler = liveAPIRequests.get(path);
		if (handler != null) return handler.handle(request);
		
		return this.notFoundHandler.handle(request);
	}

	public HttpResponse handleLivePingRequest(HttpRequest request) {
		HttpResponse response = new HttpResponse(HttpStatusCode.OK);
		response.setData("{\"status\":\"OK\"}");
		return response;
	}
	
	public HttpResponse handlePlayersRequest(HttpRequest request) {
		if (!request.getMethod().equalsIgnoreCase("GET")) return new HttpResponse(HttpStatusCode.BAD_REQUEST);
		
		try (
			StringWriter data = new StringWriter();
			JsonWriter json = new JsonWriter(data);
		){
			
			json.beginObject();
			json.name("players").beginArray();
			for (Player player : server.getOnlinePlayers()) {
				
				if (config.isHideInvisible() && player.isInvisible()) continue;
				if (config.isHideSneaking() && player.isSneaking()) continue;
				if (config.getHiddenGameModes().contains(player.getGamemode().getId())) continue;
				
				json.beginObject();
				json.name("uuid").value(player.getUuid().toString());
				json.name("name").value(player.getName().toPlainString());
				json.name("world").value(player.getWorld().toString());
				json.name("position").beginObject();
				json.name("x").value(player.getPosition().getX());
				json.name("y").value(player.getPosition().getY());
				json.name("z").value(player.getPosition().getZ());
				json.endObject();
				json.endObject();
			}
			json.endArray();
			json.endObject();
		
			HttpResponse response = new HttpResponse(HttpStatusCode.OK);
			response.setData(data.toString());
			
			return response;

		} catch (IOException ex) {
			return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}
	
}
