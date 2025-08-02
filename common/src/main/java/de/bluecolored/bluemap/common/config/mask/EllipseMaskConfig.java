package de.bluecolored.bluemap.common.config.mask;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.EllipseMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class EllipseMaskConfig extends MaskConfig {

    private double
            centerX = 0,
            centerZ = 0;
    private double radiusX = Double.MAX_VALUE;
    private double radiusZ = Double.MAX_VALUE;
    private int
            minY = Integer.MIN_VALUE,
            maxY = Integer.MAX_VALUE;

    @Override
    public Mask createMask() throws ConfigurationException {
        if (minY > maxY) {
            throw new ConfigurationException("""
                    The circle-mask configuration results in a collapsed volume.
                    Make sure that the "min-y" value is actually SMALLER than the "max-y" counterpart.
                    """.trim());
        }

        if (radiusX <= 0 || radiusZ <= 0) {
            throw new ConfigurationException("""
                    The ellipse-mask configuration results in a collapsed volume.
                    Make sure that the radius values are greater than 0.
                    """.trim());
        }

        return new EllipseMask(
                new Vector2d(centerX, centerZ),
                radiusX, radiusZ,
                minY, maxY
        );
    }

}
