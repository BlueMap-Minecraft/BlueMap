package de.bluecolored.bluemap.core.storage.file;

import de.bluecolored.bluemap.core.storage.Compression;

import java.nio.file.Path;

public interface FileStorageSettings {

    Path getRoot();

    Compression getCompression();

}
