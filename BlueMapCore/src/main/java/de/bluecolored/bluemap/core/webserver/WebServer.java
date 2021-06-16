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
package de.bluecolored.bluemap.core.webserver;

import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;

@DebugDump
public class WebServer extends Thread {

	private final int port;
	private final int maxConnections;
	private final InetAddress bindAddress;
	private final boolean verbose;

	private final HttpRequestHandler handler;
	private final Semaphore processingSemaphore;
	
	private ThreadPoolExecutor connectionThreads;
	
	private ServerSocket server;

	public WebServer(InetAddress bindAddress, int port, int maxConnections, HttpRequestHandler handler) {
		this(bindAddress, port, maxConnections, handler, false);
	}

	public WebServer(InetAddress bindAddress, int port, int maxConnections, HttpRequestHandler handler, boolean verbose) {
		this.port = port;
		this.maxConnections = maxConnections;
		this.bindAddress = bindAddress;
		this.verbose = verbose;
		
		this.handler = handler;
		this.processingSemaphore = new Semaphore(18);
		
		connectionThreads = null;
	}
	
	@Override
	public synchronized void start() {
		close();

		connectionThreads = new ThreadPoolExecutor(Math.min(maxConnections, 8), maxConnections, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
		
		try {
			server = new ServerSocket(port, maxConnections, bindAddress);
			server.setSoTimeout(0);
		} catch (IOException e){
			Logger.global.logError("Error while starting the WebServer!", e);
			return;
		}
		
		super.start();
	}
	
	@Override
	public void run(){
		if (server == null) return;
		
		Logger.global.logInfo("WebServer started.");
		
		while (!server.isClosed() && server.isBound()){

			try {
				Socket connection = server.accept();
				
				try {
					connectionThreads.execute(new HttpConnection(server, connection, handler, processingSemaphore, 10, TimeUnit.SECONDS, verbose));
				} catch (RejectedExecutionException e){
					connection.close();
					Logger.global.logWarning("Dropped an incoming HttpConnection! (Too many connections?)");
				}
				
			} catch (SocketException e){
				// this mainly occurs if the socket got closed, so we ignore this error
			} catch (IOException e){
				Logger.global.logError("Error while creating a new HttpConnection!", e);
			}
			
		}

		Logger.global.logInfo("WebServer closed.");
	}
	
	public synchronized void close(){
		if (connectionThreads != null) connectionThreads.shutdown();
		
		try {
			if (server != null && !server.isClosed()){
				server.close();
			}
		} catch (IOException e) {
			Logger.global.logError("Error while closing WebServer!", e);
		}
	}
	
}
