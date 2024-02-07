package de.bluecolored.bluemap.core.world;

@FunctionalInterface
public interface ChunkConsumer {

    default boolean filter(int chunkX, int chunkZ, long lastModified) {
        return true;
    }

    void accept(int chunkX, int chunkZ, Chunk chunk);

    @FunctionalInterface
    interface ListOnly extends ChunkConsumer {

        void accept(int chunkX, int chunkZ, long lastModified);

        @Override
        default boolean filter(int chunkX, int chunkZ, long lastModified) {
            accept(chunkX, chunkZ, lastModified);
            return false;
        }

        @Override
        default void accept(int chunkX, int chunkZ, Chunk chunk) {
            throw new IllegalStateException("Should never be called.");
        }

    }

}
