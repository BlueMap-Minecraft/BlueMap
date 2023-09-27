package de.bluecolored.bluemap.core.mca.data;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class ChunkData {
    private static final HeightmapsData EMPTY_HEIGHTMAPS_DATA = new HeightmapsData();

    private int dataVersion = 0;
    private String status = "none";
    private long inhabitedTime = 0;
    private HeightmapsData heightmaps = EMPTY_HEIGHTMAPS_DATA;
    private @Nullable SectionData[] sections = null;

    // <= 1.16
    private int[] biomes;

}
