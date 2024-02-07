package de.bluecolored.bluemap.core.world.mca;

public class PackedIntArrayAccess {

    // magic constants for fast division
    private static final int[] DIVISION_MAGIC = new int[]{
            // <editor-fold defaultstate="collapsed" desc="Division-Magic Constants">
            -1, -1, 0,
            Integer.MIN_VALUE, 0, 0,
            1431655765, 1431655765, 0,
            Integer.MIN_VALUE, 0, 1,
            858993459, 858993459, 0,
            715827882, 715827882, 0,
            613566756, 613566756, 0,
            Integer.MIN_VALUE, 0, 2,
            477218588, 477218588, 0,
            429496729, 429496729, 0,
            390451572, 390451572, 0,
            357913941, 357913941, 0,
            330382099, 330382099, 0,
            306783378, 306783378, 0,
            286331153, 286331153, 0,
            Integer.MIN_VALUE, 0, 3,
            252645135, 252645135, 0,
            238609294, 238609294, 0,
            226050910, 226050910, 0,
            214748364, 214748364, 0,
            204522252, 204522252, 0,
            195225786, 195225786, 0,
            186737708, 186737708, 0,
            178956970, 178956970, 0,
            171798691, 171798691, 0,
            165191049, 165191049, 0,
            159072862, 159072862, 0,
            153391689, 153391689, 0,
            148102320, 148102320, 0,
            143165576, 143165576, 0,
            138547332, 138547332, 0,
            Integer.MIN_VALUE, 0, 4,
            130150524, 130150524, 0,
            126322567, 126322567, 0,
            122713351, 122713351, 0,
            119304647, 119304647, 0,
            116080197, 116080197, 0,
            113025455, 113025455, 0,
            110127366, 110127366, 0,
            107374182, 107374182, 0,
            104755299, 104755299, 0,
            102261126, 102261126, 0,
            99882960, 99882960, 0,
            97612893, 97612893, 0,
            95443717, 95443717, 0,
            93368854, 93368854, 0,
            91382282, 91382282, 0,
            89478485, 89478485, 0,
            87652393, 87652393, 0,
            85899345, 85899345, 0,
            84215045, 84215045, 0,
            82595524, 82595524, 0,
            81037118, 81037118, 0,
            79536431, 79536431, 0,
            78090314, 78090314, 0,
            76695844, 76695844, 0,
            75350303, 75350303, 0,
            74051160, 74051160, 0,
            72796055, 72796055, 0,
            71582788, 71582788, 0,
            70409299, 70409299, 0,
            69273666, 69273666, 0,
            68174084, 68174084, 0,
            Integer.MIN_VALUE, 0, 5
            // </editor-fold>
    };

    private final int bitsPerElement;
    private final long[] data;

    private final int elementsPerLong, indexShift;
    private final long maxValue, indexScale, indexOffset;

    public PackedIntArrayAccess(long[] data, int elementCount) {
        this(Math.max(data.length * Long.SIZE / elementCount, 1), data);
    }

    public PackedIntArrayAccess(int bitsPerElement, long[] data) {
        this.bitsPerElement = bitsPerElement;
        this.data = data;

        this.maxValue = (1L << this.bitsPerElement) - 1L;
        this.elementsPerLong = 64 / this.bitsPerElement;

        int i = 3 * (this.elementsPerLong - 1);
        this.indexScale = Integer.toUnsignedLong(DIVISION_MAGIC[i]);
        this.indexOffset = Integer.toUnsignedLong(DIVISION_MAGIC[i + 1]);
        this.indexShift = DIVISION_MAGIC[i + 2] + 32;
    }

    public int get(int i) {
        int storageIndex = this.storageIndex(i);
        if (storageIndex >= this.data.length) return 0;
        long l = this.data[storageIndex];
        int offset = (i - storageIndex * this.elementsPerLong) * this.bitsPerElement;
        return (int)(l >> offset & this.maxValue);
    }

    private int storageIndex(int i) {
        // this is the same as doing: floor(i / elementsPerLong)
        return (int) ((long) i * this.indexScale + this.indexOffset >> this.indexShift);
    }

    public int getCapacity() {
        return data.length * elementsPerLong;
    }

    public boolean isCorrectSize(int expectedSize) {
        int capacity = getCapacity();
        return expectedSize <= capacity && expectedSize + elementsPerLong > capacity;
    }

}