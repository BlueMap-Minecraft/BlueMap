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
package de.bluecolored.bluemap.core.map.renderstate;

import java.util.Objects;

import static de.bluecolored.bluemap.core.map.renderstate.TileState.*;

@FunctionalInterface
public interface TileActionResolver {

    ActionAndNextState findActionAndNextState(
            boolean chunksChanged,
            BoundsSituation bounds
    );

    enum BoundsSituation {
        INSIDE,
        EDGE,
        OUTSIDE
    }

    enum Action {
        NONE,
        RENDER,
        DELETE
    }

    record ActionAndNextState (Action action, TileState state) {

        public ActionAndNextState(Action action, TileState state) {
            this.action = Objects.requireNonNull(action);
            this.state = Objects.requireNonNull(state);
        }

        public static final ActionAndNextState RENDER_RENDERED = new ActionAndNextState(Action.RENDER, RENDERED);
        public static final ActionAndNextState NONE_RENDERED = new ActionAndNextState(Action.NONE, RENDERED);
        public static final ActionAndNextState RENDER_RENDERED_EDGE = new ActionAndNextState(Action.RENDER, RENDERED_EDGE);
        public static final ActionAndNextState NONE_RENDERED_EDGE = new ActionAndNextState(Action.NONE, RENDERED_EDGE);
        public static final ActionAndNextState DELETE_OUT_OF_BOUNDS = new ActionAndNextState(Action.DELETE, OUT_OF_BOUNDS);
        public static final ActionAndNextState NONE_OUT_OF_BOUNDS = new ActionAndNextState(Action.NONE, OUT_OF_BOUNDS);
    }

}
