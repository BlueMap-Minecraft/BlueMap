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

import de.bluecolored.bluemap.core.util.math.Color;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class BufferedImageUtil {

    public static boolean halfTransparent(BufferedImage image){
        if (image.getTransparency() == Transparency.OPAQUE || image.getTransparency() == Transparency.BITMASK)
            return false;

        Color color = new Color();
        float[] buffer = null;
        boolean useWorkaround = shouldUseWorkaround(image);
        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                buffer = readPixel(image, x, y, color, buffer, useWorkaround);
                if (color.a > 0f && color.a < 1f) return true;
            }
        }
        return false;
    }

    public static Color averageColor(BufferedImage image) {
        Color average = new Color();
        Color color = new Color();
        float[] buffer = null;
        int count = 0;
        boolean useWorkaround = shouldUseWorkaround(image);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                buffer = readPixel(image, x, y, color, buffer, useWorkaround);

                count++;
                average.add(color.premultiplied());
            }
        }
        average.div(count);
        return average;
    }

    public static Color readPixel(BufferedImage image, int x, int y, @Nullable Color target) {
        readPixel(image, x, y, target, null, shouldUseWorkaround(image));
        return target;
    }

    private static float[] readPixel(BufferedImage image, int x, int y, @Nullable Color target, float @Nullable [] buffer, boolean useWorkaround) {
        if (target == null) target = new Color();

        // workaround for java bug: 5051418
        if (useWorkaround) {
            buffer = readPixelDirect(image, x, y, target, buffer);
        } else {
            readPixelDefault(image, x, y, target);
        }

        return buffer;
    }

    private static void readPixelDefault(BufferedImage image, int x, int y, Color target) {
        target.set(image.getRGB(x, y), image.getColorModel().isAlphaPremultiplied());
    }

    /**
     * workaround for java bug: 5051418
     */
    private static boolean shouldUseWorkaround(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_BYTE_GRAY && image.getType() != BufferedImage.TYPE_CUSTOM) return false;
        int[] componentSize = image.getColorModel().getComponentSize();
        for (int i = 0; i < componentSize.length; i++) {
            if (componentSize[i] != 8) return false;
        }
        return true;
    }

    /**
     * workaround for java bug: 5051418
     */
    private static float[] readPixelDirect(RenderedImage image, int x, int y, Color target, float @Nullable [] buffer) {
        buffer = image.getData().getPixel(x, y, buffer);

        float r = buffer[0] / 255f;
        float g = buffer.length >= 3 ? buffer[1] / 255f : r;
        float b = buffer.length >= 3 ? buffer[2] / 255f : r;
        float a = buffer.length >= 4 ? buffer[3] / 255f : buffer.length == 2 ? buffer[1] / 255f : 1f;

        target.set(r, g, b, a, image.getColorModel().isAlphaPremultiplied());

        return buffer;
    }

}
