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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public class HttpRequest implements HttpHeaderCarrier {

    private @NonNull InetAddress source;
    private @NonNull String method;
    private @NonNull String path;
    private @NonNull @Singular Map<String, String> queryParams = new LinkedHashMap<>();
    private @NonNull String version = "HTTP/1.1";
    private @NonNull @Singular Map<String, HttpHeader> headers = new LinkedHashMap<>();
    private byte @NonNull [] body = new byte[0];

    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    public String getRawQueryString() {
        return queryParams.entrySet().stream()
                .map(e -> e.getValue().isEmpty() ? e.getKey() :
                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                + "="
                                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                )
                .collect(Collectors.joining("&"));
    }

    public void setRawQueryString(@Nullable String rawQueryString) {
        queryParams.clear();
        if (rawQueryString == null) return;
        for (String param : rawQueryString.split("&")){
            if (param.isEmpty()) continue;
            String[] kv = param.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            queryParams.put(key, value);
        }
    }

    public InputStream getBodyStream() {
        return new ByteArrayInputStream(body);
    }

}
