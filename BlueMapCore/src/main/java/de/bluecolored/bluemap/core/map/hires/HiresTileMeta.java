package de.bluecolored.bluemap.core.map.hires;

import de.bluecolored.bluemap.core.util.math.Color;

public class HiresTileMeta {

    private final int[] heights;
    private final float[] colors;

    private final int minX, minZ, maxX, maxZ, sizeX, sizeZ;

    public HiresTileMeta(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxZ = maxZ;

        this.sizeX = maxX - minX + 1;
        this.sizeZ = maxZ - minZ + 1;

        this.heights = new int[sizeX * sizeZ];
        this.colors = new float[sizeX * sizeZ * 4];
    }

    public void setHeight(int x, int z, int height) {
        heights[(x - minX) * sizeZ + (z - minZ)] = height;
    }

    public int getHeight(int x, int z) {
        return heights[(x - minX) * sizeZ + (z - minZ)];
    }

    public void setColor(int x, int z, Color color) {
        if (!color.premultiplied) throw new IllegalArgumentException("Color should be premultiplied!");
        setColor(x, z, color.r, color.g, color.b, color.a);
    }

    private void setColor(int x, int z, float r, float g, float b, float a) {
        int index = (x - minX) * sizeZ + (z - minZ) * 4;
        colors[index    ] = r;
        colors[index + 1] = g;
        colors[index + 2] = b;
        colors[index + 3] = a;
    }

    public Color getColor(int x, int z, Color target) {
        int index = (x - minX) * sizeZ + (z - minZ) * 4;
        return target.set(
                colors[index    ],
                colors[index + 1],
                colors[index + 2],
                colors[index + 3],
                true
        );
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeZ() {
        return sizeZ;
    }

}
