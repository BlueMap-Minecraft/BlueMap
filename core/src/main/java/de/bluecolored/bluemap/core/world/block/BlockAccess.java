package de.bluecolored.bluemap.core.world.block;

import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface BlockAccess {

    void set(int x, int y, int z);

    @Contract(" -> new")
    BlockAccess copy();

    int getX();
    int getY();
    int getZ();

    BlockState getBlockState();
    LightData getLightData();
    Biome getBiome();
    @Nullable BlockEntity getBlockEntity();

    boolean hasOceanFloorY();
    int getOceanFloorY();

    default int getSunLightLevel() {
        return getLightData().getSkyLight();
    }

    default int getBlockLightLevel() {
        return getLightData().getBlockLight();
    }

}
