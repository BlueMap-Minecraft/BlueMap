package de.bluecolored.bluemap.core.map.mask;

import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlurMask implements Mask {

    private final CombinedMask masks;
    private final int size;

    @Override
    public boolean test(int x, int y, int z) {
        return masks.test(
                x + randomOffset(x, y, z, 23948),
                y + randomOffset(x, y, z, 53242),
                z + randomOffset(x, y, z, 75654)
        );
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return masks.test(
                minX - size, minY - size, minZ - size,
                maxX + size, maxY + size, maxZ + size
        );
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        return masks.isEdge(
                minX - size, minZ - size,
                maxX + size, maxZ + size
        );
    }

    private int randomOffset(int x, int y, int z, long seed) {
        final long hash = x * 73428767L ^ y * 4382893L ^ z * 2937119L ^ seed * 457;
        return (int)((((hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000) - 0.5f) * 2 * size);
    }

}
