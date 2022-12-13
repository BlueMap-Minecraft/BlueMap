package de.bluecolored.bluemap.common.plugin.skins;

import de.bluecolored.bluemap.api.plugin.PlayerIconFactory;
import de.bluecolored.bluemap.core.logger.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.UUID;

public class DefaultPlayerIconFactory implements PlayerIconFactory {

    @Override
    public BufferedImage apply(UUID uuid, BufferedImage in) {
        BufferedImage head;

        BufferedImage layer1 = in.getSubimage(8, 8, 8, 8);
        BufferedImage layer2 = in.getSubimage(40, 8, 8, 8);

        try {
            head = new BufferedImage(48,  48, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = head.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(layer1, 4, 4, 40, 40, null);
            g.drawImage(layer2, 0, 0, 48, 48, null);
        } catch (Throwable t) { // There might be problems with headless servers when loading the graphics class, so we catch every exception and error on purpose here
            Logger.global.noFloodWarning("headless-graphics-fail",
                    "Could not access Graphics2D to render player-skin texture. Try adding '-Djava.awt.headless=true' to your startup flags or ignore this warning.");
            head = new BufferedImage(8, 8, in.getType());
            layer1.copyData(head.getRaster());
        }

        return head;
    }

}
