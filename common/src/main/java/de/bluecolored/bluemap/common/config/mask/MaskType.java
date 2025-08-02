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
package de.bluecolored.bluemap.common.config.mask;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface MaskType extends Keyed {

    MaskType BOX = new Impl(Key.bluemap("box"), BoxMaskConfig.class);
    MaskType CIRCLE = new Impl(Key.bluemap("circle"), CircleMaskConfig.class);
    MaskType ELLIPSE = new Impl(Key.bluemap("ellipse"), EllipseMaskConfig.class);
    MaskType POLYGON = new Impl(Key.bluemap("polygon"), PolygonMaskConfig.class);
    MaskType BLUR = new Impl(Key.bluemap("blur"), BlurMaskConfig.class);

    Registry<MaskType> REGISTRY = new Registry<>(
            BOX,
            CIRCLE,
            ELLIPSE,
            POLYGON,
            BLUR
    );

    Class<? extends MaskConfig> getConfigType();

    @RequiredArgsConstructor
    @Getter
    class Impl implements MaskType {

        private final Key key;
        private final Class<? extends MaskConfig> configType;

    }

}
