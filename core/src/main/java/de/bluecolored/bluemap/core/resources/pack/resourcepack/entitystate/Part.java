package de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate;

import com.flowpowered.math.vector.Vector3f;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.map.hires.entity.EntityRendererType;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import lombok.Getter;

import java.io.IOException;

@SuppressWarnings("FieldMayBeFinal")
@JsonAdapter(Part.Adapter.class)
@Getter
public class Part {

    private EntityRendererType renderer = EntityRendererType.DEFAULT;
    private ResourcePath<Model> model = ResourcePack.MISSING_ENTITY_MODEL;
    private Vector3f position = Vector3f.ZERO;
    private Vector3f rotation = Vector3f.ZERO;

    private transient boolean transformed;
    private transient MatrixM4f transformMatrix;

    private Part(){}

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
