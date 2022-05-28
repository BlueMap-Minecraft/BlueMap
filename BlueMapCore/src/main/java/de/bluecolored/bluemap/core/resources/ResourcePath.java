package de.bluecolored.bluemap.core.resources;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

@DebugDump
@JsonAdapter(ResourcePath.Adapter.class)
public class ResourcePath<T> extends Key {

    private T resource = null;

    public ResourcePath(String formatted) {
        super(formatted.toLowerCase(Locale.ROOT));
    }

    public ResourcePath(String namespace, String value) {
        super(namespace.toLowerCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
    }

    public ResourcePath(Path filePath) {
        super(parsePath(filePath).toLowerCase(Locale.ROOT));
    }

    @Nullable
    public T getResource() {
        return resource;
    }

    @Nullable
    public T getResource(Function<ResourcePath<T>, T> supplier) {
        if (resource == null) resource = supplier.apply(this);
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }

    private static String parsePath(Path filePath) {
        if (filePath.getNameCount() < 4)
            throw new IllegalArgumentException("The provided filePath has less than 4 segments!");

        if (!filePath.getName(0).toString().equalsIgnoreCase("assets"))
            throw new IllegalArgumentException("The provided filePath doesn't start with 'assets'!");

        String namespace = filePath.getName(1).toString();
        String path = filePath.subpath(3, filePath.getNameCount()).toString().replace(filePath.getFileSystem().getSeparator(), "/");

        // remove file-ending
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1) path = path.substring(0, dotIndex);

        return namespace + ":" + path;
    }

    static class Adapter implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!type.getRawType().isAssignableFrom(ResourcePath.class))
                return null;

            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    out.value(((ResourcePath<?>) value).getFormatted());
                }

                @SuppressWarnings("unchecked")
                @Override
                public T read(JsonReader in) throws IOException {
                    return (T) new ResourcePath<>(in.nextString());
                }
            };
        }

    }
}
