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

import de.bluecolored.bluemap.core.logger.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class HttpConnection implements Runnable {

	private final HttpRequestHandler handler;

	private final ServerSocket server;
	private final Socket connection;
	private final InputStream in;
	private final OutputStream out;

	private final Semaphore processingSemaphore;

    private final boolean verbose;

	public HttpConnection(ServerSocket server, Socket connection, HttpRequestHandler handler, Semaphore processingSemaphore, int timeout, TimeUnit timeoutUnit, boolean verbose) throws IOException {
		this.server = server;
		this.connection = connection;
		this.handler = handler;
        this.verbose = verbose;

        this.processingSemaphore = processingSemaphore;

		if (isClosed()){
			throw new IOException("Socket already closed!");
		}

		connection.setSoTimeout((int) timeoutUnit.toMillis(timeout));

		in = new BufferedInputStream(this.connection.getInputStream());
		out = new BufferedOutputStream(this.connection.getOutputStream());
	}

	@Override
	public void run() {
		while (!isClosed() && !server.isClosed()){
			try {
				HttpRequest request = acceptRequest();

				boolean hasPermit = false;
				try {
					//just slow down processing if limit is reached
					hasPermit = processingSemaphore.tryAcquire(1, TimeUnit.SECONDS);

					HttpResponse response = handler.handle(request);
					sendResponse(response);

					if (verbose) log(request, response);
				} finally {
					if (hasPermit) processingSemaphore.release();
				}

			} catch (InvalidRequestException e){
				try {
					sendResponse(new HttpResponse(HttpStatusCode.BAD_REQUEST));
				} catch (IOException ignored) {}
				break;
			} catch (SocketTimeoutException | ConnectionClosedException | SocketException e) {
				break;
			} catch (IOException e) {
				Logger.global.logError("Unexpected error while processing a HttpRequest!", e);
				break;
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		try {
			close();
		} catch (IOException e){
			Logger.global.logError("Error while closing HttpConnection!", e);
		}
	}

	private void log(HttpRequest request, HttpResponse response) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		Logger.global.logInfo(
				connection.getInetAddress().toString()
				+ " [ "
				+ dateFormat.format(date)
				+ " ] \""
				+ request.getMethod()
				+ " " + request.getPath()
				+ " " + request.getVersion()
				+ "\" "
				+ response.getStatusCode().toString());
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
		connection.close();
	}

	public static class ConnectionClosedException extends IOException {
		private static final long serialVersionUID = 1L;
	}

	public static class InvalidRequestException extends IOException {
		private static final long serialVersionUID = 1L;
	}

}
