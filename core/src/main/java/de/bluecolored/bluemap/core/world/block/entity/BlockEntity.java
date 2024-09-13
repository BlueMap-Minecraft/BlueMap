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
package de.bluecolored.bluemap.core.world.block.entity;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluenbt.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
@NBTDeserializer(BlockEntity.BlockEntityDeserializer.class)
public abstract class BlockEntity {

    protected final String id;
    protected final int x, y, z;
    protected final boolean keepPacked;

    protected BlockEntity(Map<String, Object> raw) {
        this.id = (String) raw.get("id");
        this.x = (int) raw.getOrDefault("x", 0);
        this.y = (int) raw.getOrDefault("y", 0);
        this.z = (int) raw.getOrDefault("z", 0);
        this.keepPacked = (byte) raw.getOrDefault("keepPacked", (byte) 0) == 1;
    }

    @RequiredArgsConstructor
    public static class BlockEntityDeserializer implements TypeDeserializer<BlockEntity> {

        private final BlueNBT blueNBT;

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable BlockEntity read(NBTReader reader) throws IOException {
            Map<String, Object> raw = (Map<String, Object>) blueNBT.read(reader, TypeToken.of(Map.class, String.class, Object.class));

            String id = (String) raw.get("id");
            if (id == null) return null;

            Key typeKey = Key.parse(id, Key.MINECRAFT_NAMESPACE);
            BlockEntityType type = BlockEntityType.REGISTRY.get(typeKey);
            if (type == null) return null;

            return type.load(raw);
        }

    }

}
