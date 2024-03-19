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

import java.util.List;
import java.util.Map;

public class SignBlockEntity extends BlockEntity {
    private final TextData frontText;
    private final TextData backText;

    @SuppressWarnings("unchecked")
    protected SignBlockEntity(Map<String, Object> data) {
        super(data);

        // Versions before 1.20 used a different format
        if (data.containsKey("front_text")) {
            this.frontText = new TextData((Map<String, Object>) data.getOrDefault("front_text", Map.of()));
            this.backText = new TextData((Map<String, Object>) data.getOrDefault("back_text", Map.of()));
        } else {
            this.frontText = new TextData(
                    (byte) data.getOrDefault("GlowingText", (byte) 0) == 1,
                    (String) data.getOrDefault("Color", ""),
                    List.of(
                            (String) data.getOrDefault("Text1", ""),
                            (String) data.getOrDefault("Text2", ""),
                            (String) data.getOrDefault("Text3", ""),
                            (String) data.getOrDefault("Text4", "")
                    )
            );

            this.backText = new TextData(false, "", List.of());
        }
    }

    public TextData getFrontText() {
        return frontText;
    }

    public TextData getBackText() {
        return backText;
    }

    @Override
    public String toString() {
        return "SignBlockEntity{" +
                "frontText=" + frontText +
                ", backText=" + backText +
                "} " + super.toString();
    }

    public static class TextData {
        private final boolean hasGlowingText;
        private final String color;
        private final List<String> messages;

        @SuppressWarnings("unchecked")
        private TextData(Map<String, Object> data) {
            this.hasGlowingText = (byte) data.getOrDefault("has_glowing_text", (byte) 0) == 1;
            this.color = (String) data.getOrDefault("color", "");
            this.messages = (List<String>) data.getOrDefault("messages", List.of());
        }

        public TextData(boolean hasGlowingText, String color, List<String> messages) {
            this.hasGlowingText = hasGlowingText;
            this.color = color;
            this.messages = messages;
        }

        public boolean isHasGlowingText() {
            return hasGlowingText;
        }

        public String getColor() {
            return color;
        }

        public List<String> getMessages() {
            return messages;
        }

        @Override
        public String toString() {
            return "TextData{" +
                    "hasGlowingText=" + hasGlowingText +
                    ", color='" + color + '\'' +
                    ", messages=" + messages +
                    '}';
        }
    }
}
