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
public class CircleMaskConfig extends MaskConfig {

    private double
            centerX = 0,
            centerZ = 0;
    private double radius = Double.MAX_VALUE;
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

        if (radius <= 0) {
            throw new ConfigurationException("""
                    The circle-mask configuration results in a collapsed volume.
                    Make sure that the "radius" value is greater than 0.
                    """.trim());
        }

        return new EllipseMask(
                new Vector2d(centerX, centerZ),
                radius,
                minY, maxY
        );
    }

}
