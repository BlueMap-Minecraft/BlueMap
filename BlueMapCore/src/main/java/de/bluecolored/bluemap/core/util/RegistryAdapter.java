package de.bluecolored.bluemap.core.util;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeAdapter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class RegistryAdapter<T extends Keyed> implements TypeAdapter<T> {

    private final Registry<T> registry;
    private final String defaultNamespace;
    private final T fallback;

    @Override
    public T read(NBTReader reader) throws IOException {
        Key key = Key.parse(reader.nextString(), defaultNamespace);
        T value = registry.get(key);
        if (value != null) return value;

        Logger.global.noFloodWarning("unknown-registry-key-" + key.getFormatted(), "Failed to find registry-entry for key: " + key);
        return fallback;
    }

    @Override
    public void write(T value, NBTWriter writer) throws IOException {
        writer.value(value.getKey().getFormatted());
    }

    @Override
    public TagType type() {
        return TagType.STRING;
    }

}
