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
package de.bluecolored.bluemap.core.resourcepack.blockstate;


import de.bluecolored.bluemap.core.util.Preconditions;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.Map;

@FunctionalInterface
public interface PropertyCondition {

    PropertyCondition MATCH_ALL = new All();
    PropertyCondition MATCH_NONE = new None();

    boolean matches(BlockState state);

    class Property implements PropertyCondition {

        private final String key;
        private final String value;

        private Property (String key, String value) {
            this.key = key.toLowerCase();
            this.value = value.toLowerCase();
        }

        @Override
        public boolean matches(BlockState state) {
            String value = state.getProperties().get(this.key);
            if (value == null) return false;
            return value.equals(this.value);
        }

    }

    class And implements PropertyCondition {

        private final PropertyCondition[] conditions;

        private And (PropertyCondition... conditions) {
            Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

            this.conditions = conditions;
        }

        @Override
        public boolean matches(BlockState state) {
            for (PropertyCondition condition : conditions) {
                if (!condition.matches(state)) return false;
            }
            return true;
        }

    }

    class Or implements PropertyCondition {

        private final PropertyCondition[] conditions;

        private Or (PropertyCondition... conditions) {
            Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

            this.conditions = conditions;
        }

        @Override
        public boolean matches(BlockState state) {
            for (PropertyCondition condition : conditions) {
                if (condition.matches(state)) return true;
            }
            return false;
        }

    }

    class All implements PropertyCondition {

        @Override
        public boolean matches(BlockState state) {
            return true;
        }

    }

    class None implements PropertyCondition {

        @Override
        public boolean matches(BlockState state) {
            return false;
        }

    }

    static PropertyCondition all() {
        return MATCH_ALL;
    }

    static PropertyCondition none() {
        return MATCH_NONE;
    }

    static PropertyCondition and(PropertyCondition... conditions) {
        Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

        return new And(conditions);
    }

    static PropertyCondition or(PropertyCondition... conditions) {
        Preconditions.checkArgument(conditions.length > 0, "Must be at least one condition!");

        return new Or(conditions);
    }

    static PropertyCondition property(String key, String value) {
        return new Property(key, value);
    }

    static PropertyCondition property(String key, String... possibleValues) {
        Preconditions.checkArgument(possibleValues.length > 0, "Must be at least one value!");

        if (possibleValues.length == 1) {
            return property(key, possibleValues[0]);
        }

        PropertyCondition[] conditions = new PropertyCondition[possibleValues.length];
        for (int i = 0; i < possibleValues.length; i++) {
            conditions[i] = property(key, possibleValues[i]);
        }

        return or(conditions);
    }

    static PropertyCondition blockState(BlockState state) {
        Map<String, String> props = state.getProperties();
        if (props.isEmpty()) return all();

        PropertyCondition[] conditions = new Property[props.size()];
        int i = 0;
        for (Map.Entry<String, String> prop : props.entrySet()) {
            conditions[i++] = property(prop.getKey(), prop.getValue());
        }

        return and(conditions);
    }

}
