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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HttpResponse implements Closeable {

    private static final byte[] CHUNK_SUFFIX = "\r\n".getBytes(StandardCharsets.UTF_8);

    private String version;
    private HttpStatusCode statusCode;
    private final Map<String, HttpHeader> headers;
    private ReadableByteChannel data;

    private ByteBuffer headerData;
    private ByteBuffer dataBuffer;
    private boolean complete = false;
    private boolean headerComplete = false;
    private boolean dataChannelComplete = false;
    private boolean dataComplete = false;

    public HttpResponse(HttpStatusCode statusCode) {
        this.version = "HTTP/1.1";
        this.statusCode = statusCode;

        this.headers = new HashMap<>();
    }

    public synchronized boolean read(WritableByteChannel channel) throws IOException {
        if (complete) return true;

        // send headers
        if (!headerComplete) {
            if (headerData == null) writeHeaderData();
            if (headerData.hasRemaining()) {
                channel.write(headerData);
            }

            if (headerData.hasRemaining()) return false;
            headerComplete = true;
            headerData = null; // free ram
        }

        if (!hasData()){
            complete = true;
            return true;
        }

        // send data chunked
        if (dataBuffer == null) dataBuffer = ByteBuffer.allocate(1024 + 200).flip(); // 200 extra bytes
        while (true) {
            if (dataBuffer.hasRemaining()) channel.write(dataBuffer);
            if (dataBuffer.hasRemaining()) return false;
            if (dataComplete) break; // nothing more to do

            // fill data buffer from channel
            dataBuffer.clear();
            dataBuffer.position(100); // keep 100 space in front
            dataBuffer.limit(1124); // keep 100 space at the end

            int readTotal = 0;
            if (!dataChannelComplete) {
                int read = 0;
                while (dataBuffer.hasRemaining() && (read = data.read(dataBuffer)) != -1) {
                    readTotal += read;
                }

                if (read == -1) dataChannelComplete = true;
            }

            if (readTotal == 0) dataComplete = true;

            byte[] chunkPrefix = (Integer.toHexString(readTotal) + "\r\n")
                    .getBytes(StandardCharsets.UTF_8);

            dataBuffer.limit(dataBuffer.capacity());
            dataBuffer.put(CHUNK_SUFFIX);
            dataBuffer.limit(dataBuffer.position());

            int startPos = 100 - chunkPrefix.length;
            dataBuffer.position(startPos);
            dataBuffer.put(chunkPrefix);
            dataBuffer.position(startPos);
        }

        complete = true;
        return true;
    }

    private void writeHeaderData() {
        ByteArrayOutputStream headerDataOut = new ByteArrayOutputStream();

        if (hasData()){
            headers.put("Transfer-Encoding", new HttpHeader("Transfer-Encoding", "chunked"));
        } else {
            headers.put("Content-Length", new HttpHeader("Content-Length", "0"));
        }

        headerDataOut.writeBytes((version + " " + statusCode.getCode() + " " + statusCode.getMessage() + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
        for (HttpHeader header : headers.values()){
            headerDataOut.writeBytes((header.getKey() + ": " + header.getValue() + "\r\n")
                    .getBytes(StandardCharsets.UTF_8));
        }
        headerDataOut.writeBytes(("\r\n")
                .getBytes(StandardCharsets.UTF_8));

        headerData = ByteBuffer.allocate(headerDataOut.size())
                .put(headerDataOut.toByteArray())
                .flip();
    }

    public void addHeader(String key, String value){
        HttpHeader header;
        HttpHeader existing = getHeader(key);
        if (existing != null) {
            header = new HttpHeader(existing.getKey(), existing.getValue() + ", " + value);
        } else {
            header = new HttpHeader(key, value);
        }
        this.headers.put(key.toLowerCase(Locale.ROOT), header);
    }

    public void setData(ReadableByteChannel channel){
        this.data = channel;
    }

    public void setData(InputStream dataStream){
        this.data = Channels.newChannel(dataStream);
    }

    public void setData(String data){
        setData(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean hasData() {
        return this.data != null;
    }

    public boolean isComplete() {
        return complete;
    }

    @Override
    public void close() throws IOException {
        if (data != null) data.close();
    }

    public HttpStatusCode getStatusCode(){
        return statusCode;
    }

    public void setStatusCode(HttpStatusCode statusCode) {
        this.statusCode = statusCode;
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

}
