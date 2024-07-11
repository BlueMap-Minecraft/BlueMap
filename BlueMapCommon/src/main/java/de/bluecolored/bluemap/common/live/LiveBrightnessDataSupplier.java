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
package de.bluecolored.bluemap.common.live;

import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

public class LiveBrightnessDataSupplier implements Supplier<String> {

    private final Server server;

    public LiveBrightnessDataSupplier(Server server) {
        this.server = server;
    }

    @Override
    public String get() {
        String dimensionName;
        Key dimension;

        try (StringWriter jsonString = new StringWriter();
            JsonWriter json = new JsonWriter(jsonString)) {
            

            json.beginObject();

            json.name("brightness").beginArray();
            for (Map.Entry<Key, Integer> entry : this.server.getSkyBrightness().entrySet()) {
                dimension = entry.getKey();
                dimensionName = dimension.getNamespace().equals("minecraft") ? 
                    dimension.getValue() : dimension.getFormatted();
                json.beginObject();
                json.name("dimension").value(dimensionName);
                json.name("val").value(entry.getValue());
                json.endObject();
            }
            json.endArray();

            json.endObject();

            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/brightness json!", ex);
            return "BlueMap - Exception handling this request";
        }
    }

}
