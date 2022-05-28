package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.util.Direction;

import java.io.IOException;
import java.util.EnumMap;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@JsonAdapter(Element.Adapter.class)
public class Element {
    private static final Vector3f FULL_BLOCK_MIN = Vector3f.ZERO;
    private static final Vector3f FULL_BLOCK_MAX = new Vector3f(16, 16, 16);

    private Vector3f from = FULL_BLOCK_MIN, to = FULL_BLOCK_MAX;
    private Rotation rotation = Rotation.ZERO;
    private boolean shade = true;
    private EnumMap<Direction, Face> faces = new EnumMap<>(Direction.class);

    private Element(){}

    private void init() {
        faces.forEach((direction, face) -> face.init(direction, this::calculateDefaultUV));
    }

    private Vector4f calculateDefaultUV(Direction face) {
        switch (face){

            case DOWN :
            case UP :
                return new Vector4f(
                        from.getX(), from.getZ(),
                        to.getX(),   to.getZ()
                );

            case NORTH :
            case SOUTH :
                return new Vector4f(
                        from.getX(), from.getY(),
                        to.getX(),   to.getY()
                );

            case WEST :
            case EAST :
                return new Vector4f(
                        from.getZ(), from.getY(),
                        to.getZ(),   to.getY()
                );

            default :
                return new Vector4f(
                        0, 0,
                        16, 16
                );

        }
    }

    public Vector3f getFrom() {
        return from;
    }

    public Vector3f getTo() {
        return to;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean isShade() {
        return shade;
    }

    public EnumMap<Direction, Face> getFaces() {
        return faces;
    }

    boolean isFullCube() {
        if (!(FULL_BLOCK_MIN.equals(from) && FULL_BLOCK_MAX.equals(to))) return false;
        for (Direction dir : Direction.values()) {
            if (!faces.containsKey(dir)) return false;
        }
        return true;
    }

    static class Adapter extends AbstractTypeAdapterFactory<Element> {

        public Adapter() {
            super(Element.class);
        }

        @Override
        public Element read(JsonReader in, Gson gson) throws IOException {
            Element element = gson.getDelegateAdapter(this, TypeToken.get(Element.class)).read(in);
            element.init();
            return element;
        }

    }

}
