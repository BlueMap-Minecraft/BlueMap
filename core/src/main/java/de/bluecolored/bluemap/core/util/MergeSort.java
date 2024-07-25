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

public class MergeSort {

    /*
     * Adapted from: https://github.com/vigna/fastutil
     */
    public static void mergeSortInt(final int[] a, final int from, final int to, IntComparator comp, int[] supp) {
        int len = to - from;

        if (len < 16) {
            insertionSortInt(a, from, to, comp);
            return;
        }
        if (supp == null) supp = java.util.Arrays.copyOf(a, to);

        final int mid = (from + to) >>> 1;
        mergeSortInt(supp, from, mid, comp, a);
        mergeSortInt(supp, mid, to, comp, a);

        if (comp.compare(supp[mid - 1], supp[mid]) <= 0) {
            System.arraycopy(supp, from, a, from, len);
            return;
        }

        for (int i = from, p = from, q = mid; i < to; i++) {
            if (q >= to || p < mid && comp.compare(supp[p], supp[q]) <= 0) a[i] = supp[p++];
            else a[i] = supp[q++];
        }
    }

    /*
     * Adapted from: https://github.com/vigna/fastutil
     */
    private static void insertionSortInt(final int[] a, final int from, final int to, final IntComparator comp) {
        for (int i = from; ++i < to; ) {
            int t = a[i], j = i;
            for (int u = a[j - 1]; comp.compare(t, u) < 0; u = a[--j - 1]) {
                a[j] = u;
                if (from == j - 1) {
                    --j;
                    break;
                }
            }
            a[j] = t;
        }
    }

}
