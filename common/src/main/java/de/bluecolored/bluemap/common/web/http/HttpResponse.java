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

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class HttpResponse implements Closeable, HttpHeaderCarrier {

    private @NonNull String version = "HTTP/1.1";
    private @NonNull HttpStatusCode statusCode;
    private @NonNull @Singular Map<String, HttpHeader> headers = new LinkedHashMap<>();
    private @Nullable HttpResponseStreamWriter body;

    public void setBody(@Nullable InputStream body) {
        this.body = body == null ? null : asStreamWriter(body);
    }

    public void setBody(byte[] data) {
        setBody(data == null ? null : new ByteArrayInputStream(data));
    }

    public void setBody(String data) {
        setBody(data == null ? null : data.getBytes(StandardCharsets.UTF_8));
    }

    public void setBody(@Nullable HttpResponseStreamWriter streamWriter) {
        this.body = streamWriter;
    }

    @Override
    public void close() throws IOException {
        if (body != null) body.close();
    }

    /**
     * Adapt an {@link InputStream} body into a {@link HttpResponseStreamWriter}
     * that writes the data to the client in chunks.
     */
    private static HttpResponseStreamWriter asStreamWriter(InputStream body) {
        return new HttpResponseStreamWriter() {
            private final byte[] byteBuffer = new byte[1024];

            @Override
            public void write(ChunkedOutputStream out) throws IOException {
                while (true) {
                    int read = body.read(byteBuffer);
                    if (read == -1) break;
                    if (read == 0) continue;
                    out.writeChunk(byteBuffer, 0, read);
                }
            }

            @Override
            public void close() throws IOException {
                body.close();
            }
        };
    }

}
