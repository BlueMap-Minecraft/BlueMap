/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.resources.pack.resourcepack.texture;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
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
                        case "frametime" : animationMeta.frametime = (int) in.nextDouble(); break;
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
            List<FrameMeta> frames = new ArrayList<>();

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
                            case "time" : time = (int) in.nextDouble(); break;
                            default: in.skipValue(); break;
                        }
                    }
                    in.endObject();
                }

                frames.add(new FrameMeta(index, time));
            }
            in.endArray();

            if (!frames.isEmpty())
                animationMeta.frames = frames;
        }

        @Override
        public void write(JsonWriter out, AnimationMeta value, Gson gson) throws IOException {
            gson.getDelegateAdapter(this, TypeToken.get(AnimationMeta.class)).write(out, value);
        }

    }

}
