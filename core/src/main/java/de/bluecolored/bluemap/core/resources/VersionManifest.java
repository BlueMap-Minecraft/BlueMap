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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.core.resources.adapter.LocalDateTimeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class VersionManifest {

    public static final String DOMAIN = "https://piston-meta.mojang.com/";
    public static final String MANIFEST_URL = DOMAIN + "mc/game/version_manifest.json";

    private static final int CONNECTION_TIMEOUT = 10000;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private static VersionManifest instance;

    private Latest latest;
    private Version[] versions;

    @Getter(AccessLevel.NONE)
    private transient @Nullable Map<String, Version> versionMap;

    @Getter(AccessLevel.NONE)
    private transient boolean sorted;


    public static VersionManifest getOrFetch() throws IOException {
        if (instance == null) return fetch();
        return instance;
    }

    public static VersionManifest fetch() throws IOException {
        try (
                InputStream in = openInputStream(MANIFEST_URL);
                Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            instance = GSON.fromJson(reader, VersionManifest.class);
        }
        return instance;
    }

    /**
     * An array of versions, ordered newest first
     */
    public synchronized Version[] getVersions() {
        if (!sorted) Arrays.sort(versions, Comparator.reverseOrder());
        return versions;
    }

    public synchronized Version getVersion(String id) throws IOException {
        if (versionMap == null) {
            versionMap = new HashMap<>();
            for (Version version : versions)
                versionMap.put(version.id, version);
        }

        Version version = versionMap.get(id);
        if (version == null) throw new IOException("There is no version '%s' in manifest.".formatted(id));
        return version;
    }

    @Getter
    public static class Latest {
        private String release;
        private String snapshot;
    }

    @Getter
    public static class Version implements Comparable<Version> {

        private String id;
        private String type;
        private String url;
        private LocalDateTime time;
        private LocalDateTime releaseTime;

        @Getter(AccessLevel.NONE)
        private transient @Nullable VersionDetail detail;

        public synchronized VersionDetail fetchDetail() throws IOException {
            if (detail == null) {
                try (
                        InputStream in = openInputStream(url);
                        Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                ) {
                    detail = GSON.fromJson(reader, VersionDetail.class);
                }
            }

            return detail;
        }

        @Override
        public int compareTo(@NotNull VersionManifest.Version version) {
            return releaseTime.compareTo(version.releaseTime);
        }

    }

    @Getter
    public static class VersionDetail {
        private String id;
        private String type;
        private Downloads downloads;
    }

    @Getter
    public static class Downloads {
        private Download client;
        private Download server;
    }

    @Getter
    public static class Download {
        private String url;
        private long size;
        private String sha1;

        public InputStream createInputStream() throws IOException {
            return openInputStream(url);
        }

    }

    private static InputStream openInputStream(String urlPath) throws IOException {
        try {
            URL downloadUrl = new URI(urlPath).toURL();
            URLConnection connection = downloadUrl.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(CONNECTION_TIMEOUT);
            return connection.getInputStream();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
