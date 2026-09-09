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
package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockStateDeserializer implements TypeDeserializer<BlockState> {

    private final @Nullable DataPack dataPack;

    public BlockStateDeserializer() {
        this.dataPack = null;
    }

    public BlockStateDeserializer(@Nullable DataPack dataPack) {
        this.dataPack = dataPack;
    }

    @Override
    public BlockState read(NBTReader reader) throws IOException {
        if (reader.peek() == TagType.STRING)
            return getDefaultBlockstate(reader.nextString());

        reader.beginCompound();

        boolean defaultBlockState = false;
        String id = null;
        Map<String, String> properties = null;

        while (reader.hasNext()) {
            switch (reader.name()) {
                case "id", "Name" -> id = reader.nextString();
                case "properties", "Properties" -> {
                    properties = new LinkedHashMap<>();
                    reader.beginCompound();
                    while (reader.hasNext())
                        properties.put(reader.name(), reader.nextString());
                    reader.endCompound();
                }
                case "" -> {
                    id = reader.nextString();
                    defaultBlockState = true;
                }
                default -> reader.skip();
            }
        }

        reader.endCompound();

        if (id == null) throw new IOException("Invalid BlockState, Name is missing!");
        if (defaultBlockState && properties == null) return getDefaultBlockstate(id);

        Key key = Key.parse(id);
        return properties == null ? new BlockState(key) : new BlockState(key, properties);
    }

    private BlockState getDefaultBlockstate(String blockId) {
        Key key = Key.parse(blockId, Key.MINECRAFT_NAMESPACE);
        if (dataPack != null){
            BlockState blockState = dataPack.getDefaultBlockState(key);
            if (blockState != null) return blockState;
        }
        return new BlockState(key);
    }

}