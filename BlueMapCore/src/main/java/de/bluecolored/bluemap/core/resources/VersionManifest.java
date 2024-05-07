package de.bluecolored.bluemap.core.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.core.resources.adapter.LocalDateTimeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
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
                InputStream in = new URL(MANIFEST_URL).openStream();
                Reader reader = new BufferedReader(new InputStreamReader(in))
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

    public synchronized @Nullable Version getVersion(String id) {
        if (versionMap == null) {
            versionMap = new HashMap<>();
            for (Version version : versions)
                versionMap.put(version.id, version);
        }

        return versionMap.get(id);
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
                        InputStream in = new URL(url).openStream();
                        Reader reader = new BufferedReader(new InputStreamReader(in))
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
    }

}
