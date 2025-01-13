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
package de.bluecolored.bluemap.core.world.mca.blockentity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class SkullBlockEntity extends MCABlockEntity {

    @Nullable String customName;
    @Nullable String noteBlockSound;
    @Nullable Profile profile;

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class Profile {

        @Nullable UUID id;
        @Nullable String name;
        List<Property> properties = List.of();

    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class Property {

        String name;
        String value;
        @Nullable String signature;

        private Property(Map<String, Object> data) {
            this.signature = (String) data.get("signature");
            this.value = (String) data.getOrDefault("value", "");
        }

    }
}
