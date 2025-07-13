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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.atlas;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.ResourcePool;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class Atlas {

    private LinkedHashSet<Source> sources = new LinkedHashSet<>();

    @Contract("_ -> this")
    public Atlas add(Atlas atlas) {
        sources.addAll(atlas.getSources());
        return this;
    }

    public void load(Path root, ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        sources.forEach(source -> {
            try {
                source.load(root, textures, textureFilter);
            } catch (IOException e) {
                Logger.global.logDebug("Failed to load atlas-source: " + e);
            }
        });
    }

    public void bake(ResourcePool<Texture> textures, Predicate<Key> textureFilter) throws IOException {
        sources.forEach(source -> {
            try {
                source.bake(textures, textureFilter);
            } catch (IOException e) {
                Logger.global.logDebug("Failed to bake atlas-source: " + e);
            }
        });
    }

}
