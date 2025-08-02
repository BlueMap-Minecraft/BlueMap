package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PostDeserializeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        for (Method method : type.getRawType().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostDeserialize.class) && method.getParameterCount() == 0) {
                TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
                return new TypeAdapter<>() {

                    @Override
                    public void write(JsonWriter out, T value) throws IOException {
                        delegate.write(out, value);
                    }

                    @Override
                    public T read(JsonReader in) throws IOException {
                        try {
                            T obj = delegate.read(in);
                            method.setAccessible(true);
                            method.invoke(obj);
                            return obj;
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new IOException(e);
                        }
                    }

                };
            }
        }
        return null;
    }

}
