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

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.util.math.Axis;
import lombok.Getter;

import java.util.Objects;

public enum Direction {

    UP    ( 0, 1, 0, Axis.Y),
    DOWN  ( 0,-1, 0, Axis.Y),
    NORTH ( 0, 0,-1, Axis.Z),
    SOUTH ( 0, 0, 1, Axis.Z),
    WEST  (-1, 0, 0, Axis.X),
    EAST  ( 1, 0, 0, Axis.X);

    static {
        UP.opposite = DOWN;
        DOWN.opposite = UP;
        NORTH.opposite = SOUTH;
        SOUTH.opposite = NORTH;
        WEST.opposite = EAST;
        EAST.opposite = WEST;

        UP.left = UP;
        DOWN.left = DOWN;
        NORTH.left = WEST;
        SOUTH.left = EAST;
        WEST.left = SOUTH;
        EAST.left = NORTH;

        UP.right = UP;
        DOWN.right = DOWN;
        NORTH.right = EAST;
        SOUTH.right = WEST;
        WEST.right = NORTH;
        EAST.right = SOUTH;

        UP.localUp = NORTH;
        DOWN.localUp = SOUTH;
        NORTH.localUp = UP;
        SOUTH.localUp = UP;
        WEST.localUp = UP;
        EAST.localUp = UP;
    }

    private final Vector3i dir;
    @Getter private final Axis axis;
    @Getter private Direction opposite, left, right, localUp;

    Direction(int x, int y, int z, Axis axis) {
        this.dir = new Vector3i(x, y, z);
        this.axis = axis;
        this.opposite = null;
    }

    public Vector3i toVector(){
        return dir;
    }

    public static Direction fromString(String name){
        Objects.requireNonNull(name);

        return valueOf(name.toUpperCase());
    }

}
