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
package de.bluecolored.bluemap.core.web;

import de.bluecolored.bluemap.core.webserver.HttpRequest;
import de.bluecolored.bluemap.core.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.core.webserver.HttpResponse;
import de.bluecolored.bluemap.core.webserver.HttpStatusCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileRequestHandler implements HttpRequestHandler {

	private static final long DEFLATE_MIN_SIZE = 10L * 1024L;
	private static final long DEFLATE_MAX_SIZE = 10L * 1024L * 1024L;
	private static final long INFLATE_MAX_SIZE = 10L * 1024L * 1024L;
	
	private final Path webRoot;
	private final String serverName;

	private final File emptyTileFile;
	
	public FileRequestHandler(Path webRoot, String serverName) {
		this.webRoot = webRoot.normalize();
		this.serverName = serverName;

		this.emptyTileFile = webRoot.resolve("assets").resolve("emptyTile.json").toFile();
	}
	
	@Override
	public HttpResponse handle(HttpRequest request) {
		if (
			!request.getMethod().equalsIgnoreCase("GET") &&
			!request.getMethod().equalsIgnoreCase("POST") 
		) return new HttpResponse(HttpStatusCode.NOT_IMPLEMENTED); 
		
		HttpResponse response = generateResponse(request);
		response.addHeader("Server", this.serverName);
		
		HttpStatusCode status = response.getStatusCode();
		if (status.getCode() >= 400){
			response.setData(status.getCode() + " - " + status.getMessage() + "\n" + this.serverName);
		}
		
		return response;
	}

	@SuppressWarnings ("resource")
	private HttpResponse generateResponse(HttpRequest request) {
		String path = request.getPath();
		
		// normalize path
		if (path.startsWith("/")) path = path.substring(1);
		if (path.endsWith("/")) path = path.substring(0, path.length() - 1);

		Path filePath = webRoot;
		try {
			filePath = webRoot.resolve(path);
		} catch (InvalidPathException e){
			return new HttpResponse(HttpStatusCode.NOT_FOUND);
		}
		
		// can we use deflation?
		boolean isDeflationPossible = request.getLowercaseHeader("Accept-Encoding").contains("gzip");
		boolean isDeflated = false;
		
		// check if file is in web-root
		if (!filePath.normalize().startsWith(webRoot)){
			return new HttpResponse(HttpStatusCode.FORBIDDEN);
		}
		
		File file = filePath.toFile();
		
		// redirect to have correct relative paths
		if (file.isDirectory() && !request.getPath().endsWith("/")) {
			HttpResponse response = new HttpResponse(HttpStatusCode.SEE_OTHER);
			response.addHeader("Location", "/" + path + "/" + (request.getGETParamString().isEmpty() ? "" : "?" + request.getGETParamString()));
			return response;
		}
		
		if (!file.exists() || file.isDirectory()){
			file = new File(filePath.toString() + ".gz");
			isDeflated = true;
		}
		
		if (!file.exists() || file.isDirectory()){
			file = new File(filePath.toString() + "/index.html");
			isDeflated = false;
		}
		
		if (!file.exists() || file.isDirectory()){
			file = new File(filePath.toString() + "/index.html.gz");
			isDeflated = true;
		}
		
		if (!file.exists() && file.toPath().startsWith(webRoot.resolve("data"))){
			file = emptyTileFile;
			isDeflated = false;
		}

		if (!file.exists() || file.isDirectory()) {
			return new HttpResponse(HttpStatusCode.NOT_FOUND);
		}
		
		if (isDeflationPossible && (!file.getName().endsWith(".gz"))){
			File deflatedFile = new File(file.getAbsolutePath() + ".gz");
			if (deflatedFile.exists()){
				file = deflatedFile;
				isDeflated = true;
			}
		}
		
		// check if file is still in web-root and is not a directory
		if (!file.toPath().normalize().startsWith(webRoot) || file.isDirectory()){
			return new HttpResponse(HttpStatusCode.FORBIDDEN);
		}

		// check modified
		long lastModified = file.lastModified();
		Set<String> modStringSet = request.getHeader("If-Modified-Since");
		if (!modStringSet.isEmpty()){
			try {
				long since = stringToTimestamp(modStringSet.iterator().next());
				if (since + 1000 >= lastModified){
					return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
				}
			} catch (IllegalArgumentException ignored){}
		}
		
		//check ETag
		String eTag = Long.toHexString(file.length()) + Integer.toHexString(file.hashCode()) + Long.toHexString(lastModified);
		Set<String> etagStringSet = request.getHeader("If-None-Match");
		if (!etagStringSet.isEmpty()){
			if(etagStringSet.iterator().next().equals(eTag)) {
				return new HttpResponse(HttpStatusCode.NOT_MODIFIED);
			}
		}
		
		//create response
		HttpResponse response = new HttpResponse(HttpStatusCode.OK);
		response.addHeader("ETag", eTag);
		if (lastModified > 0) response.addHeader("Last-Modified", timestampToString(lastModified));
		response.addHeader("Cache-Control", "public");
		response.addHeader("Cache-Control", "max-age=" + TimeUnit.HOURS.toSeconds(1));
		
		//add content type header
		String filetype = file.getName();
		if (filetype.endsWith(".gz")) filetype = filetype.substring(0, filetype.length() - 3);
		int pointIndex = filetype.lastIndexOf('.');
		if (pointIndex >= 0) filetype = filetype.substring(pointIndex + 1);
		
		String contentType = "text/plain";
		switch (filetype) {
		case "json" :
			contentType = "application/json";
			break;
		case "png" :
			contentType = "image/png";
			break;
		case "jpg" :
		case "jpeg" :
		case "jpe" :
			contentType = "image/jpeg";
			break;
		case "svg" :
			contentType = "image/svg+xml";
			break;
		case "css" :
			contentType = "text/css";
			break;
		case "js" :
			contentType = "text/javascript";
			break;
		case "html" :
		case "htm" :
		case "shtml" :
			contentType = "text/html";
			break;
		case "xml" :
			contentType = "text/xml";
			break;
		}
		response.addHeader("Content-Type", contentType);
		

		try {	
			if (isDeflated){
				if (isDeflationPossible || file.length() > INFLATE_MAX_SIZE){
					response.addHeader("Content-Encoding", "gzip");
					response.setData(new FileInputStream(file));
					return response;
				} else {
					response.setData(new GZIPInputStream(new FileInputStream(file)));
					return response;
				}
			} else {
				if (isDeflationPossible && file.length() > DEFLATE_MIN_SIZE && file.length() < DEFLATE_MAX_SIZE){
					FileInputStream fis = new FileInputStream(file);
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					GZIPOutputStream zos = new GZIPOutputStream(byteOut);
					IOUtils.copyLarge(fis, zos);
					zos.close();
					fis.close();
					byte[] compressedData = byteOut.toByteArray();
					response.setData(new ByteArrayInputStream(compressedData));
					response.addHeader("Content-Encoding", "gzip");
					return response;
				} else {
					response.setData(new FileInputStream(file));
					return response;
				}
			}
			
		} catch (FileNotFoundException e) {
			return new HttpResponse(HttpStatusCode.NOT_FOUND);
		} catch (IOException e) {
			return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}
	
	private static String timestampToString(long time){
		return DateFormatUtils.format(time, "EEE, dd MMM yyy HH:mm:ss 'GMT'", TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
	}
	
	private static long stringToTimestamp(String timeString) throws IllegalArgumentException {
		try {
			int day = Integer.parseInt(timeString.substring(5, 7));
			int month = 1;
			switch (timeString.substring(8, 11)){
			case "Jan" : month = 0;  break;
			case "Feb" : month = 1;  break;
			case "Mar" : month = 2;  break;
			case "Apr" : month = 3;  break;
			case "May" : month = 4;  break;
			case "Jun" : month = 5;  break;
			case "Jul" : month = 6;  break;
			case "Aug" : month = 7;  break;
			case "Sep" : month = 8;  break;
			case "Oct" : month = 9; break;
			case "Nov" : month = 10; break;
			case "Dec" : month = 11; break;
			}
			int year = Integer.parseInt(timeString.substring(12, 16));
			int hour = Integer.parseInt(timeString.substring(17, 19));
			int min = Integer.parseInt(timeString.substring(20, 22));
			int sec = Integer.parseInt(timeString.substring(23, 25));
			GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			cal.set(year, month, day, hour, min, sec);
			return cal.getTimeInMillis();
		} catch (NumberFormatException | IndexOutOfBoundsException e){
			throw new IllegalArgumentException(e);
		}
	}
	
}
