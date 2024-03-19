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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SkullBlockEntity extends BlockEntity {
    private final @Nullable String noteBlockSound;
    private final @Nullable String extraType;
    private final @Nullable SkullOwner skullOwner;

    protected SkullBlockEntity(Map<String, Object> data) {
        super(data);

        this.noteBlockSound = (String) data.get("note_block_sound");
        this.extraType = (String) data.get("ExtraType");

        @SuppressWarnings("unchecked")
        Map<String, Object> ownerData = (Map<String, Object>) data.get("SkullOwner");
        this.skullOwner = ownerData != null ? new SkullOwner(ownerData) : null;
    }

    public @Nullable String getNoteBlockSound() {
        return noteBlockSound;
    }

    public @Nullable String getExtraType() {
        return extraType;
    }

    public SkullOwner getSkullOwner() {
        return skullOwner;
    }

    @Override
    public String toString() {
        return "SkullBlockEntity{" +
                "noteBlockSound='" + noteBlockSound + '\'' +
                ", extraType='" + extraType + '\'' +
                ", skullOwner=" + skullOwner +
                "} " + super.toString();
    }

    public static class SkullOwner {
        private final @Nullable UUID id;
        private final @Nullable String name;
        private final List<Texture> textures = new ArrayList<>();

        @SuppressWarnings("unchecked")
        private SkullOwner(Map<String, Object> data) {
            int[] uuidInts = (int[]) data.get("Id");
            this.id = new UUID((long) uuidInts[0] << 32 | uuidInts[1], (long) uuidInts[2] << 32 | uuidInts[3]);
            this.name = (String) data.get("Name");

            Map<String, Object> properties = (Map<String, Object>) data.getOrDefault("Properties", Map.of());
            List<Map<String, Object>> textures = (List<Map<String, Object>>) properties.getOrDefault("textures", List.of());

            for (Map<String, Object> compound : textures) {
                this.textures.add(new Texture(compound));
            }
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Texture> getTextures() {
            return textures;
        }

        @Override
        public String toString() {
            return "SkullOwner{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", textures=" + textures +
                    '}';
        }
    }

    public static class Texture {
        private final @Nullable String signature;
        private final String value;

        private Texture(Map<String, Object> data) {
            this.signature = (String) data.get("signature");
            this.value = (String) data.getOrDefault("value", "");
        }

        public String getSignature() {
            return signature;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Texture{" +
                    "signature='" + signature + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
