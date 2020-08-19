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
package de.bluecolored.bluemap.core.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;

import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;

public class Metrics {

	private static final String METRICS_REPORT_URL = "https://metrics.bluecolored.de/bluemap/";

	public static void sendReportAsync(String implementation) {
		new Thread(() -> sendReport(implementation)).start();
	}
	
	public static void sendReport(String implementation) {
		JsonObject data = new JsonObject();
		data.addProperty("implementation", implementation);
		data.addProperty("version", BlueMap.VERSION);
		
		try {
			sendData(data.toString());
		} catch (IOException | RuntimeException ex) {
			Logger.global.logDebug("Failed to send Metrics-Report: " + ex);
		}
	}
	
	private static String sendData(String data) throws MalformedURLException, IOException {
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		
        HttpsURLConnection connection = (HttpsURLConnection) new URL(METRICS_REPORT_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Content-Length", String.valueOf(bytes.length));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Connection", "close");
        connection.setRequestProperty("User-Agent", "BlueMap");
        connection.setDoOutput(true);
        
        try (OutputStream out = connection.getOutputStream()){
	        out.write(bytes);
	        out.flush();
        }
        
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        	String line;
        	StringBuilder builder = new StringBuilder();
        	
        	while ((line = in.readLine()) != null) {
        		builder.append(line + "\n");
        	}
        	
        	return builder.toString(); 
        }
        
	}
	
}
