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

import java.util.HashMap;
import java.util.Map;

import de.bluecolored.bluemap.core.webserver.HttpRequest;
import de.bluecolored.bluemap.core.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.core.webserver.HttpResponse;
import de.bluecolored.bluemap.core.webserver.HttpStatusCode;

public class LiveAPIRequestHandler implements HttpRequestHandler {

	private HttpRequestHandler notFoundHandler;
	private Map<String, HttpRequestHandler> liveAPIRequests;
	
	public LiveAPIRequestHandler(HttpRequestHandler notFoundHandler) {
		this.notFoundHandler = notFoundHandler;
		
		this.liveAPIRequests = new HashMap<>();
		
		this.liveAPIRequests.put("live/events", this::handleEventsRequest);
	}

	@Override
	public HttpResponse handle(HttpRequest request) {
		HttpRequestHandler handler = liveAPIRequests.get(request.getPath());
		if (handler != null) return handler.handle(request);
		
		return this.notFoundHandler.handle(request);
	}

	public HttpResponse handleEventsRequest(HttpRequest request) {
		if (!request.getMethod().equalsIgnoreCase("GET")) return new HttpResponse(HttpStatusCode.BAD_REQUEST);
		
		return new HttpResponse(HttpStatusCode.NOT_IMPLEMENTED);
	}
	
}
