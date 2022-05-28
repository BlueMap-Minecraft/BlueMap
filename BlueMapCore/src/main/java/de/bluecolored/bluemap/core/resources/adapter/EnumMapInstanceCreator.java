package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;
import java.util.EnumMap;

public class EnumMapInstanceCreator<K extends Enum<K>, V> implements InstanceCreator<EnumMap<K, V>> {

    private final Class<K> type;

    public EnumMapInstanceCreator(Class<K> type) {
        this.type = type;
    }

    @Override
    public EnumMap<K, V> createInstance(Type type) {
        return new EnumMap<>(this.type);
    }

}
