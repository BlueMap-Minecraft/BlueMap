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
		} catch (Exception ex) {}
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
