package de.bluecolored.bluemap.core.storage;

import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.Key;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class KeyedMapStorage implements MapStorage {

    private static final Key HIRES_TILES_KEY = Key.bluemap("hires");
    private static final Key TILE_STATE_KEY = Key.bluemap("tile-state");
    private static final Key CHUNK_STATE_KEY = Key.bluemap("chunk-state");
    private static final Key SETTINGS_KEY = Key.bluemap("settings");
    private static final Key TEXTURES_KEY = Key.bluemap("textures");
    private static final Key MARKERS_KEY = Key.bluemap("markers");
    private static final Key PLAYERS_KEY = Key.bluemap("players");

    private final Compression compression;

    @Override
    public GridStorage hiresTiles() {
        return grid(HIRES_TILES_KEY, compression);
    }

    @Override
    public GridStorage lowresTiles(int lod) {
        return grid(Key.bluemap("lowres/" + lod), Compression.NONE);
    }

    @Override
    public GridStorage tileState() {
        return grid(TILE_STATE_KEY, Compression.GZIP);
    }

    @Override
    public GridStorage chunkState() {
        return grid(CHUNK_STATE_KEY, Compression.GZIP);
    }

    @Override
    public ItemStorage asset(String name) {
        return item(Key.bluemap("asset/" + MapStorage.escapeAssetName(name)), Compression.NONE);
    }

    @Override
    public ItemStorage settings() {
        return item(SETTINGS_KEY, Compression.NONE);
    }

    @Override
    public ItemStorage textures() {
        return item(TEXTURES_KEY, compression);
    }

    @Override
    public ItemStorage markers() {
        return item(MARKERS_KEY, Compression.NONE);
    }

    @Override
    public ItemStorage players() {
        return item(PLAYERS_KEY, Compression.NONE);
    }

    /**
     * Returns a {@link GridStorage} for the given {@link Key}.<br>
     * The compressionHint can be used if a new {@link GridStorage} needs to be created, but is not guaranteed.
     */
    public abstract GridStorage grid(Key key, Compression compressionHint);

    /**
     * Returns a {@link ItemStorage} for the given {@link Key}.<br>
     * The compressionHint can be used if a new {@link ItemStorage} needs to be created, but is not guaranteed.
     */
    public abstract ItemStorage item(Key key, Compression compressionHint);

}
