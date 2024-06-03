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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate;

import de.bluecolored.bluemap.core.util.Preconditions;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@FunctionalInterface
public interface BlockStateCondition {

    BlockStateCondition MATCH_ALL = new All();
    BlockStateCondition MATCH_NONE = new None();

    boolean matches(BlockState state);

    class Property implements BlockStateCondition {

        private final String key;
        private final String value;

        private Property(String key, String value) {
            this.key = key.toLowerCase(Locale.ROOT);
            this.value = value.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean matches(BlockState state) {
            String value = state.getProperties().get(this.key);
            if (value == null) return false;
            return value.equals(this.value);
        }

    }

    class PropertySet implements BlockStateCondition {

        private final String key;
        private final Set<String> possibleValues;

        private PropertySet(String key, String... possibleValues) {
            this.key = key.toLowerCase(Locale.ROOT);
            this.possibleValues = new HashSet<>();
            for (String value : possibleValues) this.possibleValues.add(value.toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean matches(BlockState state) {
            String value = state.getProperties().get(this.key);
            if (value == null) return false;
            return possibleValues.contains(value);
        }

    }

    class And implements BlockStateCondition {

        final BlockStateCondition[] conditions;
        final int distinctProperties;

        private And (BlockStateCondition... conditions) {
            Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

            this.conditions = conditions;

            // Optimization: count distinct properties
            Set<String> distinctPropertiesSet = new HashSet<>();
            for (BlockStateCondition condition : this.conditions) {
                if (condition instanceof Property) {
                    distinctPropertiesSet.add(((Property) condition).key);
                }
            }
            this.distinctProperties = distinctPropertiesSet.size();
        }

        @Override
        public boolean matches(BlockState state) {
            // fast exit
            if (state.getProperties().size() < this.distinctProperties) return false;

            // check all
            for (BlockStateCondition condition : conditions) {
                if (!condition.matches(state)) return false;
            }
            return true;
        }

    }

    class Or implements BlockStateCondition {

        private final BlockStateCondition[] conditions;

        private Or (BlockStateCondition... conditions) {
            Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

            this.conditions = conditions;
        }

        @Override
        public boolean matches(BlockState state) {
            for (BlockStateCondition condition : conditions) {
                if (condition.matches(state)) return true;
            }
            return false;
        }

    }

    class All implements BlockStateCondition {

        @Override
        public boolean matches(BlockState state) {
            return true;
        }

    }

    class None implements BlockStateCondition {

        @Override
        public boolean matches(BlockState state) {
            return false;
        }

    }

    static BlockStateCondition all() {
        return MATCH_ALL;
    }

    static BlockStateCondition none() {
        return MATCH_NONE;
    }

    static BlockStateCondition and(BlockStateCondition... conditions) {
        Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");
        if (conditions.length == 1) return conditions[0];
        return new And(conditions);
    }

    static BlockStateCondition or(BlockStateCondition... conditions) {
        Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");
        if (conditions.length == 1) return conditions[0];
        return new Or(conditions);
    }

    static BlockStateCondition property(String key, String value) {
        return new Property(key, value);
    }

    static BlockStateCondition property(String key, String... possibleValues) {
        Preconditions.checkArgument(possibleValues.length > 0, "Must be at least one value!");
        if (possibleValues.length == 1)return property(key, possibleValues[0]);
        return new PropertySet(key, possibleValues);
    }

}