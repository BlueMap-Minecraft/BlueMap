package de.bluecolored.bluemap.core.debug;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.mca.mapping.BlockPropertiesMapper;
import de.bluecolored.bluemap.core.world.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

public class OneBlockWorld implements World {

    private final World delegate;

    public OneBlockWorld(World delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public UUID getUUID() {
        return delegate.getUUID();
    }

    @Override
    public Path getSaveFolder() {
        return delegate.getSaveFolder();
    }

    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    public Vector3i getSpawnPoint() {
        return new Vector3i(0, 70, 0);
    }

    @Override
    public int getMaxY(int x, int z) {
        return 255;
    }

    @Override
    public int getMinY(int x, int z) {
        return 0;
    }

    @Override
    public Grid getChunkGrid() {
        return delegate.getChunkGrid();
    }

    @Override
    public Grid getRegionGrid() {
        return delegate.getRegionGrid();
    }

    @Override
    public Biome getBiome(int x, int y, int z) {
        return Biome.DEFAULT;
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        if (x == 0 && z == 0 && y == 70) return BlockState.MISSING;
        return BlockState.AIR;
    }

    @Override
    public BlockProperties getBlockProperties(BlockState blockState) {
        return delegate.getBlockProperties(blockState);
    }

    @Override
    public Chunk getChunkAtBlock(int x, int y, int z) {
        return delegate.getChunkAtBlock(x, y, z);
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return delegate.getChunk(x, z);
    }

    @Override
    public Region getRegion(int x, int z) {
        return delegate.getRegion(x, z);
    }

    @Override
    public Collection<Vector2i> listRegions() {
        return delegate.listRegions();
    }

    @Override
    public void invalidateChunkCache() {
        delegate.invalidateChunkCache();
    }

    @Override
    public void invalidateChunkCache(int x, int z) {
        delegate.invalidateChunkCache(x, z);
    }

    @Override
    public void cleanUpChunkCache() {
        delegate.cleanUpChunkCache();
    }

    @Override
    public BlockPropertiesMapper getBlockPropertiesMapper() {
        return delegate.getBlockPropertiesMapper();
    }

}
