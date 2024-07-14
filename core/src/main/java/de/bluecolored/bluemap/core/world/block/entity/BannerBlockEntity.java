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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BannerBlockEntity extends BlockEntity {
    private final List<Pattern> patterns = new ArrayList<>();

    protected BannerBlockEntity(Map<String, Object> data) {
        super(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) data.getOrDefault("Patterns", List.of());

        for (Map<String, Object> compound : patterns) {
            this.patterns.add(new Pattern(compound));
        }
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "BannerBlockEntity{" +
                "patterns=" + patterns +
                "} " + super.toString();
    }

    public static class Pattern {
        private final String code;
        private final Color color;

        private Pattern(Map<String, Object> data) {
            this.code = (String) data.get("Pattern");
            this.color = Color.values()[(int) data.get("Color")];
        }

        public String getCode() {
            return code;
        }

        public Color getColor() {
            return color;
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "code='" + code + '\'' +
                    ", color=" + color +
                    '}';
        }
    }

    public enum Color {
        WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY, LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN,
        RED, BLACK
    }
}
