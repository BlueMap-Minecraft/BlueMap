package de.bluecolored.bluemap.common.config.typeserializer;

import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.mask.MaskConfig;
import de.bluecolored.bluemap.common.config.mask.MaskType;
import de.bluecolored.bluemap.core.map.mask.CombinedMask;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

public class CombinedMaskSerializer implements TypeSerializer<CombinedMask> {

    @Override
    public CombinedMask deserialize(Type type, ConfigurationNode node) throws SerializationException {
        try {
            CombinedMask combinedMask = new CombinedMask();
            for (ConfigurationNode listNode : node.childrenList()) {
                MaskConfig maskConfig = Objects.requireNonNull(listNode.get(MaskConfig.Base.class));
                MaskType maskType = maskConfig.getMaskType();
                maskConfig = Objects.requireNonNull(listNode.get(maskType.getConfigType()));
                maskConfig.addTo(combinedMask);
            }
            return combinedMask;
        } catch (ConfigurationException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable CombinedMask obj, ConfigurationNode node) {
        throw new UnsupportedOperationException("Serialize not supported.");
    }

}
