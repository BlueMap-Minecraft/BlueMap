package de.bluecolored.bluemap.core.resources.resourcepack.blockstate;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.resourcepack.blockmodel.BlockModel;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;

import java.io.IOException;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@JsonAdapter(Variant.Adapter.class)
public class Variant {

    private ResourcePath<BlockModel> model = ResourcePack.MISSING_BLOCK_MODEL;
    private float x = 0, y = 0;
    private boolean uvlock = false;
    private double weight = 1;

    private transient boolean rotated;
    private transient MatrixM3f rotationMatrix;

    private Variant(){}

    private void init() {
        this.rotated = x != 0 || y != 0;
        this.rotationMatrix = new MatrixM3f().rotate(-x, -y, 0);
    }

    public ResourcePath<BlockModel> getModel() {
        return model;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isUvlock() {
        return uvlock;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isRotated() {
        return rotated;
    }

    public MatrixM3f getRotationMatrix() {
        return rotationMatrix;
    }

    static class Adapter extends AbstractTypeAdapterFactory<Variant> {

        public Adapter() {
            super(Variant.class);
        }

        @Override
        public Variant read(JsonReader in, Gson gson) throws IOException {
            Variant variant = gson.getDelegateAdapter(this, TypeToken.get(Variant.class)).read(in);
            variant.init();
            return variant;
        }

    }

}
