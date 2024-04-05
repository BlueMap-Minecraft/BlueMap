package de.bluecolored.bluemap.core.storage;

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;

public interface SingleItemStorage {

    /**
     * Returns an {@link OutputStream} that can be used to write the item-data of this storage
     * (overwriting any existing item).
     * The OutputStream is expected to be closed by the caller of this method.
     */
    OutputStream write() throws IOException;

    /**
     * Returns a {@link CompressedInputStream} that can be used to read the item-data from this storage
     * or null if there is nothing stored.
     * The CompressedInputStream is expected to be closed by the caller of this method.
     */
    @Nullable CompressedInputStream read() throws IOException;

    /**
     * Deletes the item from this storage
     */
    void delete() throws IOException;

    /**
     * Tests if this item of this storage exists
     */
    boolean exists() throws IOException;

    /**
     * Checks if this storage is closed
     */
    boolean isClosed();

}
