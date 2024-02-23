package de.bluecolored.bluemap.core.storage.s3;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.core.util.OnCloseOutputStream;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class S3Storage extends Storage {
    private boolean closed = false;
    private final S3Client client;
    private final Compression hiresCompression;
    private final String bucket;
    public S3Storage(S3StorageSettings settings) {
        AwsSessionCredentials awsCreds = AwsSessionCredentials.create(settings.getAccessKey(), settings.getSecretKey(), "");
        var builder = S3Client
                .builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(settings.getRegion()));
        if (settings.getEndpoint().isPresent()) {
            builder = builder.endpointOverride(URI.create(settings.getEndpoint().get()));
        }
        this.client = builder.build();
        this.hiresCompression = settings.getCompression();
        this.bucket = settings.getBucket();
    }
    @Override
    public void initialize() throws IOException {}

    @Override
    public OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);

        OutputStream pipeOut = makeUploadStream(path, getFileMime(lod));
        return new BufferedOutputStream(compression.compress(pipeOut));
    }

    @Override
    public Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);
        try {
            InputStream is = client.getObject(
                    GetObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(new CompressedInputStream(is, compression));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<TileInfo> readMapTileInfo(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);
        try {
            var info = client.headObject(
                    HeadObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(new TileInfo() {
                @Override
                public CompressedInputStream readMapTile() throws IOException {
                    return S3Storage.this.readMapTile(mapId, lod, tile)
                            .orElseThrow(() -> new IOException("Tile no longer present!"));
                }

                @Override
                public Compression getCompression() {
                    return compression;
                }

                @Override
                public long getSize() {
                    return info.contentLength();
                }

                @Override
                public long getLastModified() {
                    return info.lastModified().getEpochSecond();
                }
            });
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteMapTile(String mapId, int lod, Vector2i tile) throws IOException {
        String path = getFilePath(mapId, lod, tile);
        try {
            client.deleteObject(
                    DeleteObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
        } catch (NoSuchKeyException ignored) {
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream writeMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);

        OutputStream pipeOut = makeUploadStream(path, null);
        return new BufferedOutputStream(pipeOut);
    }

    private OutputStream makeUploadStream(String path, String FileMime) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        return new OnCloseOutputStream(new BufferedOutputStream(byteOut), () ->
                client.putObject(
                        PutObjectRequest
                                .builder()
                                .bucket(bucket)
                                .key(path)
                                .contentType(FileMime)
                                .build(),
                        RequestBody.fromBytes(byteOut.toByteArray())
                )
        );
    }

    @Override
    public Optional<InputStream> readMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            InputStream is = client.getObject(
                    GetObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(is);
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            var info = client.headObject(
                    HeadObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(new MetaInfo() {
                @Override
                public InputStream readMeta() throws IOException {
                    return S3Storage.this.readMeta(mapId, name)
                            .orElseThrow(() -> new IOException("Meta no longer present!"));
                }

                @Override
                public long getSize() {
                    return info.contentLength();
                }
            });
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteMeta(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            client.deleteObject(
                    DeleteObjectRequest
                            .builder()
                            .bucket(bucket)
                            .key(path)
                            .build()
            );
        } catch (NoSuchKeyException ignored) {
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException {
        String directory = getFilePath(mapId);
        try {
            var files = client.listObjectsV2Paginator(
                    ListObjectsV2Request
                            .builder()
                            .bucket(bucket)
                            .prefix(directory + "/")
                            .build()
            );
            var filesList = files.contents().stream().collect(Collectors.toList());
            for (int i = 0; i < filesList.size(); i++) {
                var file = filesList.get(i);
                try {
                    client.deleteObject(
                            DeleteObjectRequest
                                    .builder()
                                    .bucket(bucket)
                                    .key(file.key())
                                    .build()
                    );
                } catch (NoSuchKeyException ignored) {}

                if (!onProgress.apply(
                        new ProgressInfo((double) (i + 1) / filesList.size())
                )) return;
            }
        } catch (SdkException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Collection<String> collectMapIds() throws IOException {
        try {
            var files = client.listObjectsV2Paginator(
                    ListObjectsV2Request
                            .builder()
                            .bucket(bucket)
                            .delimiter("/")
                            .build()
            );
            List<String> ids = new ArrayList<>();
            for (var file: files.commonPrefixes()) {
                String id = file.prefix().split("/")[0];
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        client.close();
        closed = true;
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

        StringBuilder p = new StringBuilder(getFilePath(mapId) + "/tiles/" + lod);
        for (String s : folders){
            p.append("/").append(s);
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
