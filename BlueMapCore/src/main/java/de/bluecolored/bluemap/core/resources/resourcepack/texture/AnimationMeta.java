package de.bluecolored.bluemap.core.resources.resourcepack.texture;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@JsonAdapter(AnimationMeta.Adapter.class)
public class AnimationMeta {

    private boolean interpolate = false;
    private int width = 1;
    private int height = 1;
    private int frametime = 1;

    @Nullable private List<FrameMeta> frames = null;

    @Getter
    @AllArgsConstructor
    public static class FrameMeta {
        private int index;
        private int time;
    }

    static class Adapter extends AbstractTypeAdapterFactory<AnimationMeta> {

        public Adapter() {
            super(AnimationMeta.class);
        }

        @Override
        public AnimationMeta read(JsonReader in, Gson gson) throws IOException {
            AnimationMeta animationMeta = new AnimationMeta();

            in.beginObject();
            while (in.hasNext()) {
                if (!in.nextName().equals("animation")){
                    in.skipValue();
                    continue;
                }

                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "interpolate" : animationMeta.interpolate = in.nextBoolean(); break;
                        case "width" : animationMeta.width = in.nextInt(); break;
                        case "height" : animationMeta.height = in.nextInt(); break;
                        case "frametime" : animationMeta.frametime = in.nextInt(); break;
                        case "frames" : readFramesList(in, animationMeta); break;
                        default: in.skipValue(); break;
                    }
                }
                in.endObject();

            }
            in.endObject();

            // default frame-time
            if (animationMeta.frames != null) {
                for (FrameMeta frameMeta : animationMeta.frames) {
                    if (frameMeta.time == -1) frameMeta.time = animationMeta.frametime;
                }
            }

            return animationMeta;
        }

        private void readFramesList(JsonReader in, AnimationMeta animationMeta) throws IOException {
            animationMeta.frames = new ArrayList<>();

            in.beginArray();
            while (in.hasNext()) {
                int index = 0;
                int time = -1;

                if (in.peek() == JsonToken.NUMBER) {
                    index = in.nextInt();
                } else {
                    in.beginObject();
                    while (in.hasNext()) {
                        switch (in.nextName()) {
                            case "index" : index = in.nextInt(); break;
                            case "time" : time = in.nextInt(); break;
                            default: in.skipValue(); break;
                        }
                    }
                    in.endObject();
                }

                animationMeta.frames.add(new FrameMeta(index, time));
            }
            in.endArray();
        }

        @Override
        public void write(JsonWriter out, AnimationMeta value, Gson gson) throws IOException {
            gson.getDelegateAdapter(this, TypeToken.get(AnimationMeta.class)).write(out, value);
        }

    }

}
