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
package de.bluecolored.bluemap.common.webserver;

import de.bluecolored.bluemap.common.webserver.HttpConnection.ConnectionClosedException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OriginalHttpRequest extends HttpRequest {

    private static final Pattern REQUEST_PATTERN = Pattern.compile("^(\\w+) (\\S+) (.+)$");

    private final String method;
    private final String address;
    private final String version;
    private final Map<String, Set<String>> header;
    private final Map<String, Set<String>> headerLC;
    private byte[] data;

    public OriginalHttpRequest(String method, String address, String version, Map<String, Set<String>> header) {
        this.method = method;
        this.address = address;
        this.version = version;
        this.header = header;
        this.headerLC = new HashMap<>();

        for (Entry<String, Set<String>> e : header.entrySet()){
            Set<String> values = new HashSet<>();
            for (String v : e.getValue()){
                values.add(v.toLowerCase());
            }

            headerLC.put(e.getKey().toLowerCase(), values);
        }

        this.data = new byte[0];
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getAddress(){
        return address;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Map<String, Set<String>> getHeader() {
        return header;
    }

    @Override
    public Map<String, Set<String>> getLowercaseHeader() {
        return headerLC;
    }

    @Override
    public Set<String> getHeader(String key){
        Set<String> headerValues = header.get(key);
        if (headerValues == null) return Collections.emptySet();
        return headerValues;
    }

    @Override
    public Set<String> getLowercaseHeader(String key){
        Set<String> headerValues = headerLC.get(key.toLowerCase());
        if (headerValues == null) return Collections.emptySet();
        return headerValues;
    }

    @Override
    public InputStream getData(){
        return new ByteArrayInputStream(data);
    }

    private static String readLine(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null){
            throw new ConnectionClosedException();
        }
        return line;
    }

    public static HttpRequest read(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        List<String> header = new ArrayList<>(20);
        while (header.size() < 1000) {
            String headerLine = OriginalHttpRequest.readLine(reader);
            if (headerLine.isEmpty()) break;
            header.add(headerLine);
        }

        if (header.isEmpty()) throw new HttpConnection.InvalidRequestException();

        Matcher m = OriginalHttpRequest.REQUEST_PATTERN.matcher(header.remove(0));
        if (!m.find()) throw new HttpConnection.InvalidRequestException();

        String method = m.group(1);
        if (method == null) throw new HttpConnection.InvalidRequestException();

        String address = m.group(2);
        if (address == null) throw new HttpConnection.InvalidRequestException();

        String version = m.group(3);
        if (version == null) throw new HttpConnection.InvalidRequestException();

        Map<String, Set<String>> headerMap = new HashMap<>();
        for (String line : header) {
            if (line.trim().isEmpty()) continue;

            String[] kv = line.split(":", 2);
            if (kv.length < 2) continue;

            Set<String> values = new HashSet<>();
            if (kv[0].trim().equalsIgnoreCase("If-Modified-Since")) {
                values.add(kv[1].trim());
            } else {
                for (String v : kv[1].split(",")) {
                    values.add(v.trim());
                }
            }

            headerMap.put(kv[0].trim(), values);
        }

        OriginalHttpRequest request = new OriginalHttpRequest(method, address, version, headerMap);

        if (request.getLowercaseHeader("Transfer-Encoding").contains("chunked")) {
            try {
                ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                while (dataStream.size() < 1000000) {
                    String hexSize = reader.readLine();
                    int chunkSize = Integer.parseInt(hexSize, 16);
                    if (chunkSize <= 0) break;
                    byte[] data = new byte[chunkSize];
                    in.read(data);
                    dataStream.write(data);
                }

                if (dataStream.size() >= 1000000) {
                    throw new HttpConnection.InvalidRequestException();
                }

                request.data = dataStream.toByteArray();

                return request;
            } catch (NumberFormatException ex) {
                return request;
            }
        } else {
            Set<String> clSet = request.getLowercaseHeader("Content-Length");
            if (clSet.isEmpty()) {
                return request;
            } else {
                try {
                    int cl = Integer.parseInt(clSet.iterator().next());
                    byte[] data = new byte[cl];
                    in.read(data);
                    request.data = data;
                    return request;
                } catch (NumberFormatException ex) {
                    return request;
                }
            }
        }
    }

}
