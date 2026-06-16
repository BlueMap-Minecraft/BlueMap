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
package de.bluecolored.bluemap.common.rendermanager;

import de.bluecolored.bluemap.core.map.renderstate.TileState;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.util.function.Predicate;

public interface TileUpdateStrategy extends Predicate<TileState>, Keyed {

    TileUpdateStrategy FORCE_ALL = new Impl(Key.bluemap("force_all"), tileState -> true);
    TileUpdateStrategy FORCE_EDGE = new Impl(Key.bluemap("force_edge"), tileState -> tileState == TileState.RENDERED_EDGE);
    TileUpdateStrategy FORCE_NONE = new Impl(Key.bluemap("force_none"), tileState -> false);

    Registry<TileUpdateStrategy> REGISTRY = new Registry<>(
            FORCE_ALL,
            FORCE_EDGE,
            FORCE_NONE
    );

    static TileUpdateStrategy fixed(boolean force) {
        return force ? FORCE_ALL : FORCE_NONE;
    }

    @RequiredArgsConstructor
    @Getter
    class Impl implements TileUpdateStrategy {

        private final Key key;
        @Delegate private final Predicate<TileState> predicate;

    }

}
