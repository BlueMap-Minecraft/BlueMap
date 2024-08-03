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
package de.bluecolored.bluemap.core.world.block.entity;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

public interface BlockEntityType extends Keyed, BlockEntityLoader {

    BlockEntityType SIGN = new Impl(Key.minecraft("sign"), SignBlockEntity::new);
    BlockEntityType HANGING_SIGN = new Impl(Key.minecraft("hanging_sign"), SignBlockEntity::new);
    BlockEntityType SKULL = new Impl(Key.minecraft("skull"), SkullBlockEntity::new);
    BlockEntityType BANNER = new Impl(Key.minecraft("banner"), BannerBlockEntity::new);

    Registry<BlockEntityType> REGISTRY = new Registry<>(
            SIGN,
            HANGING_SIGN,
            SKULL,
            BANNER
    );

    @RequiredArgsConstructor
    class Impl implements BlockEntityType {

        @Getter
        private final Key key;
        private final BlockEntityLoader loader;

        @Override
        public BlockEntity load(Map<String, Object> raw) {
            return loader.load(raw);
        }

    }

}
