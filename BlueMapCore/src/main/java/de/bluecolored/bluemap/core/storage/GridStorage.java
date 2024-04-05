package de.bluecolored.bluemap.core.storage;

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * A storage storing items on an infinite grid (x,z), each position on the grid can hold one item.
 */
public interface GridStorage {

    /**
     * Returns an {@link OutputStream} that can be used to write an item into this storage at the given position
     * (overwriting any existing item).
     * The OutputStream is expected to be closed by the caller of this method.
     */
    OutputStream write(int x, int z) throws IOException;

    /**
     * Returns a {@link CompressedInputStream} that can be used to read the item from this storage at the given position
     * or null if there is no item stored.
     * The CompressedInputStream is expected to be closed by the caller of this method.
     */
    @Nullable CompressedInputStream read(int x, int z) throws IOException;

    /**
     * Deletes the item from this storage at the given position
     */
    void delete(int x, int z) throws IOException;

    /**
     * Tests if there is an item stored on the given position in this storage
     */
    boolean exists(int x, int z) throws IOException;

    /**
     * Returns a stream over all <b>existing</b> items in this storage
     */
    Stream<Cell> stream() throws IOException;

    /**
     * Checks if this storage is closed
     */
    boolean isClosed();

    interface Cell extends SingleItemStorage {

        /**
         * Returns the x position of this item in the grid
         */
        int getX();

        /**
         * Returns the z position of this item in the grid
         */
        int getZ();

    }

    @SuppressWarnings("ClassCanBeRecord")
    @Getter
    @RequiredArgsConstructor
    class GridStorageCell implements Cell {

        private final GridStorage storage;
        private final int x, z;

        @Override
        public OutputStream write() throws IOException {
            return storage.write(x, z);
        }

        @Override
        public CompressedInputStream read() throws IOException {
            return storage.read(x, z);
        }

        @Override
        public void delete() throws IOException {
            storage.delete(x, z);
        }

        @Override
        public boolean exists() throws IOException {
            return storage.exists(x, z);
        }

        @Override
        public boolean isClosed() {
            return storage.isClosed();
        }

    }

}
