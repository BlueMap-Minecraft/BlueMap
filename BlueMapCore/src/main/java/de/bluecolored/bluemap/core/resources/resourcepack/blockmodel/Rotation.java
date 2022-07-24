package de.bluecolored.bluemap.core.resources.resourcepack.blockmodel;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.util.math.Axis;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;

import java.io.IOException;

@SuppressWarnings("FieldMayBeFinal")
@DebugDump
@JsonAdapter(Rotation.Adapter.class)
public class Rotation {
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(8, 8, 8);
    private static final double FIT_TO_BLOCK_SCALE_MULTIPLIER = 2 - Math.sqrt(2);

    public static final Rotation ZERO = new Rotation();
    static {
        ZERO.init();
    }

    private Vector3f origin = DEFAULT_ORIGIN;
    private Axis axis = Axis.Y;
    private float angle = 0;
    private boolean rescale = false;

    private transient MatrixM4f matrix;

    private Rotation(){}

    private void init() {
        Vector3i axisAngle = axis.toVector();

        matrix = new MatrixM4f();
        if (angle != 0f) {
            matrix.translate(-origin.getX(), -origin.getY(), -origin.getZ());
            matrix.rotate(
                    angle,
                    axisAngle.getX(),
                    axisAngle.getY(),
                    axisAngle.getZ()
            );

            if (rescale) {
                float scale = (float) (Math.abs(TrigMath.sin(angle * TrigMath.DEG_TO_RAD)) * FIT_TO_BLOCK_SCALE_MULTIPLIER);
                matrix.scale(
                        (1 - axisAngle.getX()) * scale + 1,
                        (1 - axisAngle.getY()) * scale + 1,
                        (1 - axisAngle.getZ()) * scale + 1
                );
            }

            matrix.translate(origin.getX(), origin.getY(), origin.getZ());
        }
    }

    public Vector3f getOrigin() {
        return origin;
    }

    public Axis getAxis() {
        return axis;
    }

    public double getAngle() {
        return angle;
    }

    public boolean isRescale() {
        return rescale;
    }

    public MatrixM4f getMatrix() {
        return matrix;
    }

    static class Adapter extends AbstractTypeAdapterFactory<Rotation> {

        public Adapter() {
            super(Rotation.class);
        }

        @Override
        public Rotation read(JsonReader in, Gson gson) throws IOException {
            Rotation rotation = gson.getDelegateAdapter(this, TypeToken.get(Rotation.class)).read(in);
            rotation.init();
            return rotation;
        }

    }

}
