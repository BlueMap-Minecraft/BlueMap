package de.bluecolored.bluemap.core.mca.region;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public enum RegionType {

    MCA (MCARegion::new, MCARegion.FILE_SUFFIX, MCARegion::getRegionFileName),
    LINEAR (LinearRegion::new, LinearRegion.FILE_SUFFIX, LinearRegion::getRegionFileName);

    // we do this to improve performance, as calling values() creates a new array each time
    private final static RegionType[] VALUES = values();
    private final static RegionType DEFAULT = MCA;

    private final String fileSuffix;
    private final RegionFactory regionFactory;
    private final RegionFileNameFunction regionFileNameFunction;

    RegionType(RegionFactory regionFactory, String fileSuffix, RegionFileNameFunction regionFileNameFunction) {
        this.fileSuffix = fileSuffix;
        this.regionFactory = regionFactory;
        this.regionFileNameFunction = regionFileNameFunction;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public Region createRegion(MCAWorld world, Path regionFile) {
        return this.regionFactory.create(world, regionFile);
    }

    public String getRegionFileName(int regionX, int regionZ) {
        return regionFileNameFunction.getRegionFileName(regionX, regionZ);
    }

    public Path getRegionFile(Path regionFolder, int regionX, int regionZ) {
        return regionFolder.resolve(getRegionFileName(regionX, regionZ));
    }

    @Nullable
    public static RegionType forFileName(String fileName) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < VALUES.length; i++) {
            RegionType regionType = VALUES[i];
            if (fileName.endsWith(regionType.fileSuffix))
                return regionType;
        }
        return null;
    }

    @NotNull
    public static Region loadRegion(MCAWorld world, Path regionFolder, int regionX, int regionZ) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < VALUES.length; i++) {
            RegionType regionType = VALUES[i];
            Path regionFile = regionType.getRegionFile(regionFolder, regionX, regionZ);
            if (Files.exists(regionFile)) return regionType.createRegion(world, regionFile);
        }
        return DEFAULT.createRegion(world, DEFAULT.getRegionFile(regionFolder, regionX, regionZ));
    }

    @FunctionalInterface
    interface RegionFactory {
        Region create(MCAWorld world, Path regionFile);
    }

    @FunctionalInterface
    interface RegionFileNameFunction {
        String getRegionFileName(int regionX, int regionZ);
    }

}
