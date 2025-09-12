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
package de.bluecolored.bluemap.common.web.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequest {

    private static final Pattern REQUEST_PATTERN = Pattern.compile("^(\\w+) (\\S+) (.+)$");

    // reading helper
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    private final StringBuffer lineBuffer = new StringBuffer();

    private boolean complete = false;
    private boolean headerComplete = false;
    private final List<String> headerLines = new ArrayList<>(20);

    // request data
    private final InetAddress source;
    private URI address;
    private String method, version;
    private final Map<String, HttpHeader> headers = new HashMap<>();
    private byte[] data;

    // these values can be overwritten separately by a HttpRequestHandler for delegation
    private String path = null;
    private String getParamString = null;
    private Map<String, String> getParams = null;

    public HttpRequest(InetAddress source) {
        this.source = source;
    }

    public boolean write(ReadableByteChannel channel) throws IOException {
        if (complete) return true;

        int read = channel.read(byteBuffer);
        if (read == 0) return false;
        if (read == -1) {
            channel.close();
            return false;
        }

        byteBuffer.flip();
        try {

            // read headers
            while (!headerComplete) {
                if (!writeLine()) return false;
                String line = lineBuffer.toString().stripTrailing();
                lineBuffer.setLength(0);

                if (line.isEmpty()) {
                    headerComplete = true;
                    parseHeaders();
                } else {
                    headerLines.add(line);
                }
            }

            if (hasHeaderValue("transfer-encoding", "chunked")) {
                writeChunkedBody();
            } else {
                HttpHeader contentLengthHeader = getHeader("content-length");
                int contentLength = 0;
                if (contentLengthHeader != null) {
                    try {
                        contentLength = Integer.parseInt(contentLengthHeader.getValue().trim());
                    } catch (NumberFormatException ex) {
                        throw new IOException("Invalid HTTP Request: content-length is not a number", ex);
                    }
                }

                if (contentLength > 0) {
                    writeBody(contentLength);
                }
            }

            complete = true;
            return true;

        } finally {
            byteBuffer.compact();
        }
    }

    private void writeChunkedBody() {
        // TODO
    }

    private void writeBody(int length) {
        // TODO
    }

    private void parseHeaders() throws IOException {
        if (headerLines.isEmpty()) throw new IOException("Invalid HTTP Request: No Header");

        Matcher m = REQUEST_PATTERN.matcher(headerLines.get(0));
        if (!m.find()) throw new IOException("Invalid HTTP Request: Request-Pattern not matching");

        method = m.group(1);
        if (method == null) throw new IOException("Invalid HTTP Request: Request-Pattern not matching (method)");

        String addressString = m.group(2);
        if (addressString == null) throw new IOException("Invalid HTTP Request: Request-Pattern not matching (address)");
        try {
            address = new URI(addressString);
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid HTTP Request: Request-URI is invalid", ex);
        }

        version = m.group(3);
        if (version == null) throw new IOException("Invalid HTTP Request: Request-Pattern not matching (version)");

        headers.clear();
        for (int i = 1; i < headerLines.size(); i++) {
            String line = headerLines.get(i);
            if (line.trim().isEmpty()) continue;

            String[] kv = line.split(":", 2);
            if (kv.length < 2) continue;

            headers.put(kv[0].trim().toLowerCase(Locale.ROOT), new HttpHeader(kv[0], kv[1]));
        }
    }

    private boolean writeLine() {
        while (lineBuffer.length() <= 0 || lineBuffer.charAt(lineBuffer.length() - 1) != '\n'){
            if (!byteBuffer.hasRemaining()) return false;
            lineBuffer.append((char) byteBuffer.get());
        }
        return true;
    }

    public InetAddress getSource() {
        return source;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public URI getAddress() {
        return address;
    }

    public void setAddress(URI address) {
        this.address = address;
        this.path = null;
        this.getParams = null;
        this.getParamString = null;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, HttpHeader> getHeaders() {
        return headers;
    }

    public HttpHeader getHeader(String header) {
        return this.headers.get(header.toLowerCase(Locale.ROOT));
    }

    public boolean hasHeaderValue(String key, String value) {
        HttpHeader header = getHeader(key);
        if (header == null) return false;
        return header.contains(value);
    }

    public byte[] getData() {
        return data;
    }

    public InputStream getDataStream() {
        return new ByteArrayInputStream(data);
    }

    public String getPath() {
        if (path == null) parseAddress();
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getGETParams() {
        if (getParams == null) parseGetParams();
        return getParams;
    }

    public String getGETParamString() {
        if (getParamString == null) parseAddress();
        return getParamString;
    }

    public void setGetParamString(String getParamString) {
        this.getParamString = getParamString;
        this.getParams = null;
    }

    private void parseAddress() {
        this.path = address.getPath();
        this.getParamString = address.getQuery();
    }

    private void parseGetParams() {
        Map<String, String> getParams = new HashMap<>();
        for (String getParam : this.getGETParamString().split("&")){
            if (getParam.isEmpty()) continue;
            String[] kv = getParam.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            getParams.put(key, value);
        }
        this.getParams = getParams;
    }

    public boolean isComplete() {
        return complete;
    }

    public void clear() {
        byteBuffer.clear();
        lineBuffer.setLength(0);

        complete = false;
        headerComplete = false;
        headerLines.clear();

        method = null;
        address = null;
        version = null;
        headers.clear();
        data = null;

        path = null;
        getParamString = null;
        getParams = null;
    }

}
