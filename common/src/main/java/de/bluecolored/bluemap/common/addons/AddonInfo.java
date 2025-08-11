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
package de.bluecolored.bluemap.common.addons;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
@Getter
public class AddonInfo {
    public static final String ADDON_INFO_FILE = "bluemap.addon.json";

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .create();

    private String id;
    private String entrypoint;
    private Set<String> dependencies = Set.of();
    private Set<String> softDependencies = Set.of();

    public static @Nullable AddonInfo load(Path addonJarFile) throws ConfigurationException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(addonJarFile, (ClassLoader) null)) {
            for (Path root : fileSystem.getRootDirectories()) {
                Path addonInfoFile = root.resolve(ADDON_INFO_FILE);
                if (!Files.exists(addonInfoFile)) continue;

                try (Reader reader = Files.newBufferedReader(addonInfoFile, StandardCharsets.UTF_8)) {
                    AddonInfo addonInfo = GSON.fromJson(reader, AddonInfo.class);

                    if (addonInfo.getId() == null)
                        throw new ConfigurationException("'id' is missing");

                    if (addonInfo.getEntrypoint() == null)
                        throw new ConfigurationException("'entrypoint' is missing");

                    return addonInfo;
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException("There was an exception trying to access the file.", e);
        }

        return null;
    }

}
