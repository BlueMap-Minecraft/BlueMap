package de.bluecolored.bluemap.common.config.typeserializer;

import com.flowpowered.math.vector.Vector2i;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class Vector2iTypeSerializer implements TypeSerializer<Vector2i> {

    @Override
    public Vector2i deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var xNode = node.node("x");
        var yNode = node.node("y");

        if (xNode.virtual() || yNode.virtual()) throw new SerializationException("Cannot parse Vector2i: value x or y missing");

        return Vector2i.from(
                xNode.getInt(),
                yNode.getInt()
        );
    }

    @Override
    public void serialize(Type type, @Nullable Vector2i obj, ConfigurationNode node) throws SerializationException {
        if (obj != null) {
            node.node("x").set(obj.getX());
            node.node("y").set(obj.getY());
        }
    }

}
