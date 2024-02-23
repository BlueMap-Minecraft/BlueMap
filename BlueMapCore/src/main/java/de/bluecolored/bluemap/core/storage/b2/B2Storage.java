package de.bluecolored.bluemap.core.storage.b2;

import com.backblaze.b2.client.B2ListFilesIterable;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2NotFoundException;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.core.util.OnCloseOutputStream;

import java.io.*;
import java.nio.file.Path;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class B2Storage extends Storage {
    private boolean closed = false;
    private final B2StorageClient client;
    private final Compression hiresCompression;
    private final String bucket;

    public B2Storage(B2StorageSettings settings) {
        this.client = B2StorageClientFactory.createDefaultFactory().create(settings.getApplicationKeyId(), settings.getApplicationKey(), "BlueMap/unknownversion");
        this.hiresCompression = settings.getCompression();
        this.bucket = settings.getBucket();
    }
    @Override
    public void initialize() throws IOException {}

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new OnCloseOutputStream(new BufferedOutputStream(compression.compress(byteOut)), () -> {
            client.uploadSmallFile(B2UploadFileRequest.builder(
                    bucket,
                    path,
                    getFileMime(lod),
                    B2ByteArrayContentSource.build(byteOut.toByteArray())

            ).build());
        });
    }

    @Override
    public Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);
        B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        try {
            client.downloadByName(bucket, path, writer);
            ByteArrayInputStream is = new ByteArrayInputStream(writer.getBytes());
            return Optional.of(new CompressedInputStream(is, compression));
        } catch (B2Exception e) {
            if (e instanceof B2NotFoundException) {
                return Optional.empty();
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public Optional<TileInfo> readMapTileInfo(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);
        try {
            B2FileVersion info = client.getFileInfoByName(bucket, path);
            return Optional.of(new TileInfo() {
                @Override
                public CompressedInputStream readMapTile() throws IOException {
                    return B2Storage.this.readMapTile(mapId, lod, tile)
                            .orElseThrow(() -> new IOException("Tile no longer present!"));
                }

                @Override
                public Compression getCompression() {
                    return compression;
                }

                @Override
                public long getSize() {
                    return info.getContentLength();
                }

                @Override
                public long getLastModified() {
                    return info.getUploadTimestamp();
                }
            });
        } catch (B2Exception e) {
            if (e instanceof B2NotFoundException) {
                return Optional.empty();
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void deleteMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        String path = getFilePath(mapId, lod, tile);
        try {
            client.hideFile(bucket, path);
        } catch (B2Exception e) {
            if (!(e instanceof B2NotFoundException)) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new OnCloseOutputStream(new BufferedOutputStream(byteOut), () -> {
            client.uploadSmallFile(B2UploadFileRequest.builder(
                    bucket,
                    path,
                    "b2/x-auto",
                    B2ByteArrayContentSource.build(byteOut.toByteArray())

            ).build());
        });
    }

    @Override
    public Optional<InputStream> readMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        try {
            client.downloadByName(bucket, path, writer);
            ByteArrayInputStream is = new ByteArrayInputStream(writer.getBytes());
            return Optional.of(is);
        } catch (B2Exception e) {
            if (e instanceof B2NotFoundException) {
                return Optional.empty();
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            B2FileVersion info = client.getFileInfoByName(bucket, path);
            return Optional.of(new MetaInfo() {
                @Override
                public InputStream readMeta() throws IOException {
                    return B2Storage.this.readMeta(mapId, name)
                            .orElseThrow(() -> new IOException("Meta no longer present!"));
                }

                @Override
                public long getSize() {
                    return info.getContentLength();
                }
            });
        } catch (B2Exception e) {
            if (e instanceof B2NotFoundException) {
                return Optional.empty();
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void deleteMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            client.hideFile(bucket, path);
        } catch (B2Exception e) {
            if (!(e instanceof B2NotFoundException)) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException {
        String directory = getFilePath(mapId);
        try {
            B2ListFilesIterable files = client.fileNames(B2ListFileNamesRequest.builder(bucket).setWithinFolder(directory).build());
            List<B2FileVersion> filesList = StreamSupport.stream(files.spliterator(), false).collect(Collectors.toList());

            for (int i = 0; i < filesList.size(); i++) {
                var file = filesList.get(i);
                client.hideFile(bucket, file.getFileName());

                if (!onProgress.apply(
                        new ProgressInfo((double) (i + 1) / filesList.size())
                )) return;
            }
        } catch (B2Exception e) {
            if (!(e instanceof B2NotFoundException)) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public Collection<String> collectMapIds() throws IOException {
        try {
            B2ListFilesIterable files = client.fileNames(B2ListFileNamesRequest.builder(bucket).setDelimiter("/").build());
            List<String> ids = new ArrayList<>();
            for (var file: files) {
                String id = file.getFileName().split("/")[0];
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (B2Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        client.close();
    }

    public String getFileMime(int lod){
        if (lod == 0) {
            if (hiresCompression.getFileSuffix().equals(".gz")) {
                return "application/gzip";
            } else if (hiresCompression == Compression.NONE) {
                return "application/json";
            } else {
                return "application/octet-stream";
            }
        } else {
            return "image/png";
        }
    }

    public String getFilePath(String mapId, int lod, Vector2i tile){
        String path = "x" + tile.getX() + "z" + tile.getY();
        char[] cs = path.toCharArray();
        List<String> folders = new ArrayList<>();
        StringBuilder folder = new StringBuilder();
        for (char c : cs){
            folder.append(c);
            if (c >= '0' && c <= '9'){
                folders.add(folder.toString());
                folder.delete(0, folder.length());
            }
        }
        String fileName = folders.remove(folders.size() - 1);

        String p = getFilePath(mapId) + "/tiles/" + Integer.toString(lod);
        for (String s : folders){
            p += "/" + s;
        }

        if (lod == 0) {
            return p + "/" + (fileName + ".json" + hiresCompression.getFileSuffix());
        } else {
            return p + "/" + (fileName + ".png");
        }
    }

    public String getFilePath(String mapId) {
        return mapId;
    }

    public String getMetaFilePath(String mapId, String name) {
        return getFilePath(mapId) + "/" + escapeMetaName(name);
    }
}
