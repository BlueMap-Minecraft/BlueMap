/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.resources;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.pack.PackVersion;
import de.bluecolored.bluemap.core.util.FileHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipException;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinecraftVersion {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private static final String EARLIEST_RESOURCEPACK_VERSION = "1.13";
    private static final String EARLIEST_DATAPACK_VERSION = "1.19.4";

    private final String id;

    private final Path resourcePack;
    private final PackVersion resourcePackVersion;

    private final Path dataPack;
    private final PackVersion dataPackVersion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static MinecraftVersion load(@Nullable String id, Path dataRoot, boolean allowDownload) throws IOException {
        Path resourcePack;
        Path dataPack;

        try {
            VersionManifest manifest = VersionManifest.getOrFetch();
            if (id == null) id = manifest.getLatest().getRelease();

            VersionManifest.Version version = manifest.getVersion(id);
            VersionManifest.Version resourcePackVersion = manifest.getVersion(EARLIEST_RESOURCEPACK_VERSION);
            VersionManifest.Version dataPackVersion = manifest.getVersion(EARLIEST_DATAPACK_VERSION);

            if (version.compareTo(resourcePackVersion) > 0) resourcePackVersion = version;
            if (version.compareTo(dataPackVersion) > 0) dataPackVersion = version;

            resourcePack = dataRoot.resolve(getClientVersionFileName(resourcePackVersion.getId()));
            dataPack = dataRoot.resolve(getClientVersionFileName(dataPackVersion.getId()));

            if (allowDownload) {
                if (!Files.exists(resourcePack)) download(resourcePackVersion, resourcePack);
                if (!Files.exists(dataPack)) download(dataPackVersion, dataPack);
            }

        } catch (IOException ex) {
            if (id == null) throw ex;

            Logger.global.logWarning("Failed to fetch version-info from mojang-servers: " + ex);

            resourcePack = dataRoot.resolve(getClientVersionFileName(id));
            dataPack = resourcePack;
        }

        if (!Files.exists(resourcePack)) throw new IOException("Resource-File missing: " + resourcePack);
        if (!Files.exists(dataPack)) throw new IOException("Resource-File missing: " + dataPack);

        try {
            VersionInfo resourcePackVersionInfo = loadVersionInfo(resourcePack);
            VersionInfo dataPackVersionInfo = resourcePack.equals(dataPack) ? resourcePackVersionInfo : loadVersionInfo(dataPack);

            return new MinecraftVersion(
                    id,
                    resourcePack, resourcePackVersionInfo.getPackVersion().getResource(),
                    dataPack, dataPackVersionInfo.getPackVersion().getData()
            );
        } catch (IOException ex) {
            // If something went wrong with reading the resource-files, delete them so they will be re-downloaded on the next try.
            if (allowDownload) {
                Files.deleteIfExists(resourcePack);
                Files.deleteIfExists(dataPack);
            }
            throw ex;
        }

    }

    private static void download(VersionManifest.Version version, Path file) throws IOException {
        VersionManifest.Download download = version.fetchDetail().getDownloads().getClient();
        Logger.global.logInfo("Downloading '" + download.getUrl() + "' to '" + file + "'...");

        FileHelper.createDirectories(file.toAbsolutePath().normalize().getParent());
        Path unverifiedFile = file.getParent().resolve(file.getFileName().toString() + ".unverified");

        try {
            try (
                    DigestInputStream in = new DigestInputStream(
                            download.createInputStream(),
                            MessageDigest.getInstance("SHA-1")
                    );
                    OutputStream out = Files.newOutputStream(unverifiedFile)
            ) {

                // download
                in.transferTo(out);

                // verify sha-1
                if (!Arrays.equals(
                        in.getMessageDigest().digest(),
                        hexStringToByteArray(download.getSha1())
                )) {
                    throw new IOException("SHA-1 of the downloaded file does not match!");
                }

            }

            // rename once verified
            FileHelper.atomicMove(unverifiedFile, file);

        } catch (NoSuchAlgorithmException | IOException ex) {
            Logger.global.logWarning("Failed to download '" + download.getUrl() + "': " + ex);
        } finally {
            Files.deleteIfExists(unverifiedFile);
        }

    }

    private static String getClientVersionFileName(String versionId) {
        return "minecraft-client-" + versionId + ".jar";
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        if (length % 2 != 0)
            throw new IllegalArgumentException("Invalid hex-string.");

        int halfLength = length / 2;

        byte[] data = new byte[halfLength];
        int c;
        for (int i = 0; i < halfLength; i += 1) {
            c = i * 2;
            data[i] = (byte) (
                    (Character.digit(hexString.charAt(c), 16) << 4) +
                    Character.digit(hexString.charAt(c + 1), 16)
            );
        }

        return data;
    }

    private static VersionInfo loadVersionInfo(Path file) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(file, (ClassLoader) null)) {
            for (Path fsRoot : fileSystem.getRootDirectories()) {
                if (!Files.isDirectory(fsRoot)) continue;

                Path versionFile = fsRoot.resolve("version.json");
                if (!Files.exists(versionFile)) continue;

                try (Reader reader = Files.newBufferedReader(versionFile, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, VersionInfo.class);
                }
            }

            // no version.json found, assume 1.13 - 1.14.4
            return new VersionInfo();
        }
    }

    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    @Getter
    private static class VersionInfo {
        private PackVersions packVersion = new PackVersions();
    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    @JsonAdapter(PackVersions.Adapter.class)
    @NoArgsConstructor
    private static class PackVersions {

        public PackVersions(int resource, int data) {
            this.resourceMajor = resource;
            this.dataMajor = data;
        }

        @SerializedName(value = "resource_major", alternate = "resource")
        private int resourceMajor = 4;
        private int resourceMinor = 0;
        @SerializedName(value = "data_major", alternate = "data")
        private int dataMajor = 4;
        private int dataMinor = 0;

        public PackVersion getResource() {
            return new PackVersion(resourceMajor, resourceMinor);
        }

        public PackVersion getData() {
            return new PackVersion(dataMajor, dataMinor);
        }

        private static class Adapter extends AbstractTypeAdapterFactory<PackVersions> {

            public Adapter() {
                super(PackVersions.class);
            }

            @Override
            public PackVersions read(JsonReader in, Gson gson) throws IOException {
                return in.peek() == JsonToken.NUMBER ?
                    new PackVersions(in.nextInt(), 4) :
                    gson.getDelegateAdapter(this, TypeToken.get(PackVersions.class)).read(in);
            }

        }

    }

}
