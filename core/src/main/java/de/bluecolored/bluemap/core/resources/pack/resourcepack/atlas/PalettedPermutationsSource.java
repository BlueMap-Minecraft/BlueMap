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
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.BufferedImageUtil;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"unused", "FieldMayBeFinal"})
public class PalettedPermutationsSource extends Source {

    private Set<ResourcePath<Texture>> textures;
    private String separator = "_";
    @SerializedName("palette_key")
    private ResourcePath<Texture> paletteKey;
    private Map<String, ResourcePath<Texture>> permutations;

    @Override
    public void load(Path root, ResourcePool<Texture> texturePool, Predicate<Key> textureFilter) throws IOException {
        if (textures == null) return;
        if (paletteKey == null) return;
        if (permutations == null || permutations.isEmpty()) return;

        // textures
        for (ResourcePath<Texture> resource : textures) {
            texturePool.load(resource, rp -> {
                Path file = getFile(root, resource);
                return loadTexture(resource, file);
            });
        }

        // key
        texturePool.load(paletteKey, rp -> {
            Path file = getFile(root, paletteKey);
            return loadTexture(paletteKey, file);
        });

        // permutations
        for (ResourcePath<Texture> resource : permutations.values()) {
            texturePool.load(resource, rp -> {
                Path file = getFile(root, resource);
                return loadTexture(resource, file);
            });
        }
    }

    @Override
    public void bake(ResourcePool<Texture> texturePool, Predicate<Key> textureFilter) throws IOException {
        if (textures == null) return;
        if (paletteKey == null) return;
        if (permutations == null || permutations.isEmpty()) return;

        // get key palette
        Texture paletteKeyTexture = texturePool.get(paletteKey);
        if (paletteKeyTexture == null) return;
        BufferedImage keyPalette = paletteKeyTexture.getTextureImage();

        // get target palettes and create PaletteMap's
        Map<String, PaletteMap> palettes = new HashMap<>(permutations.size());
        for (var permutationEntry : permutations.entrySet()) {
            try {
                Texture texture = texturePool.get(permutationEntry.getValue());
                if (texture == null) continue;
                BufferedImage image = texture.getTextureImage();
                PaletteMap palette = new PaletteMap(keyPalette, image);
                palettes.put(permutationEntry.getKey(), palette);
            } catch (ArrayIndexOutOfBoundsException ex) {
                Logger.global.logDebug("Failed to load paletted_permutation: Permutation palette %s does not match key palette %s.".formatted(
                        permutationEntry.getValue(),
                        paletteKey
                ));
            }
        }

        // generate textures
        Color tempColor = new Color();
        for (ResourcePath<Texture> resource : textures) {
            Texture texture = texturePool.get(resource);
            if (texture == null) continue;
            BufferedImage image = texture.getTextureImage();

            for (var paletteEntry : palettes.entrySet()) {
                String suffix = paletteEntry.getKey();
                PaletteMap palette = paletteEntry.getValue();

                ResourcePath<Texture> sprite = new ResourcePath<>(
                        resource.getNamespace(),
                        resource.getValue() + separator + suffix
                );
                if (texturePool.contains(sprite)) continue;
                if (!textureFilter.test(sprite)) continue;

                BufferedImage resultImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

                // map texture
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        int color = BufferedImageUtil.readPixel(image, x, y, tempColor).getInt();
                        float alpha = (color >> 24 & 0xFF) / 255f;

                        color = palette.applyAsInt(color);
                        alpha *= (color >> 24 & 0xFF) / 255f;

                        resultImage.setRGB(x, y, (((int) (alpha * 255f) & 0xFF) << 24) | (color & 0xFFFFFF));
                    }
                }

                texturePool.put(sprite, Texture.from(sprite, resultImage, texture.getAnimation()));
            }
        }

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PalettedPermutationsSource that = (PalettedPermutationsSource) o;
        return
                Objects.equals(textures, that.textures) &&
                Objects.equals(separator, that.separator) &&
                Objects.equals(paletteKey, that.paletteKey) &&
                Objects.equals(permutations, that.permutations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), textures, separator, paletteKey, permutations);
    }

    private static class PaletteMap implements IntUnaryOperator {

        private final Map<Integer, Integer> map = new HashMap<>();

        public PaletteMap(BufferedImage keys, BufferedImage values) {
            Color tempColor  = new Color();
            for (int x = 0; x < keys.getWidth(); x++) {
                for (int y = 0; y < keys.getHeight(); y++) {
                    int keyColor = BufferedImageUtil.readPixel(keys, x, y, tempColor).getInt();
                    int valueColor = BufferedImageUtil.readPixel(values, x, y, tempColor).getInt();
                    map.put(keyColor | 0xFF000000, valueColor);
                }
            }
        }

        @Override
        public int applyAsInt(int operand) {
            operand |= 0xFF000000;
            Integer result = map.get(operand);
            return result == null ? operand : result;
        }

    }

}
