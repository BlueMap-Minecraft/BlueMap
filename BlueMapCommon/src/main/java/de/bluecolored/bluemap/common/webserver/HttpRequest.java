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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class HttpRequest {

    private String path = null;
    private Map<String, String> getParams = null;
    private String getParamString = null;

    public abstract String getMethod();

    public abstract String getAddress();

    public abstract String getVersion();

    public abstract Map<String, Set<String>> getHeader();

    public abstract Map<String, Set<String>> getLowercaseHeader();

    public abstract Set<String> getHeader(String key);

    public abstract Set<String> getLowercaseHeader(String key);

    public String getPath() {
        if (path == null) parseAddress();
        return path;
    }

    public Map<String, String> getGETParams() {
        if (getParams == null) parseAddress();
        return Collections.unmodifiableMap(getParams);
    }

    public String getGETParamString() {
        if (getParamString == null) parseAddress();
        return getParamString;
    }

    protected void parseAddress() {
        String address = this.getAddress();
        if (address.isEmpty()) address = "/";
        String[] addressParts = address.split("\\?", 2);
        String path = addressParts[0];
        this.getParamString = addressParts.length > 1 ? addressParts[1] : "";

        Map<String, String> getParams = new HashMap<>();
        for (String getParam : this.getParamString.split("&")){
            if (getParam.isEmpty()) continue;
            String[] kv = getParam.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            getParams.put(key, value);
        }

        this.path = path;
        this.getParams = getParams;
    }

    public abstract InputStream getData();

    static HttpRequest read(InputStream in) throws IOException {
        return OriginalHttpRequest.read(in);
    }

}
