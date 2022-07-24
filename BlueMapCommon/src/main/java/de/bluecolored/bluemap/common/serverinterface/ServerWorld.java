package de.bluecolored.bluemap.common.serverinterface;

import de.bluecolored.bluemap.api.debug.DebugDump;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface ServerWorld {

    @DebugDump
    default Optional<String> getId() {
        return Optional.empty();
    }

    @DebugDump
    default Optional<String> getName() {
        return Optional.empty();
    }

    @DebugDump
    Path getSaveFolder();

    @DebugDump
    default Dimension getDimension() {
        Path saveFolder = getSaveFolder();
        String lastName = saveFolder.getFileName().toString();
        if (lastName.equals("DIM-1")) return Dimension.NETHER;
        if (lastName.equals("DIM1")) return Dimension.END;
        return Dimension.OVERWORLD;
    }

    /**
     * Attempts to persist all changes that have been made in a world to disk.
     *
     * @return <code>true</code> if the changes have been successfully persisted, <code>false</code> if this operation is not supported by the implementation
     *
     * @throws IOException if something went wrong trying to persist the changes
     */
    default boolean persistWorldChanges() throws IOException {
        return false;
    }

}
