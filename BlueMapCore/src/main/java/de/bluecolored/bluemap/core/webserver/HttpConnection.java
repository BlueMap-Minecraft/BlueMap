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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import de.bluecolored.bluemap.core.logger.Logger;

public class HttpConnection implements Runnable {

	private HttpRequestHandler handler;

	private ServerSocket server;
	private Socket connection;
	private InputStream in;
	private OutputStream out;
	
	public HttpConnection(ServerSocket server, Socket connection, HttpRequestHandler handler, int timeout, TimeUnit timeoutUnit) throws IOException {
		this.server = server;
		this.connection = connection;
		this.handler = handler;
		
		if (isClosed()){
			throw new IOException("Socket already closed!");
		}
		
		connection.setSoTimeout((int) timeoutUnit.toMillis(timeout));
		
		in = this.connection.getInputStream();
		out = this.connection.getOutputStream();
	}

	@Override
	public void run() {
		while (!isClosed() && !server.isClosed()){
			try {
				HttpRequest request = acceptRequest();
				HttpResponse response = handler.handle(request);
				sendResponse(response);
			} catch (InvalidRequestException e){
				try {
					sendResponse(new HttpResponse(HttpStatusCode.BAD_REQUEST));
				} catch (IOException e1) {}
				break;
			} catch (SocketTimeoutException e) {
				break;
			} catch (SocketException e){
				break;
			} catch (ConnectionClosedException e){
				break;
			} catch (IOException e) {
				Logger.global.logError("Unexpected error while processing a HttpRequest!", e);
				break;
			}
		}
		
		try {
			close();
		} catch (IOException e){
			Logger.global.logError("Error while closing HttpConnection!", e);
		}
	}
	
	private void sendResponse(HttpResponse response) throws IOException {
		response.write(out);
		out.flush();
	}
	
	private HttpRequest acceptRequest() throws ConnectionClosedException, InvalidRequestException, IOException {
		return HttpRequest.read(in);
	}
	
	public boolean isClosed(){
		return !connection.isBound() || connection.isClosed() || !connection.isConnected() || connection.isOutputShutdown() || connection.isInputShutdown();
	}
	
	public void close() throws IOException {
		try {
			in.close();
		} finally {
			try {
				out.close();
			} finally {
				connection.close();
			}
		}
	}
	
	public static class ConnectionClosedException extends IOException {
		private static final long serialVersionUID = 1L;
	}
	
	public static class InvalidRequestException extends IOException {
		private static final long serialVersionUID = 1L;
	}
	
}
