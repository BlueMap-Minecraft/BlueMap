package de.bluecolored.bluemap.core.util;

import com.flowpowered.math.vector.Vector2i;

public class Vector2iCache {

    private static final int CACHE_SIZE = 0x4000;
    private static final int CACHE_MASK = CACHE_SIZE - 1;
    private final Vector2i[] cache;

    public Vector2iCache() {
        this.cache = new Vector2i[CACHE_SIZE];
    }

    public Vector2i get(int x, int y) {
        int cacheIndex = (x * 1456 ^ y * 948375892) & CACHE_MASK;
        Vector2i possibleMatch = cache[cacheIndex];

        if (possibleMatch != null && possibleMatch.getX() == x && possibleMatch.getY() == y) {
            return possibleMatch;
        }
        return cache[cacheIndex] = new Vector2i(x, y);
    }

}
