package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface WorldLoader {

    /**
     * Loads the world (one dimension of a level) from a Path, a dimension-key and a DataPack.<br>
     * The Path is deserialized directly from the map-config and could either be directly the location of the world-data (world-folder)
     * or it could be the path to another config-file that is providing more information on how to load this world for the WorldLoader.<br>
     * It is up to the implementation of the WorldLoader how to interpret the path.
     */
    World loadWorld(Path path, Key dimension, DataPack dataPack) throws IOException, InterruptedException;

    /**
     * Returns a list of DataPacks that should be loaded additionally when loading the provided Path / dimension.
     */
    default List<Path> worldDataPacks(Path path, Key dimension) throws IOException, InterruptedException {
        Path worldPacksFolder = path.resolve("datapacks");
        if (Files.isDirectory(worldPacksFolder)) {
            try (Stream<Path> worldPacksStream = Files.list(worldPacksFolder)) {
                return worldPacksStream.toList();
            }
        }
        return List.of();
    }

}
