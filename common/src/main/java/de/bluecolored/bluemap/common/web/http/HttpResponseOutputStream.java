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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class HttpResponseOutputStream implements Closeable {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

    private final OutputStream outputStream;

    public void write(HttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        HttpResponseStreamWriter streamWriter = response.resolveStreamWriter();

        writeLine(response.getVersion() + " " + statusCode.getCode() + " " + statusCode.getMessage());

        // headers
        if (streamWriter != null) {
            response.addHeader("Transfer-Encoding","chunked");
        } else {
            response.addHeader("Content-Length", "0");
        }
        for (HttpHeader header : response.getHeaders().values()) {
            writeLine(header.getKey() + ": " + header.getValue());
        }
        writeLine();
        outputStream.flush();  // ensure headers are always immediately pushed to the client

        // body
        if (streamWriter != null) {
            try (ChunkedOutputStream chunkedOut = new ChunkedOutputStream(outputStream)){
                streamWriter.write(chunkedOut);
            }
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
