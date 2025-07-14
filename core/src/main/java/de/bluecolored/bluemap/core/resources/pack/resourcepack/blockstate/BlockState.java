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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockState {

    private Variants variants = null;
    private Multipart multipart = null;

    public BlockState(Variants variants){
        this.variants = variants;
    }

    public BlockState(Multipart multipart){
        this.multipart = multipart;
    }

    @Nullable
    public Variants getVariants() {
        return variants;
    }

    @Nullable
    public Multipart getMultipart() {
        return multipart;
    }

    public void forEach(Consumer<Variant> consumer) {
        variants.forEach(consumer);
        multipart.forEach(consumer);
    }

    public void forEach(de.bluecolored.bluemap.core.world.BlockState blockState, int x, int y, int z, Consumer<Variant> consumer) {
        if (variants != null) variants.forEach(blockState, x, y, z, consumer);
        if (multipart != null) multipart.forEach(blockState, x, y, z, consumer);
    }

}
