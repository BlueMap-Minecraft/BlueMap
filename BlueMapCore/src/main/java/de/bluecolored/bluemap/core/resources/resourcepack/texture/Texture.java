package de.bluecolored.bluemap.core.resources.resourcepack.texture;

import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
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
        //crop off animation frames
        if (image.getHeight() > image.getWidth()){
            image = image.getSubimage(0, 0, image.getWidth(), image.getWidth());
        }

        //check halfTransparency
        boolean halfTransparent = checkHalfTransparent(image);

        //calculate color
        Color color = calculateColor(image);

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

    private static Color calculateColor(BufferedImage image){
        float alpha = 0f, red = 0f, green = 0f, blue = 0f;
        int count = 0;

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int pixel = image.getRGB(x, y);
                float pixelAlpha = ((pixel >> 24) & 0xff) / 255f;
                float pixelRed = ((pixel >> 16) & 0xff) / 255f;
                float pixelGreen = ((pixel >> 8) & 0xff) / 255f;
                float pixelBlue = (pixel & 0xff) / 255f;

                count++;
                alpha += pixelAlpha;
                red += pixelRed * pixelAlpha;
                green += pixelGreen * pixelAlpha;
                blue += pixelBlue * pixelAlpha;
            }
        }

        if (count == 0 || alpha == 0) return new Color();

        red /= alpha;
        green /= alpha;
        blue /= alpha;
        alpha /= count;

        return new Color().set(red, green, blue, alpha, false);
    }

}
