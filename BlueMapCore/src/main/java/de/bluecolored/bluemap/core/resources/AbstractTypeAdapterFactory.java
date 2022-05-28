package de.bluecolored.bluemap.core.resources;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public abstract class AbstractTypeAdapterFactory<T> implements TypeAdapterFactory {
    protected static final String JSON_COMMENT = "__comment";

    private final Class<T> type;

    public AbstractTypeAdapterFactory(Class<T> type) {
        this.type = type;
    }

    public void write(JsonWriter out, T value, Gson gson) throws IOException {
        throw new UnsupportedOperationException();
    }

    public abstract T read(JsonReader in, Gson gson) throws IOException;

    @SuppressWarnings("unchecked")
    public <U> TypeAdapter<U> create(Gson gson, TypeToken<U> type) {
        if (!type.getRawType().isAssignableFrom(this.type)) return null;
        return (TypeAdapter<U>) new Adapter(gson);
    }

    public class Adapter extends TypeAdapter<T> {

        private final Gson gson;

        public Adapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            AbstractTypeAdapterFactory.this.write(out, value, gson);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            return AbstractTypeAdapterFactory.this.read(in, gson);
        }

    }

}
