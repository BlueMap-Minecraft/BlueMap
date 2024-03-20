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

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTDeserializer;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

@NBTDeserializer(BlockEntity.BlockEntityDeserializer.class)
public class BlockEntity {
    private static final BlueNBT BLUENBT = new BlueNBT();

    @FunctionalInterface
    private interface BlockEntityInitializer {
        BlockEntity create(Map<String, Object> data);
    }

    @SuppressWarnings("StaticInitializerReferencesSubClass")
    private static final Map<String, BlockEntityInitializer> ID_MAPPING = Map.of(
            "minecraft:sign", SignBlockEntity::new,
            "minecraft:skull", SkullBlockEntity::new,
            "minecraft:banner", BannerBlockEntity::new
    );

    protected final String id;
    protected final int x, y, z;
    protected final boolean keepPacked;

    protected BlockEntity(Map<String, Object> data) {
        this.id = (String) data.get("id");
        this.x = (int) data.get("x");
        this.y = (int) data.get("y");
        this.z = (int) data.get("z");
        this.keepPacked = (byte) data.getOrDefault("keepPacked", (byte) 0) == 1;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isKeepPacked() {
        return keepPacked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockEntity that = (BlockEntity) o;
        return x == that.x && y == that.y && z == that.z && keepPacked == that.keepPacked && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, z, keepPacked);
    }

    @Override
    public String toString() {
        return "BlockEntity{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", keepPacked=" + keepPacked +
                '}';
    }

    public static class BlockEntityDeserializer implements TypeDeserializer<BlockEntity> {
        @Override
        public BlockEntity read(NBTReader reader) throws IOException {
            @SuppressWarnings("unchecked") Map<String, Object> data =
                    (Map<String, Object>) BLUENBT.read(reader, TypeToken.getParameterized(Map.class, String.class, Object.class));

            String id = (String) data.get("id");
            if (id == null || id.isBlank()) {
                return null;
            }

            BlockEntityInitializer instance = ID_MAPPING.getOrDefault(id, BlockEntity::new);

            try {
                return instance.create(data);
            } catch (Exception e) {
                Logger.global.logError("Failed to instantiate BlockEntity instance!", e);
            }

            return null;
        }
    }
}
