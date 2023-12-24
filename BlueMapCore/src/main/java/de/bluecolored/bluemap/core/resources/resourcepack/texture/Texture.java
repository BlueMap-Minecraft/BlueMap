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
package de.bluecolored.bluemap.core.resources.resourcepack.texture;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.util.BufferedImageUtil;
import de.bluecolored.bluemap.core.util.math.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@DebugDump
public class Texture {

    public static final Texture MISSING = new Texture(
            new ResourcePath<>("bluemap", "missing"),
            new Color().set(0.5f, 0f, 0.5f, 1.0f, false),
            false,
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPklEQVR4Xu3MsQkAMAwDQe2/tFPnBB4gpLhG8MpkZpNkZ6AKZKAKZKAKZKAKZKAKZKAKZKAKWg0XD/UPnjg4MbX+EDdeTUwAAAAASUVORK5CYII\u003d"
    );

    private ResourcePath<Texture> resourcePath;
    private Color color;
    private boolean halfTransparent;
    private String texture;

    private transient Color colorPremultiplied;

    @SuppressWarnings("unused")
    private Texture() {}

    private Texture(ResourcePath<Texture> resourcePath, Color color, boolean halfTransparent, String texture) {
        this.resourcePath = resourcePath;
        this.color = color.straight();
        this.halfTransparent = halfTransparent;
        this.texture = texture;
    }

    private Texture(ResourcePath<Texture> resourcePath) {
        this.resourcePath = resourcePath;
        this.color = MISSING.color;
        this.halfTransparent = MISSING.halfTransparent;
        this.texture = MISSING.texture;
    }

    public ResourcePath<Texture> getResourcePath() {
        return resourcePath;
    }

    public Color getColorStraight() {
        return color;
    }

    public boolean isHalfTransparent() {
        return halfTransparent;
    }

    public Color getColorPremultiplied() {
        if (colorPremultiplied == null && color != null) {
            colorPremultiplied = new Color()
                    .set(color)
                    .premultiplied();
        }

        return colorPremultiplied;
    }

    public String getTexture() {
        return texture;
    }

    public void unloadImageData() {
        texture = null;
    }

    public static Texture from(ResourcePath<Texture> resourcePath, BufferedImage image) throws IOException {
        return from(resourcePath, image, true);
    }

    public static Texture from(ResourcePath<Texture> resourcePath, BufferedImage image, boolean animated) throws IOException {
        //crop off animation frames
        if (animated && image.getHeight() > image.getWidth()){
            image = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
        }

        //check halfTransparency
        boolean halfTransparent = checkHalfTransparent(image);

        //calculate color
        Color color = BufferedImageUtil.averageColor(image);

        //write to Base64
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());

        return new Texture(resourcePath, color, halfTransparent, base64);
    }

    private static boolean checkHalfTransparent(BufferedImage image){
        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 0x00 && alpha < 0xff){
                    return true;
                }
            }
        }

        return false;
    }

    public static Texture missing(ResourcePath<Texture> resourcePath) {
        return new Texture(resourcePath);
    }

}
