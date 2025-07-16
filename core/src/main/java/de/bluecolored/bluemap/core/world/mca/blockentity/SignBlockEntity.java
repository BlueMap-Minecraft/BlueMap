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

import de.bluecolored.bluenbt.NBTName;
import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class SignBlockEntity extends MCABlockEntity {

    @Nullable TextData frontText;
    @Nullable TextData backText;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class TextData {

        boolean hasGlowingText;
        String color = "black";
        List<Object> messages = List.of();

    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString
    public static class LegacySignBlockEntity extends SignBlockEntity {

        @NBTName("GlowingText") boolean hasGlowingText;
        @NBTName("Color") String color = "black";
        @NBTName("Text1") String text1;
        @NBTName("Text2") String text2;
        @NBTName("Text3") String text3;
        @NBTName("Text4") String text4;

        @Override
        public TextData getFrontText() {
            if (frontText == null)
                frontText = new TextData(hasGlowingText, color, List.of(text1, text2, text3, text4));
            return frontText;
        }

    }

}
