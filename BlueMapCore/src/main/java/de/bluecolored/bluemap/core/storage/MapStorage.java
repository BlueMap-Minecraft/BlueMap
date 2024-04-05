package de.bluecolored.bluemap.core.storage;

import java.io.IOException;
import java.util.function.DoublePredicate;

public interface MapStorage {

    /**
     * Returns the {@link GridStorage} holding the maps hires-tiles
     */
    GridStorage hiresTiles();

    /**
     * Returns the {@link GridStorage} holding the maps lowres-tiles of the given lod level
     */
    GridStorage lowresTiles(int lod);

    /**
     * Returns a {@link SingleItemStorage} for a map asset with the given name
     */
    SingleItemStorage asset(String name);

    /**
     * Returns a {@link SingleItemStorage} for the render-state data of this map
     */
    SingleItemStorage renderState();

    /**
     * Returns a {@link SingleItemStorage} for the settings (settings.json) of this map
     */
    SingleItemStorage settings();

    /**
     * Returns a {@link SingleItemStorage} for the texture-data (textures.json) of this map
     */
    SingleItemStorage textures();

    /**
     * Returns a {@link SingleItemStorage} for the marker-data (live/markers.json) of this map
     */
    SingleItemStorage markers();

    /**
     * Returns a {@link SingleItemStorage} for the player-data (live/players.json) of this map
     */
    SingleItemStorage players();

    /**
     * Deletes the entire map from the storage
     */
    default void delete() throws IOException {
        delete(info -> true);
    }

    /**
     * Deletes the entire map from the storage
     * @param onProgress a function that takes in a progress-percentage and returns true
     *                   if the deletion should continue or false if it should be aborted.
     *                   No guarantees are made on how often (if at all) this method is actually being called and if the
     *                   progress is actually aborted when false is returned.
     */
    void delete(DoublePredicate onProgress) throws IOException;

    /**
     * Tests whether this map currently exists on the storage or not
     */
    boolean exists() throws IOException;

    /**
     * Checks if this storage is closed
     */
    boolean isClosed();

    static String escapeAssetName(String name) {
        return name
                .replaceAll("[^\\w\\d.\\-_/]", "_")
                .replace("..", "_.");
    }

}
