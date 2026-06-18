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
package de.bluecolored.bluemap.common.rendermanager.serialization;

import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeAdapter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class BmMapAdapter implements TypeAdapter<BmMap> {

    private final BlueMapService blueMap;

    @Override
    public BmMap read(NBTReader reader) throws IOException {
        BmMap map = blueMap.getMaps().get(reader.nextString());
        if (map == null) throw new IOException("No map with id '" + reader.nextString() + "' loaded.");
        return map;
    }

    @Override
    public void write(BmMap value, NBTWriter writer) throws IOException {
        writer.value(value.getId());
    }

    @Override
    public TagType type() {
        return TagType.STRING;
    }

}
