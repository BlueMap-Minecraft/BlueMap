package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;

import java.util.function.Function;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
public class Face {

    private static final TextureVariable DEFAULT_TEXTURE = new TextureVariable((ResourcePath<Texture>) null);

    private Vector4f uv;
    private TextureVariable texture = DEFAULT_TEXTURE;
    private Direction cullface;
    private int rotation = 0;
    private int tintindex = -1;

    private Face(){}

    void init(Direction direction, Function<Direction, Vector4f> defaultUvCalculator) {
        if (cullface == null) cullface = direction;
        if (uv == null) uv = defaultUvCalculator.apply(direction);
    }

    public Vector4f getUv() {
        return uv;
    }

    public TextureVariable getTexture() {
        return texture;
    }

    public Direction getCullface() {
        return cullface;
    }

    public int getRotation() {
        return rotation;
    }

    public int getTintindex() {
        return tintindex;
    }

}
