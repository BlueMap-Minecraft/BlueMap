package de.bluecolored.bluemap.common.config.mask;

import de.bluecolored.bluemap.core.map.mask.BlurMask;
import de.bluecolored.bluemap.core.map.mask.CombinedMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class BlurMaskConfig extends MaskConfig {

    private int size = 5;
    private CombinedMask masks = new CombinedMask();

    @Override
    public Mask createMask() {
        return size > 0 ? new BlurMask(masks, size) : masks;
    }

}
