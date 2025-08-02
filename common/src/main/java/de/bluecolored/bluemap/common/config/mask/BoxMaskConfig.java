package de.bluecolored.bluemap.common.config.mask;

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.BoxMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class BoxMaskConfig extends MaskConfig {

    private int
            minX = Integer.MIN_VALUE,
            minY = Integer.MIN_VALUE,
            minZ = Integer.MIN_VALUE,
            maxX = Integer.MAX_VALUE,
            maxY = Integer.MAX_VALUE,
            maxZ = Integer.MAX_VALUE;

    @Override
    public Mask createMask() throws ConfigurationException {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new ConfigurationException("""
                    The box-mask configuration results in a degenerate mask.
                    Make sure that all "min-" values are actually SMALLER than their "max-" counterparts.
                    """.trim());
        }

        return new BoxMask(
                new Vector3i(minX, minY, minZ),
                new Vector3i(maxX, maxY, maxZ)
        );
    }

}
