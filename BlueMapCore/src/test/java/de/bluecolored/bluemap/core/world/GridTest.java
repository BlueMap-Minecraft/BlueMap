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
package de.bluecolored.bluemap.core.world;

import com.flowpowered.math.vector.Vector2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GridTest {

    @Test
    public void testGetCell() {
        Grid grid = new Grid(16, 0);
        assertEquals(new Vector2i(0, 0), grid.getCell(new Vector2i(0, 0)));
        assertEquals(new Vector2i(0, 0), grid.getCell(new Vector2i(15, 2)));
        assertEquals(new Vector2i(1, 1), grid.getCell(new Vector2i(16, 20)));
        assertEquals(new Vector2i(-1, -1), grid.getCell(new Vector2i(-1, -16)));

        Grid grid2 = new Grid(16,2);
        assertEquals(new Vector2i(-1, -1), grid2.getCell(new Vector2i(0, 0)));
        assertEquals(new Vector2i(0, 0), grid2.getCell(new Vector2i(17, 2)));
    }

    @Test
    public void testCellMin() {
        Grid grid = new Grid(16, 0);
        assertEquals(new Vector2i(0, 0), grid.getCellMin(new Vector2i(0, 0)));
        assertEquals(new Vector2i(16, 32), grid.getCellMin(new Vector2i(1, 2)));
        assertEquals(new Vector2i(-32, -16), grid.getCellMin(new Vector2i(-2, -1)));

        Grid grid2 = new Grid(16, 2);
        assertEquals(new Vector2i(2, 2), grid2.getCellMin(new Vector2i(0, 0)));
        assertEquals(new Vector2i(18, 34), grid2.getCellMin(new Vector2i(1, 2)));
        assertEquals(new Vector2i(-30, -14), grid2.getCellMin(new Vector2i(-2, -1)));
    }

    @Test
    public void testCellMax() {
        Grid grid = new Grid(16, 0);
        assertEquals(new Vector2i(15, 15), grid.getCellMax(new Vector2i(0, 0)));
        assertEquals(new Vector2i(31, 47), grid.getCellMax(new Vector2i(1, 2)));
        assertEquals(new Vector2i(-17, -1), grid.getCellMax(new Vector2i(-2, -1)));

        Grid grid2 = new Grid(16, 2);
        assertEquals(new Vector2i(17, 17), grid2.getCellMax(new Vector2i(0, 0)));
        assertEquals(new Vector2i(33, 49), grid2.getCellMax(new Vector2i(1, 2)));
        assertEquals(new Vector2i(-15, 1), grid2.getCellMax(new Vector2i(-2, -1)));
    }

    @Test
    public void testCellMinWithSmallerTargetGrid() {
        Grid grid = new Grid(16, 0);
        Grid target = new Grid(2, 1);

        assertEquals(new Vector2i(-1, -1), grid.getCellMin(new Vector2i(0, 0), target));
        assertEquals(new Vector2i(-9, 7), grid.getCellMin(new Vector2i(-1, 1), target));
    }

    @Test
    public void testCellMinWithBiggerTargetGrid() {
        Grid grid = new Grid(2, 0);
        Grid target = new Grid(8, 2);

        assertEquals(new Vector2i(-1, -1), grid.getCellMin(new Vector2i(0, 0), target));
        assertEquals(new Vector2i(-1, 1), grid.getCellMin(new Vector2i(-1, 8), target));
        assertEquals(new Vector2i(-1, 2), grid.getCellMin(new Vector2i(-1, 9), target));
    }

    @Test
    public void testCellMaxWithSmallerTargetGrid() {
        Grid grid = new Grid(16, 0);
        Grid target = new Grid(2, 1);

        assertEquals(new Vector2i(7, 7), grid.getCellMax(new Vector2i(0, 0), target));
        assertEquals(new Vector2i(-1, 15), grid.getCellMax(new Vector2i(-1, 1), target));
    }

    @Test
    public void testCellMaxWithBiggerTargetGrid() {
        Grid grid = new Grid(2, 0);
        Grid target = new Grid(8, 2);

        assertEquals(new Vector2i(-1, -1), grid.getCellMax(new Vector2i(0, 0), target));
        assertEquals(new Vector2i(-1, 1), grid.getCellMax(new Vector2i(-1, 8), target));
        assertEquals(new Vector2i(-1, 2), grid.getCellMax(new Vector2i(-1, 9), target));
    }

    @Test
    public void testMultiply() {
        Grid grid1 = new Grid(2, 5);
        Grid grid2 = new Grid(4, 2);

        Grid result1 = new Grid(8, 22);
        Grid result2 = new Grid(8, 9);

        assertEquals(result1, grid1.multiply(grid2));
        assertEquals(result2, grid2.multiply(grid1));
    }

    @Test
    public void testDivide() {
        Grid grid1 = new Grid(8, 22);
        Grid grid2 = new Grid(4, 2);
        Grid result1 = new Grid(2, 5);
        assertEquals(result1, grid1.divide(grid2));

        Grid grid3 = new Grid(8, 9);
        Grid grid4 = new Grid(2, 5);
        Grid result2 = new Grid(4, 2);
        assertEquals(result2, grid3.divide(grid4));
    }

}