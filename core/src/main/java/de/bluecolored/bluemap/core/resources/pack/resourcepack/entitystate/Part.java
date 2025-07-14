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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate;

import com.flowpowered.math.vector.Vector3f;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.map.hires.entity.EntityRendererType;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import lombok.*;

import java.io.IOException;

@SuppressWarnings("FieldMayBeFinal")
@JsonAdapter(Part.Adapter.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Part {

    @Setter
    private EntityRendererType renderer = EntityRendererType.DEFAULT;

    private ResourcePath<Model> model = ResourcePack.MISSING_ENTITY_MODEL;
    private Vector3f position = Vector3f.ZERO;
    private Vector3f rotation = Vector3f.ZERO;

    private transient boolean transformed;
    private transient MatrixM4f transformMatrix;

    public Part(ResourcePath<Model> model) {
        this.model = model;
        init();
    }

    public Part(ResourcePath<Model> model, Vector3f position, Vector3f rotation) {
        this.model = model;
        this.position = position;
        this.rotation = rotation;
        init();
    }

    private void init() {
        this.transformed = !position.equals(Vector3f.ZERO) || !rotation.equals(Vector3f.ZERO);
        this.transformMatrix = new MatrixM4f()
                .rotate(rotation.getX(), rotation.getY(), rotation.getZ())
                .translate(position.getX(), position.getY(), position.getZ());
    }

    static class Adapter extends AbstractTypeAdapterFactory<Part> {

        public Adapter() {
            super(Part.class);
        }

        @Override
        public Part read(JsonReader in, Gson gson) throws IOException {
            Part part = gson.getDelegateAdapter(this, TypeToken.get(Part.class)).read(in);
            part.init();
            return part;
        }

    }

}
