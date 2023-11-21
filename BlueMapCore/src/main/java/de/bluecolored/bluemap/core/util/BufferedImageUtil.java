package de.bluecolored.bluemap.core.util;

import de.bluecolored.bluemap.core.util.math.Color;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class BufferedImageUtil {

    public static Color averageColor(BufferedImage image) {
        Color average = new Color();
        Color color = new Color();
        float[] buffer = null;
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                buffer = readPixel(image, x, y, color, buffer);

                count++;
                average.add(color.premultiplied());
            }
        }
        average.div(count);
        return average;
    }

    public static Color readPixel(BufferedImage image, int x, int y, @Nullable Color target) {
        readPixel(image, x, y, target, null);
        return target;
    }

    private static float[] readPixel(BufferedImage image, int x, int y, @Nullable Color target, float @Nullable [] buffer) {
        if (target == null) target = new Color();

        // workaround for java bug: 5051418
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            buffer = readPixelDirect(image, x, y, target, buffer);
        } else {
            readPixelDefault(image, x, y, target);
        }

        return buffer;
    }

    private static void readPixelDefault(BufferedImage image, int x, int y, Color target) {
        target.set(image.getRGB(x, y), image.getColorModel().isAlphaPremultiplied());
    }

    private static float[] readPixelDirect(RenderedImage image, int x, int y, Color target, float @Nullable [] buffer) {
        buffer = image.getData().getPixel(x, y, buffer);

        float a = buffer.length >= 4 ? buffer[3] / 255f : 1f;
        float r = buffer[0] / 255f;
        float g = buffer.length >= 3 ? buffer[1] / 255f : r;
        float b = buffer.length >= 3 ? buffer[2] / 255f : r;

        target.set(r, g, b, a, image.getColorModel().isAlphaPremultiplied());

        return buffer;
    }

}
