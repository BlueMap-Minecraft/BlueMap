package de.bluecolored.bluemap.core.storage.s3;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.storage.*;
import de.bluecolored.bluemap.core.util.OnCloseOutputStream;
import io.github.linktosriram.s3lite.api.client.S3Client;
import io.github.linktosriram.s3lite.api.exception.NoSuchKeyException;
import io.github.linktosriram.s3lite.api.exception.S3Exception;
import io.github.linktosriram.s3lite.api.region.Region;
import io.github.linktosriram.s3lite.api.request.DeleteObjectRequest;
import io.github.linktosriram.s3lite.api.request.GetObjectRequest;
import io.github.linktosriram.s3lite.api.request.ListObjectsV2Request;
import io.github.linktosriram.s3lite.api.request.PutObjectRequest;
import io.github.linktosriram.s3lite.api.response.CommonPrefix;
import io.github.linktosriram.s3lite.api.response.ListObjectsV2ResponsePager;
import io.github.linktosriram.s3lite.core.auth.AwsBasicCredentials;
import io.github.linktosriram.s3lite.core.client.DefaultS3ClientBuilder;
import io.github.linktosriram.s3lite.http.spi.request.RequestBody;
import io.github.linktosriram.s3lite.http.urlconnection.URLConnectionSdkHttpClient;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class S3Storage extends Storage {
    private boolean closed = false;
    private final S3Client client;
    private final Compression hiresCompression;
    private final String bucket;
    public S3Storage(S3StorageSettings settings) {
        String endpoint = settings.getEndpoint().orElse(
                String.format("https://s3.%s.amazonaws.com", settings.getRegion())
        );
        this.client = new DefaultS3ClientBuilder()
                .credentialsProvider(() -> AwsBasicCredentials.create(settings.getAccessKey(), settings.getSecretKey()))
                .region(Region.of(settings.getRegion(), URI.create(endpoint)))
                .httpClient(URLConnectionSdkHttpClient.create()) // Or use URLConnectionSdkHttpClient
                .build();
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
                            .bucketName(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(new CompressedInputStream(is, compression));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<TileInfo> readMapTileInfo(String mapId, int lod, Vector2i tile) throws IOException {
        Compression compression = lod == 0 ? this.hiresCompression : Compression.NONE;
        String path = getFilePath(mapId, lod, tile);
        try {
            var info = client.headObject(
                    GetObjectRequest
                            .builder()
                            .bucketName(bucket)
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
                    return info.getContentLength();
                }

                @Override
                public long getLastModified() {
                    return info.getLastModified().getEpochSecond();
                }
            });
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
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
                            .bucketName(bucket)
                            .key(path)
                            .build()
            );
        } catch (NoSuchKeyException ignored) {
        } catch (S3Exception e) {
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
                                .bucketName(bucket)
                                .key(path)
                                .contentType(FileMime)
                                .contentLength((long) byteOut.toByteArray().length)
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
                            .bucketName(bucket)
                            .key(path)
                            .build()
            );
            return Optional.of(is);
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException {
        String path = getMetaFilePath(mapId, name);
        try {
            var info = client.headObject(
                    GetObjectRequest
                            .builder()
                            .bucketName(bucket)
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
                    return info.getContentLength();
                }
            });
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
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
                            .bucketName(bucket)
                            .key(path)
                            .build()
            );
        } catch (NoSuchKeyException ignored) {
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException {
        String directory = getFilePath(mapId);
        try {
            var files = new ListObjectsV2ResponsePager(client,
                    ListObjectsV2Request
                            .builder()
                            .bucketName(bucket)
                            .prefix(directory + "/")
            );
            var filesList = StreamSupport
                    .stream(
                            Spliterators.spliteratorUnknownSize(
                                    files.getContents(), 0),
                            false
                    ).collect(Collectors.toList());
            for (int i = 0; i < filesList.size(); i++) {
                var file = filesList.get(i);
                try {
                    client.deleteObject(
                            DeleteObjectRequest
                                    .builder()
                                    .bucketName(bucket)
                                    .key(file.getKey())
                                    .build()
                    );
                } catch (NoSuchKeyException ignored) {}

                if (!onProgress.apply(
                        new ProgressInfo((double) (i + 1) / filesList.size())
                )) return;
            }
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Collection<String> collectMapIds() throws IOException {
        try {
            var files = new ListObjectsV2ResponsePager(client,
                    ListObjectsV2Request
                            .builder()
                            .bucketName(bucket)
                            .delimiter("/")
            );
            List<String> ids = new ArrayList<>();
            for (Iterator<CommonPrefix> it = files.getCommonPrefixes(); it.hasNext(); ) {
                var file = it.next();
                String id = file.getPrefix().split("/")[0];
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
