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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import com.google.gson.annotations.SerializedName;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unused")
public class UnstitchSource extends Source {

    private Key resource;
    @SerializedName("divisor_x") private double divisorX;
    @SerializedName("divisor_y") private double divisorY;
    private Set<Region> regions;

    @Override
    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        if (resource == null) return;
        if (regions == null || regions.isEmpty()) return;

        textures.load(resource, resource -> {
            Path file = getFile(root, resource);
            return loadTexture(resource, file);
        });
    }

    @Override
    public void bake(ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        if (resource == null) return;
        if (regions == null || regions.isEmpty()) return;

        Texture texture = textures.get(resource);
        if (texture == null) return;

        BufferedImage image = texture.getTextureImage();
        if (divisorX <= 0) divisorX = image.getWidth();
        if (divisorY <= 0) divisorY = image.getHeight();
        double fX = image.getWidth() / divisorX;
        double fY = image.getHeight() / divisorY;

        for (Region region : regions) {
            if (region == null) continue;
            if (textures.containsKey(region.sprite)) continue;
            if (!textureFilter.test(region.sprite)) continue;

            try {
                BufferedImage regionImage = image.getSubimage(
                        (int) (region.x * fX), (int) (region.y * fY),
                        (int) (region.width * fX), (int) (region.height * fY)
                );
                textures.put(region.sprite, Texture.from(region.sprite, regionImage, texture.getAnimation()));
            } catch (RasterFormatException e) {
                Logger.global.logDebug("Failed to unstitch %s into %s because defined region is out of image-bounds: %s".formatted(resource, region.sprite, e));
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        UnstitchSource that = (UnstitchSource) object;
        return
                Double.compare(divisorX, that.divisorX) == 0 &&
                Double.compare(divisorY, that.divisorY) == 0 &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(regions, that.regions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(resource);
        result = 31 * result + Double.hashCode(divisorX);
        result = 31 * result + Double.hashCode(divisorY);
        result = 31 * result + Objects.hashCode(regions);
        return result;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class Region {
        private Key sprite;
        private double x;
        private double y;
        private double width;
        private double height;

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            Region region = (Region) object;
            return
                    Double.compare(x, region.x) == 0 &&
                    Double.compare(y, region.y) == 0 &&
                    Double.compare(width, region.width) == 0 &&
                    Double.compare(height, region.height) == 0 &&
                    Objects.equals(sprite, region.sprite);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(sprite);
            result = 31 * result + Double.hashCode(x);
            result = 31 * result + Double.hashCode(y);
            result = 31 * result + Double.hashCode(width);
            result = 31 * result + Double.hashCode(height);
            return result;
        }
    }

}
