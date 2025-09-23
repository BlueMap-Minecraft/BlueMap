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

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequestInputStream implements Closeable {

    private static final Pattern REQUEST_PATTERN = Pattern.compile("^(\\w+) (\\S+) (.+)$");

    private final InetAddress source;
    private final DataInputStream in;
    private final Reader reader;

    private byte[] byteBuffer = new byte[1024];
    private final char[] charBuffer = new char[1];

    public HttpRequestInputStream(InputStream in, InetAddress source) {
        this.source = source;
        this.in = new DataInputStream(in);
        this.reader = new InputStreamReader(this.in, StandardCharsets.UTF_8);
    }

    public @Nullable HttpRequest read() throws IOException {

        String requestLine;
        requestLine = readLine();

        Matcher m = REQUEST_PATTERN.matcher(requestLine);
        if (!m.find()) throw new IOException("Invalid HTTP Request: Request-Pattern not matching '%s'".formatted(requestLine));

        URI address = URI.create(m.group(2));

        HttpRequest request = new HttpRequest(
                source,
                m.group(1),
                address.getPath()
        );
        request.setVersion(m.group(3));
        request.setRawQueryString(address.getRawQuery());

        // headers
        while (true) {
            String line = readLine();
            if (line.isBlank()) break;

            String[] kv = line.split(":", 2);
            if (kv.length < 2) continue;

            request.addHeader(kv[0], kv[1].trim());
        }

        // body
        if (request.hasHeaderValue("transfer-encoding", "chunked")) {
            request.setBody(readChunkedBody());
        } else {
            HttpHeader contentLengthHeader = request.getHeader("content-length");
            int contentLength = 0;
            if (contentLengthHeader != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthHeader.getValue().trim());
                } catch (NumberFormatException ex) {
                    throw new IOException("Invalid HTTP Request: content-length is not a number", ex);
                }
            }

            if (contentLength > 0) {
                request.setBody(readBody(contentLength));
            }
        }

        return request;
    }

    private String readLine() throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        do {
            if (reader.read(charBuffer, 0, 1) == -1) throw new EOFException();
            stringBuilder.append(charBuffer, 0, 1);
        } while (charBuffer[0] != '\n');

        return stringBuilder.toString();
    }

    private byte[] readChunkedBody() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

        while (true) {
            String prefix = readLine();
            int size = Integer.valueOf(prefix.formatted(), 16);
            if (size > byteBuffer.length) byteBuffer = new byte[size];
            size = in.readNBytes(byteBuffer, 0, size);
            body.write(byteBuffer, 0, size);
            readLine(); // suffix
            if (size == 0) break;
        }

        return body.toByteArray();
    }

    private byte[] readBody(int contentLength) throws IOException {
        return in.readNBytes(contentLength);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
