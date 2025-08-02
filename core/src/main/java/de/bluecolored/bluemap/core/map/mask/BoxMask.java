package de.bluecolored.bluemap.core.map.mask;

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoxMask implements Mask {

    private final Vector3i min, max;

    @Override
    public boolean test(int x, int y, int z) {
        return
                testXZ(x, z) &&
                y >= min.getY() &&
                y <= max.getY();
    }

    public boolean testXZ(int x, int z) {
        return
                x >= min.getX() &&
                x <= max.getX() &&
                z >= min.getZ() &&
                z <= max.getZ();
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        if (
                minX >= min.getX() && maxX <= max.getX() &&
                minZ >= min.getZ() && maxZ <= max.getZ() &&
                minY >= min.getY() && maxY <= max.getY()
        ) return Tristate.TRUE;

        if (
                maxX < min.getX() || minX > max.getX() ||
                maxZ < min.getZ() || minZ > max.getZ() ||
                maxY < min.getY() || minY > max.getY()
        ) return Tristate.FALSE;

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        return test(minX, min.getY(), minZ, maxX, max.getY(), maxZ) == Tristate.UNDEFINED;
    }

}
