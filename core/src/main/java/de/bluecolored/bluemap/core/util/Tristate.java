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
package de.bluecolored.bluemap.core.util;

import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@RequiredArgsConstructor
public enum Tristate {

    TRUE (true) {
        @Override
        public Tristate and(Supplier<Tristate> other) {
            return other.get();
        }

        @Override
        public Tristate or(Supplier<Tristate> other) {
            return this;
        }
    },
    UNDEFINED (false) {
        @Override
        public Tristate getOr(Tristate other) {
            return other;
        }

        @Override
        public boolean getOr(BooleanSupplier other) {
            return other.getAsBoolean();
        }

        @Override
        public boolean getOr(boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public Tristate and(Supplier<Tristate> other) {
            return other.get() == FALSE ? FALSE : this;
        }

        @Override
        public Tristate or(Supplier<Tristate> other) {
            return other.get() == TRUE ? TRUE : this;
        }
    },
    FALSE (false) {
        @Override
        public Tristate and(Supplier<Tristate> other) {
            return this;
        }

        @Override
        public Tristate or(Supplier<Tristate> other) {
            return other.get();
        }
    };

    private final boolean value;
    private Tristate negative;

    static {
        TRUE.negative = FALSE;
        UNDEFINED.negative = UNDEFINED;
        FALSE.negative = TRUE;
    }

    public Tristate getOr(Tristate other) {
        return this;
    }

    public boolean getOr(BooleanSupplier other) {
        return value;
    }

    public boolean getOr(boolean defaultValue) {
        return value;
    }

    public Tristate negated() {
        return negative;
    }

    public abstract Tristate and(Supplier<Tristate> other);

    public abstract Tristate or(Supplier<Tristate> other);

    public Tristate and(Tristate other) {
        return this.and(() -> other);
    }

    public Tristate or(Tristate other) {
        return this.or(() -> other);
    }

    @Override
    public String toString() {
        return "Tristate." + name();
    }

    public static Tristate valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

}
