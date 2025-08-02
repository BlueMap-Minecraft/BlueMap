package de.bluecolored.bluemap.common.config.mask;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.math.Shape;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.Mask;
import de.bluecolored.bluemap.core.map.mask.PolygonMask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class PolygonMaskConfig extends MaskConfig {

    private int
            minY = Integer.MIN_VALUE,
            maxY = Integer.MAX_VALUE;

    private Vector2d[] shape;

    @Override
    public Mask createMask() throws ConfigurationException {
        if (minY > maxY) {
            throw new ConfigurationException("""
                    The polygon-mask configuration results in a degenerate mask.
                    Make sure that the "min-y" value is actually SMALLER than the "max-y" counterpart.
                    """.trim());
        }

        if (shape == null || shape.length < 3) {
            throw new ConfigurationException("""
                    The polygon-mask configuration needs at least 3 points for a valid shape.
                    """.trim());
        }

        return new PolygonMask(new Shape(shape), minY, maxY);
    }

}
