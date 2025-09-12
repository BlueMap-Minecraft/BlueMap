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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.texture;

import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.util.BufferedImageUtil;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.math.Color;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Base64;

public class Texture implements Keyed {

    private static final String TEXTURE_STRING_PREFIX = "data:image/png;base64,";
    public static final Texture MISSING = new Texture(
            new ResourcePath<>("bluemap", "missing"),
            new Color().set(0.5f, 0f, 0.5f, 1.0f, false),
            false,
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAPklEQVR4Xu3MsQkAMAwDQe2/tFPnBB4gpLhG8MpkZpNkZ6AKZKAKZKAKZKAKZKAKZKAKZKAKWg0XD/UPnjg4MbX+EDdeTUwAAAAASUVORK5CYII\u003d",
            null,
            null
    );

    private ResourcePath<Texture> resourcePath;
    private Color color;
    private boolean halfTransparent;
    private String texture;
    @Nullable private AnimationMeta animation;

    private transient @Nullable Color colorPremultiplied;
    private transient SoftReference<BufferedImage> textureImage = new SoftReference<>(null);

    @SuppressWarnings("unused")
    private Texture() {}

    private Texture(
            ResourcePath<Texture> resourcePath, Color color, boolean halfTransparent,
            String texture, @Nullable AnimationMeta animation, @Nullable BufferedImage textureImage
    ) {
        this.resourcePath = resourcePath;
        this.color = color.straight();
        this.halfTransparent = halfTransparent;
        this.texture = texture;
        this.animation = animation;
        this.textureImage = new SoftReference<>(textureImage);
    }

    private Texture(ResourcePath<Texture> resourcePath) {
        this.resourcePath = resourcePath;
        this.color = MISSING.color;
        this.halfTransparent = MISSING.halfTransparent;
        this.texture = MISSING.texture;
        this.animation = null;
    }

    public ResourcePath<Texture> getKey() {
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

    public BufferedImage getTextureImage() throws IOException {
        BufferedImage image = textureImage.get();
        if (image != null) return image;

        if (!texture.startsWith(TEXTURE_STRING_PREFIX))
            throw new IOException("Texture-string is not in the expected format.");
        byte[] imageData = Base64.getDecoder().decode(texture.substring(TEXTURE_STRING_PREFIX.length()));
        image = ImageIO.read(new ByteArrayInputStream(imageData));

        textureImage = new SoftReference<>(image);
        return image;
    }

    public @Nullable AnimationMeta getAnimation() {
        return animation;
    }

    public static Texture from(ResourcePath<Texture> resourcePath, BufferedImage image) throws IOException {
        return from(resourcePath, image, null);
    }

    public static Texture from(ResourcePath<Texture> resourcePath, BufferedImage image, @Nullable AnimationMeta animation) throws IOException {

        //check halfTransparency
        boolean halfTransparent = BufferedImageUtil.halfTransparent(image);

        //calculate color
        Color color = BufferedImageUtil.averageColor(image);

        //write to Base64
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        String base64 = TEXTURE_STRING_PREFIX + Base64.getEncoder().encodeToString(os.toByteArray());

        return new Texture(resourcePath, color, halfTransparent, base64, animation, image);
    }

    public static Texture missing(ResourcePath<Texture> resourcePath) {
        return new Texture(resourcePath);
    }

}
