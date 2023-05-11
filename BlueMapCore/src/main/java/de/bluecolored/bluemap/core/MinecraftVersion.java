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
package de.bluecolored.bluemap.core;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.util.Lazy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DebugDump
public class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Pattern VERSION_REGEX = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?(?:-(?:pre|rc)\\d+)?");

    public static final MinecraftVersion LATEST_SUPPORTED = new MinecraftVersion(1, 19, 4);
    public static final MinecraftVersion EARLIEST_SUPPORTED = new MinecraftVersion(1, 13);

    private final int major, minor, patch;

    private final Lazy<MinecraftResource> resource;

    public MinecraftVersion(int major, int minor) {
        this(major, minor, 0);
    }

    public MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;

        this.resource = new Lazy<>(this::findBestMatchingResource);
    }

    public String getVersionString() {
        return major + "." + minor + "." + patch;
    }

    public MinecraftResource getResource() {
        return this.resource.getValue();
    }

    public boolean isAtLeast(MinecraftVersion minVersion) {
        return compareTo(minVersion) >= 0;
    }

    public boolean isAtMost(MinecraftVersion maxVersion) {
        return compareTo(maxVersion) <= 0;
    }

    public boolean isBefore(MinecraftVersion minVersion) {
        return compareTo(minVersion) < 0;
    }

    public boolean isAfter(MinecraftVersion minVersion) {
        return compareTo(minVersion) > 0;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        int result;

        result = Integer.compare(major, other.major);
        if (result != 0) return result;

        result = Integer.compare(minor, other.minor);
        if (result != 0) return result;

        result = Integer.compare(patch, other.patch);
        return result;
    }

    public boolean majorEquals(MinecraftVersion that) {
        return major == that.major;
    }

    public boolean minorEquals(MinecraftVersion that) {
        return major == that.major && minor == that.minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    private MinecraftResource findBestMatchingResource() {
        MinecraftResource[] resources = MinecraftResource.values();
        Arrays.sort(resources, Comparator.comparing(MinecraftResource::getVersion).reversed());

        for (MinecraftResource resource : resources){
            if (isAtLeast(resource.version)) return resource;
        }

        return resources[resources.length - 1];
    }

    public static MinecraftVersion of(String versionString) {
        Matcher matcher = VERSION_REGEX.matcher(versionString);
        if (!matcher.matches()) throw new IllegalArgumentException("Not a valid version string!");

        int major = Integer.parseInt(matcher.group("major"));
        int minor = Integer.parseInt(matcher.group("minor"));
        int patch = 0;
        String patchString = matcher.group("patch");
        if (patchString != null) patch = Integer.parseInt(patchString);

        return new MinecraftVersion(major, minor, patch);
    }

    @DebugDump
    public enum MinecraftResource {

        MC_1_13 (new MinecraftVersion(1, 13), "mc1_13", "https://piston-data.mojang.com/v1/objects/30bfe37a8db404db11c7edf02cb5165817afb4d9/client.jar"),
        MC_1_14 (new MinecraftVersion(1, 14), "mc1_13", "https://piston-data.mojang.com/v1/objects/8c325a0c5bd674dd747d6ebaa4c791fd363ad8a9/client.jar"),
        MC_1_15 (new MinecraftVersion(1, 15), "mc1_15", "https://piston-data.mojang.com/v1/objects/e3f78cd16f9eb9a52307ed96ebec64241cc5b32d/client.jar"),
        MC_1_16 (new MinecraftVersion(1, 16), "mc1_16", "https://piston-data.mojang.com/v1/objects/228fdf45541c4c2fe8aec4f20e880cb8fcd46621/client.jar"),
        MC_1_16_2 (new MinecraftVersion(1, 16, 2), "mc1_16", "https://piston-data.mojang.com/v1/objects/653e97a2d1d76f87653f02242d243cdee48a5144/client.jar"),
        MC_1_17 (new MinecraftVersion(1, 17), "mc1_16", "https://piston-data.mojang.com/v1/objects/1cf89c77ed5e72401b869f66410934804f3d6f52/client.jar"),
        MC_1_18 (new MinecraftVersion(1, 18), "mc1_18", "https://piston-data.mojang.com/v1/objects/020aa79e63a7aab5d6f30e5ec7a6c08baee6b64c/client.jar"),
        MC_1_19 (new MinecraftVersion(1, 19), "mc1_18", "https://piston-data.mojang.com/v1/objects/a45634ab061beb8c878ccbe4a59c3315f9c0266f/client.jar"),
        MC_1_19_4 (new MinecraftVersion(1, 19, 4), "mc1_18", "https://piston-data.mojang.com/v1/objects/958928a560c9167687bea0cefeb7375da1e552a8/client.jar"),
        MC_1_20 (new MinecraftVersion(1, 20), "mc1_18", "https://piston-data.mojang.com/v1/objects/07dfbe4c383c4bb6d26254e49c0fce5398bab5b5/client.jar");

        private final MinecraftVersion version;
        private final String resourcePrefix;
        private final String clientUrl;

        MinecraftResource(MinecraftVersion version, String resourcePrefix, String clientUrl) {
            this.version = version;
            this.resourcePrefix = resourcePrefix;
            this.clientUrl = clientUrl;
        }

        public MinecraftVersion getVersion() {
            return version;
        }

        public String getResourcePrefix() {
            return resourcePrefix;
        }

        public String getClientUrl() {
            return clientUrl;
        }
    }

}
