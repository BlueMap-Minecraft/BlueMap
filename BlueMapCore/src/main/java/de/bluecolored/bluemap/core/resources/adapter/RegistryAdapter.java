package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class RegistryAdapter<T extends Keyed> extends TypeAdapter<T> {

    private final Registry<T> registry;
    private final String defaultNamespace;
    private final T fallback;

    @Override
    public T read(JsonReader in) throws IOException {
        Key key = Key.parse(in.nextString(), defaultNamespace);
        T value = registry.get(key);
        if (value != null) return value;

        Logger.global.noFloodWarning("unknown-registry-key-" + key.getFormatted(), "Failed to find registry-entry for key: " + key);
        return fallback;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(value.getKey().getFormatted());
    }

}