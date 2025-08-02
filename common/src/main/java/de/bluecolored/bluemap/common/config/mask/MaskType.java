package de.bluecolored.bluemap.common.config.mask;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface MaskType extends Keyed {

    MaskType BOX = new Impl(Key.bluemap("box"), BoxMaskConfig.class);
    MaskType CIRCLE = new Impl(Key.bluemap("circle"), CircleMaskConfig.class);
    MaskType ELLIPSE = new Impl(Key.bluemap("ellipse"), EllipseMaskConfig.class);
    MaskType POLYGON = new Impl(Key.bluemap("polygon"), PolygonMaskConfig.class);
    MaskType BLUR = new Impl(Key.bluemap("blur"), BlurMaskConfig.class);

    Registry<MaskType> REGISTRY = new Registry<>(
            BOX,
            CIRCLE,
            ELLIPSE,
            POLYGON,
            BLUR
    );

    Class<? extends MaskConfig> getConfigType();

    @RequiredArgsConstructor
    @Getter
    class Impl implements MaskType {

        private final Key key;
        private final Class<? extends MaskConfig> configType;

    }

}
