package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import com.google.gson.annotations.SerializedName;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
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
@SuppressWarnings("unused")
public class UnstitchSource extends Source {

    private ResourcePath<Texture> resource;
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
            if (textures.contains(region.sprite)) continue;
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Region {
        private ResourcePath<Texture> sprite;
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
