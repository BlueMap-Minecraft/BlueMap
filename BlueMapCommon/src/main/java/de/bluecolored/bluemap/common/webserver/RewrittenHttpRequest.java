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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RewrittenHttpRequest extends HttpRequest {

    private final HttpRequest originalRequest;

    private String method = null;
    private String address = null;
    private String version = null;
    private Map<String, Set<String>> header = null;
    private Map<String, Set<String>> headerLC = null;
    private byte[] data = null;

    public RewrittenHttpRequest(HttpRequest originalRequest) {
        this.originalRequest = originalRequest;

    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getMethod() {
        return method != null ? method : originalRequest.getMethod();
    }

    public void setAddress(String address) {
        this.address = address;
        parseAddress();
    }

    @Override
    public String getAddress() {
        return address != null ? address : originalRequest.getAddress();
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getVersion() {
        return version != null ? version : originalRequest.getVersion();
    }

    public void setHeader(Map<String, Set<String>> header) {
        this.header = header;
        this.headerLC = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : header.entrySet()){
            Set<String> values = new HashSet<>();
            for (String v : e.getValue()){
                values.add(v.toLowerCase());
            }

            headerLC.put(e.getKey().toLowerCase(), values);
        }
    }

    @Override
    public Map<String, Set<String>> getHeader() {
        return header != null ? header : originalRequest.getHeader();
    }

    @Override
    public Map<String, Set<String>> getLowercaseHeader() {
        return headerLC != null ? headerLC : originalRequest.getLowercaseHeader();
    }

    public void setHeader(String key, String value) {
        if (header == null || headerLC == null) {
            header = new HashMap<>(originalRequest.getHeader());
            headerLC = new HashMap<>(originalRequest.getLowercaseHeader());
        }

        header.computeIfAbsent(key, k -> new HashSet<>()).add(value);
        headerLC.computeIfAbsent(key.toLowerCase(), k -> new HashSet<>()).add(value.toLowerCase());
    }

    public void removeHeader(String key) {
        if (header == null || headerLC == null) {
            header = new HashMap<>(originalRequest.getHeader());
            headerLC = new HashMap<>(originalRequest.getLowercaseHeader());
        }

        header.remove(key);
        headerLC.remove(key.toLowerCase());
    }

    @Override
    public Set<String> getHeader(String key) {
        return header != null ? header.get(key) : originalRequest.getHeader(key);
    }

    @Override
    public Set<String> getLowercaseHeader(String key) {
        return headerLC != null ? headerLC.get(key.toLowerCase()) : originalRequest.getLowercaseHeader(key);
    }

    public void setPath(String path) {
        if (getGETParamString().isEmpty()) this.address = path;
        else this.address = path + "?" + getGETParamString();
        parseAddress();
    }

    @Override
    public String getPath() {
        if (address == null) return originalRequest.getPath();
        return super.getPath();
    }

    @Override
    public Map<String, String> getGETParams() {
        if (address == null) return originalRequest.getGETParams();
        return super.getGETParams();
    }

    public void setGETParamString(String paramString) {
        this.address = getAddress() + "?" + paramString;
        parseAddress();
    }

    @Override
    public String getGETParamString() {
        if (address == null) return originalRequest.getGETParamString();
        return super.getGETParamString();
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public InputStream getData() {
        return data != null ? new ByteArrayInputStream(data) : originalRequest.getData();
    }

    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }

}
