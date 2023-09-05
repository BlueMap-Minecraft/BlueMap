package de.bluecolored.bluemap.core.mca.data;

import lombok.Getter;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class BiomesData {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    private String[] palette = EMPTY_STRING_ARRAY;
    private long[] data = EMPTY_LONG_ARRAY;

}
