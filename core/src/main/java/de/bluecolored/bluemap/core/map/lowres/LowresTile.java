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
package de.bluecolored.bluemap.core.map.lowres;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.util.math.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LowresTile {

    public static final int HEIGHT_UNDEFINED = Integer.MIN_VALUE;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final BufferedImage texture;
    private final Vector2i size;

    public LowresTile(Vector2i tileSize) {
        this.size = tileSize.add(1, 1); // add 1 for seamless edges
        this.texture = new BufferedImage(this.size.getX(), this.size.getY() * 2, BufferedImage.TYPE_INT_ARGB);
    }

    public LowresTile(Vector2i tileSize, InputStream in) throws IOException {
        this.size = tileSize.add(1, 1); // add 1 for seamless edges
        this.texture = ImageIO.read(in);

        if (this.texture == null) {
            throw new IOException("No registered ImageReader is able to read the image-stream");
        }

        if (this.texture.getWidth() != this.size.getX() || this.texture.getHeight() != this.size.getY() * 2) {
            throw new IOException("Size of tile does not match");
        }
    }

    public void set(int x, int z, Color color, int height, int blockLight) {
        lock.readLock().lock();
        try {
            texture.setRGB(x, z, color.straight().getInt());
            texture.setRGB(x, size.getY() + z,
                    (height & 0x0000FFFF) |
                            ((blockLight << 16) & 0x00FF0000) |
                            0xFF000000
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    public Color getColor(int x, int z, Color target) {
        return target.set(texture.getRGB(x, z));
    }

    public int getHeight(int x, int z) {
        int height = texture.getRGB(x, size.getY() + z) & 0x0000FFFF;
        if (height > 0x00008000)
            return height | 0xFFFF0000;
        return height;
    }

    public int getBlockLight(int x, int z) {
        return (texture.getRGB(x, size.getY() + z) & 0x00FF0000) >> 16;
    }

    public void save(OutputStream out) throws IOException {
        lock.writeLock().lock();
        try {
            ImageIO.write(texture, "png", out);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
