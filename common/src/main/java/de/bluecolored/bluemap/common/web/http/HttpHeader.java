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

import lombok.Getter;

import java.util.*;

public class HttpHeader {

    @Getter private final String key;
    @Getter private String value;
    private List<String> values;
    private Set<String> valuesLC;

    public HttpHeader(String key, String... values) {
        this.key = key;
        this.value = String.join(",", values);
    }

    public synchronized void add(String... values) {
        if (value.isEmpty()) {
            set(values);
            return;
        }

        this.value = value + "," + String.join(",", values);
        this.values = null;
        this.valuesLC = null;
    }

    public synchronized void set(String... values) {
        this.value = String.join(",", values);
        this.values = null;
        this.valuesLC = null;
    }

    public synchronized List<String> getValues() {
        if (values == null) {
            List<String> vs = new ArrayList<>();
            for (String v : value.split(",")) {
                vs.add(v.trim());
            }
            values = Collections.unmodifiableList(vs);
        }

        return values;
    }

    public synchronized boolean contains(String value) {
        if (valuesLC == null) {
            valuesLC = new HashSet<>();
            for (String v : getValues()) {
                valuesLC.add(v.toLowerCase(Locale.ROOT));
            }
        }

        return valuesLC.contains(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HttpHeader that = (HttpHeader) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }

}
