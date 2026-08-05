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

@RequiredArgsConstructor
public class HttpResponse implements Closeable, HttpHeaderCarrier {

    private @Getter @Setter @NonNull String version = "HTTP/1.1";
    private @Getter @Setter @NonNull HttpStatusCode statusCode;
    private @Getter @Setter @NonNull @Singular Map<String, HttpHeader> headers = new LinkedHashMap<>();

    /**
     * The response body.
     *
     * Can be stored as either an {@link InputStream} or an {@link HttpResponseStreamWriter}.
     * The {@link #streamWriter} is used for responses that push data over time (Server-Sent Events).
     */
    private @Nullable InputStream body;
    private @Nullable HttpResponseStreamWriter streamWriter;

    public void setBody(@Nullable InputStream body) {
        this.streamWriter = null;
        this.body = body;
    }

    public void setBody(byte[] data) {
        this.streamWriter = null;
        if (data == null) {
            this.body = null;
            return;
        }

        setBody(new ByteArrayInputStream(data));
    }

    public void setBody(String data) {
        this.streamWriter = null;
        if (data == null) {
            this.body = null;
            return;
        }

        setBody(data.getBytes(StandardCharsets.UTF_8));
    }

    public void setBody(@Nullable HttpResponseStreamWriter streamWriter) {
        this.body = null;
        this.streamWriter = streamWriter;
    }

    public boolean hasBody() {
        return body != null || streamWriter != null;
    }

    @Override
    public void close() throws IOException {
        if (body != null) body.close();
    }

    /**
     * Returns {@link #streamWriter} if set, otherwise adapts {@link #body} into a
     * {@link HttpResponseStreamWriter} that writes data in chunks.
     * Returns {@code null} if neither is set.
     */
    @Nullable HttpResponseStreamWriter resolveStreamWriter() {
        if (streamWriter != null) return streamWriter;
        if (body == null) return null;

        byte[] byteBuffer = new byte[1024];
        return out -> {
            while (true) {
                int read = body.read(byteBuffer);
                if (read == -1) break;
                if (read == 0) continue;
                out.writeChunk(byteBuffer, 0, read);
            }
        };
    }

}
