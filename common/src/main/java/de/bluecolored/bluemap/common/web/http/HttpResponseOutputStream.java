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

import lombok.RequiredArgsConstructor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class HttpResponseOutputStream implements Closeable {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private final OutputStream outputStream;

    private final byte[] byteBuffer = new byte[1024];

    public void write(HttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        InputStream body = response.getBody();

        writeLine(response.getVersion() + " " + statusCode.getCode() + " " + statusCode.getMessage());

        // headers
        if (body != null) {
            response.addHeader("Transfer-Encoding","chunked");
        } else {
            response.addHeader("Content-Length", "0");
        }
        for (HttpHeader header : response.getHeaders().values()) {
            writeLine(header.getKey() + ": " + header.getValue());
        }
        writeLine();

        // body
        if (body != null) {

            while (true) {
                int read = body.read(byteBuffer);
                if (read == -1) break;
                if (read == 0) continue;
                writeLine(Integer.toHexString(read));
                outputStream.write(byteBuffer, 0, read);
                writeLine();
            }

            writeLine(Integer.toHexString(0));
            writeLine();
        }

        outputStream.flush();
    }

    private void writeLine() throws IOException {
        outputStream.write(CRLF);
    }

    private void writeLine(String line) throws IOException {
        outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        outputStream.write(CRLF);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

}
