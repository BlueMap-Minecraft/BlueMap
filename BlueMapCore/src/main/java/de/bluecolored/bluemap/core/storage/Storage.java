package de.bluecolored.bluemap.core.storage;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;

public interface Storage extends Closeable {

    /**
     * Does everything necessary to initialize this storage.
     * (E.g. create tables on a database if they don't exist or upgrade older data).
     */
    void initialize() throws IOException;

    /**
     * Returns the {@link MapStorage} for the given mapId
     */
    MapStorage map(String mapId);

    /**
     * Fetches and returns a stream of all map-id's in this storage
     */
    Stream<String> mapIds() throws IOException;

    /**
     * Checks if this storage is closed
     */
    boolean isClosed();

}
