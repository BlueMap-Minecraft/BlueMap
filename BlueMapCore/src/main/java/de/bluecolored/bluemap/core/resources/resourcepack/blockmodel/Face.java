package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Direction;

import java.util.function.Function;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
public class Face {

    private static final TextureVariable DEFAULT_TEXTURE = new TextureVariable(ResourcePack.MISSING_TEXTURE);

    private Vector4f uv;
    private TextureVariable texture = DEFAULT_TEXTURE;
    private Direction cullface;
    private int rotation = 0;
    private int tintindex = -1;

    @SuppressWarnings("unused")
    private Face() {}

    private Face(Face copyFrom) {
        this.uv = copyFrom.uv;
        this.texture = copyFrom.texture.copy();
        this.cullface = copyFrom.cullface;
        this.rotation = copyFrom.rotation;
        this.tintindex = copyFrom.tintindex;
    }

    void init(Direction direction, Function<Direction, Vector4f> defaultUvCalculator, Function<Direction, Direction> defaultCullfaceCalculator) {
        if (cullface == null) cullface = defaultCullfaceCalculator.apply(direction);
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

    public Face copy() {
        return new Face(this);
    }

    public void optimize(ResourcePack resourcePack) {
        this.texture.optimize(resourcePack);
    }
}
